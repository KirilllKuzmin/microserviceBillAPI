package com.kuzmin.deposit.repository;

import com.kuzmin.deposit.entity.Deposit;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DepositRepository extends CrudRepository<Deposit, Long> {

    List<Deposit> findDepositByEmail(String email);
}
