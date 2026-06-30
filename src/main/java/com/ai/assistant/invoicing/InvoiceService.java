package com.ai.assistant.invoicing;

import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository repository;
    private final CompanyService companyService;
    private final InvoicePdfParser pdfParser;

    public InvoiceService(InvoiceRepository repository, CompanyService companyService, InvoicePdfParser pdfParser) {
        this.repository = repository;
        this.companyService = companyService;
        this.pdfParser = pdfParser;
    }

    /** Extrage datele unei facturi dintr-un PDF (nu o salvează — userul confirmă apoi). */
    public ParsedInvoice parsePdf(Long companyId, MultipartFile file) throws IOException {
        companyService.get(companyId);   // verifică ownership înainte de orice
        return pdfParser.parse(file);
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
