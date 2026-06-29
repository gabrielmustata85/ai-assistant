package com.ai.assistant.invoicing;

import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository repository;
    private final CompanyService companyService;

    public InvoiceService(InvoiceRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    public Invoice add(Long companyId, Invoice invoice) {
        companyService.get(companyId);   // aruncă dacă firma nu există sau nu e owner
        invoice.setId(null);
        invoice.setCompanyId(companyId);
        return repository.save(invoice);
    }

    public List<Invoice> listForCompany(Long companyId) {
        companyService.get(companyId);   // verifică ownership
        return repository.findByCompanyId(companyId);
    }

    public void delete(Long id) {
        Invoice invoice = repository.findById(id).orElseThrow();
        companyService.get(invoice.getCompanyId());   // verifică ownership-ul firmei facturii
        repository.deleteById(id);
    }
}
