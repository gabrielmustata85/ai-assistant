package com.ai.assistant.bank;

import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Salvează în lot tranzacțiile confirmate de user, sărind peste duplicate.
     * Extrasele cu perioade suprapuse pot conține aceleași tranzacții — o tranzacție
     * este considerată duplicat dacă are aceeași dată + sens + sumă + descriere + contraparte,
     * fie că există deja în baza de date, fie că se repetă în lotul curent.
     */
    public BankImportResult saveAll(Long companyId, List<BankTransaction> transactions) {
        companyService.get(companyId);

        Set<String> seen = new HashSet<>();
        for (BankTransaction existing : repository.findByCompanyIdOrderByTxnDateDesc(companyId)) {
            seen.add(fingerprint(existing));
        }

        List<BankTransaction> toSave = new ArrayList<>();
        int skipped = 0;
        for (BankTransaction t : transactions) {
            t.setId(null);
            t.setCompanyId(companyId);
            String fp = fingerprint(t);
            if (!seen.add(fp)) {   // deja văzut (în DB sau mai devreme în lot)
                skipped++;
                continue;
            }
            toSave.add(t);
        }

        List<BankTransaction> saved = repository.saveAll(toSave);
        return new BankImportResult(saved.size(), skipped, saved);
    }

    /** Amprenta unei tranzacții pentru detectarea duplicatelor. */
    private String fingerprint(BankTransaction t) {
        BigDecimal amount = t.getAmount();
        String amountKey = amount == null ? "" : amount.stripTrailingZeros().toPlainString();
        return t.getTxnDate() + "|" + t.getDirection() + "|" + amountKey
                + "|" + norm(t.getDescription()) + "|" + norm(t.getCounterparty());
    }

    private String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", " ");
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
