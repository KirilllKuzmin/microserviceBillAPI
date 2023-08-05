package com.kuzmin.deposit.rest;

import com.kuzmin.deposit.rest.dto.BillRequestDTO;
import com.kuzmin.deposit.rest.dto.BillResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@FeignClient(name = "bill-service")
public interface BillServiceClient {

    @RequestMapping(value = "/bills/{billId}", method = RequestMethod.GET)
    BillResponseDTO getBillById(@PathVariable("billId") Long billId);

    @RequestMapping(value = "/bills/{billId}", method = RequestMethod.PUT)
    void updateBill(@PathVariable("billId") Long billId, BillRequestDTO billRequestDTO);

    @RequestMapping(value = "/bills/account/{accountId}", method = RequestMethod.GET)
    List<BillResponseDTO> getBillsByAccountId(@PathVariable Long accountId);
}
