package com.kuzmin.deposit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuzmin.deposit.DepositApplication;
import com.kuzmin.deposit.config.SpringH2TestConfig;
import com.kuzmin.deposit.controller.dto.DepositResponseDTO;
import com.kuzmin.deposit.entity.Deposit;
import com.kuzmin.deposit.repository.DepositRepository;
import com.kuzmin.deposit.rest.AccountServiceClient;
import com.kuzmin.deposit.rest.BillServiceClient;
import com.kuzmin.deposit.rest.dto.AccountResponseDTO;
import com.kuzmin.deposit.rest.dto.BillResponseDTO;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DepositApplication.class, SpringH2TestConfig.class})
public class DepositControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepositRepository depositRepository;

    @MockBean
    private BillServiceClient billServiceClient;

    @MockBean
    private AccountServiceClient accountServiceClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Before
    public  void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static final String REQUEST = "{\n" +
            "    \"billId\": 1,\n" +
            "    \"amount\": 3000\n" +
            "}";

    @Test
    public void createDeposit() throws Exception {
        BillResponseDTO billResponseDTO = createBillResponseDTO();
        AccountResponseDTO accountResponseDTO = createAccountResponseDTO();

        //Создаем заглушки на ответ от других сервисов
        Mockito.when(billServiceClient.getBillById(ArgumentMatchers.anyLong()))
                .thenReturn(billResponseDTO);

        Mockito.when(accountServiceClient.getAccountById(ArgumentMatchers.anyLong()))
                .thenReturn(accountResponseDTO);

        //Пробрасываем rest запрос на тестируемый сервис
        MvcResult mvcResult = mockMvc.perform(post("/deposits/")
                    .content(REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Преобразуем ответ в строку
        String body = mvcResult.getResponse().getContentAsString();

        //Полуаем реальный данные в H2
        List<Deposit> deposits = depositRepository.findDepositByEmail(accountResponseDTO.getEmail());

        //создаем objectMapper, чтобы преобразовать json в объект
        ObjectMapper objectMapper = new ObjectMapper();
        DepositResponseDTO depositResponseDTO = objectMapper.readValue(body, DepositResponseDTO.class);

        //Итоговая проверка что email и сумма депозита совпадает
        Assertions.assertThat(depositResponseDTO.getMail()).isEqualTo(deposits.get(0).getEmail());
        Assertions.assertThat(depositResponseDTO.getAmount()).isEqualTo(BigDecimal.valueOf(300000, 2));
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
