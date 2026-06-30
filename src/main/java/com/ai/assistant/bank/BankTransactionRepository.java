package com.ai.assistant.bank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    List<BankTransaction> findByCompanyIdOrderByTxnDateDesc(Long companyId);
}
