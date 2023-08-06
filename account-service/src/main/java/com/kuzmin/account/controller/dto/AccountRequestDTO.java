package com.kuzmin.account.controller.dto;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class AccountRequestDTO {

    private String name;

    private String email;

    private String phone;

    private List<Long> bills;
}
