package com.kuzmin.deposit.service;

import com.kuzmin.deposit.controller.dto.DepositResponseDTO;
import com.kuzmin.deposit.exception.DepositServiceException;
import com.kuzmin.deposit.repository.DepositRepository;
import com.kuzmin.deposit.rest.AccountServiceClient;
import com.kuzmin.deposit.rest.BillServiceClient;
import com.kuzmin.deposit.rest.dto.AccountResponseDTO;
import com.kuzmin.deposit.rest.dto.BillResponseDTO;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class DepositServiceTest {

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private BillServiceClient billServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DepositService depositService;

    @Test
    public void depositServiceTest_withBillId() {
        BillResponseDTO billResponseDTO = createBillResponseDTO();

        //Создаем моки на вызовы методов
        Mockito.when(billServiceClient.getBillById(ArgumentMatchers.anyLong()))
                .thenReturn(billResponseDTO);

        Mockito.when(accountServiceClient.getAccountById(ArgumentMatchers.anyLong()))
                .thenReturn(createAccountResponseDTO());

        DepositResponseDTO depositResponseDTO = depositService.deposit(null, 1L, BigDecimal.valueOf(1000));

        Assertions.assertThat(depositResponseDTO.getMail()).isEqualTo("test@test.com");
    }

    @Test(expected = DepositServiceException.class)
    public void depositServiceTest_exception() {
        depositService.deposit(null, null, BigDecimal.valueOf(1000));
    }

    private AccountResponseDTO createAccountResponseDTO() {
        AccountResponseDTO accountResponseDTO = new AccountResponseDTO();

        accountResponseDTO.setAccountId(1L);
        accountResponseDTO.setName("test");
        accountResponseDTO.setEmail("test@test.com");
        accountResponseDTO.setPhone("88000000000");
        accountResponseDTO.setCreationDate(OffsetDateTime.now());
        accountResponseDTO.setBills(Arrays.asList(1L, 2L, 3L));

        return accountResponseDTO;
    }

    private BillResponseDTO createBillResponseDTO() {
        BillResponseDTO billResponseDTO = new BillResponseDTO();

        billResponseDTO.setBillId(1L);
        billResponseDTO.setAccountId(1L);
        billResponseDTO.setAmount(BigDecimal.valueOf(1000));
        billResponseDTO.setIsDefault(true);
        billResponseDTO.setCreationDate(OffsetDateTime.now());
        billResponseDTO.setOverdraftEnabled(true);

        return billResponseDTO;
    }
}
