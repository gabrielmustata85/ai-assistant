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
        companyService.get(companyId);   // aruncă dacă firma nu există
        invoice.setId(null);
        invoice.setCompanyId(companyId);
        return repository.save(invoice);
    }

    public List<Invoice> listForCompany(Long companyId) {
        return repository.findByCompanyId(companyId);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
