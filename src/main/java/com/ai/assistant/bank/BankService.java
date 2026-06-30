package com.ai.assistant.bank;

import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class BankService {

    private final BankTransactionRepository repository;
    private final CompanyService companyService;
    private final BankStatementParser parser;

    public BankService(BankTransactionRepository repository, CompanyService companyService, BankStatementParser parser) {
        this.repository = repository;
        this.companyService = companyService;
        this.parser = parser;
    }

    /** Extrage tranzacțiile dintr-un extras PDF (nu le salvează — userul confirmă). */
    public ParsedStatement parseStatement(Long companyId, MultipartFile file) throws IOException {
        companyService.get(companyId);   // verifică ownership
        return parser.parse(file);
    }

    /** Salvează în lot tranzacțiile confirmate de user. */
    public List<BankTransaction> saveAll(Long companyId, List<BankTransaction> transactions) {
        companyService.get(companyId);
        for (BankTransaction t : transactions) {
            t.setId(null);
            t.setCompanyId(companyId);
        }
        return repository.saveAll(transactions);
    }

    public List<BankTransaction> list(Long companyId) {
        companyService.get(companyId);
        return repository.findByCompanyIdOrderByTxnDateDesc(companyId);
    }

    public void delete(Long id) {
        BankTransaction t = repository.findById(id).orElseThrow();
        companyService.get(t.getCompanyId());   // verifică ownership-ul firmei
        repository.deleteById(id);
    }
}
