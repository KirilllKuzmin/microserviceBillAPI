package com.kuzmin.deposit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuzmin.deposit.controller.dto.DepositResponseDTO;
import com.kuzmin.deposit.entity.Deposit;
import com.kuzmin.deposit.exception.DepositServiceException;
import com.kuzmin.deposit.repository.DepositRepository;
import com.kuzmin.deposit.rest.AccountServiceClient;
import com.kuzmin.deposit.rest.BillServiceClient;
import com.kuzmin.deposit.rest.dto.AccountResponseDTO;
import com.kuzmin.deposit.rest.dto.BillRequestDTO;
import com.kuzmin.deposit.rest.dto.BillResponseDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class DepositService {

    private static final String TOPIC_EXCHANGE_DEPOSIT = "js.deposit.notify.exchange";
    private static final String ROUTING_KEY_DEPOSIT = "js.key.deposit";

    private final DepositRepository depositRepository;

    private final AccountServiceClient accountServiceClient;

    private final BillServiceClient billServiceClient;

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public DepositService(DepositRepository depositRepository,
                          AccountServiceClient accountServiceClient,
                          BillServiceClient billServiceClient,
                          RabbitTemplate rabbitTemplate) {

        this.depositRepository = depositRepository;
        this.accountServiceClient = accountServiceClient;
        this.billServiceClient = billServiceClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    public DepositResponseDTO deposit(Long accountId, Long billId, BigDecimal amount) {
        if (accountId == null && billId == null)
            throw new DepositServiceException("We can't make a deposit, because the Account and the Bill is null");

        //Предпочтителен для создания депозита сначала имнно указанный Bill
        if (billId != null) {
            BillResponseDTO billResponseDTO = billServiceClient.getBillById(billId);

            //Проверяем, что переданный счет принадлежит и переданный аккаунт принадлежат одной сущности
            if (!Objects.equals(accountId, billResponseDTO.getAccountId()) && accountId != null)
                throw new DepositServiceException("Account and Bill are not compatible");

            //Создаем request с обновленным amount, который далее отправим в сервис
            BillRequestDTO billRequestDTO = createBillRequest(amount, billResponseDTO);

            //Отправляем в bill-service
            billServiceClient.updateBill(billId, billRequestDTO);

            //Получаем аккаунт и создаем депозит в БД
            AccountResponseDTO accountResponseDTO = accountServiceClient.getAccountById(billResponseDTO.getAccountId());
            depositRepository.save(new Deposit(amount, billId, OffsetDateTime.now(), accountResponseDTO.getEmail()));

            return createResponseToRabbit(amount, accountResponseDTO);
        }
        BillResponseDTO defaultBill = getDefaultBill(accountId);
        BillRequestDTO billRequestDTO = createBillRequest(amount, defaultBill);
        billServiceClient.updateBill(defaultBill.getBillId(), billRequestDTO);

        AccountResponseDTO accountResponseDTO = accountServiceClient.getAccountById(accountId);
        depositRepository
                .save(new Deposit(amount, defaultBill.getBillId(), OffsetDateTime.now(), accountResponseDTO.getEmail()));

        return createResponseToRabbit(amount, accountResponseDTO);
    }

    /*
     * Создаем ответ и отправляем нотифу на кролик
     */
    private DepositResponseDTO createResponseToRabbit(BigDecimal amount, AccountResponseDTO accountResponseDTO) {
        DepositResponseDTO depositResponseDTO = new DepositResponseDTO(amount, accountResponseDTO.getEmail());
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            rabbitTemplate.convertAndSend(TOPIC_EXCHANGE_DEPOSIT,
                    ROUTING_KEY_DEPOSIT,
                    objectMapper.writeValueAsString(depositResponseDTO));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new DepositServiceException("Can't send message to RabbitMQ");
        }

        return depositResponseDTO;
    }

    /*
     * Метод для поиска счета по умолчанию
     */
    private BillResponseDTO getDefaultBill(Long accountId) {
        return billServiceClient.getBillsByAccountId(accountId)
                .stream()
                .filter(BillResponseDTO::getIsDefault)
                .findAny()
                .orElseThrow(() -> new DepositServiceException("Unable to find default bill for account: " + accountId));
    }

    /*
     * Создание request для дальнейшей отправки rest-запроса в bill-service
     */
    private BillRequestDTO createBillRequest(BigDecimal amount, BillResponseDTO billResponseDTO) {
        return new BillRequestDTO(billResponseDTO.getAccountId(),
                billResponseDTO.getAmount().add(amount), //+ добавляем сумму депозита
                billResponseDTO.getIsDefault(),
                billResponseDTO.getOverdraftEnabled());
    }

}
