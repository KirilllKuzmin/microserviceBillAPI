package com.kuzmin.bill.repository;

import com.kuzmin.bill.entity.Bill;
import org.springframework.data.repository.CrudRepository;

public interface BillRepository extends CrudRepository<Bill, Long> {
}
