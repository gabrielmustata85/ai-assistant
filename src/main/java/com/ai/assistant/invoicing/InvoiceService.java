package com.ai.assistant.invoicing;

import com.ai.assistant.common.BatchParseResult;
import com.ai.assistant.company.Company;
import com.ai.assistant.company.CompanyService;
import com.ai.assistant.partner.PartnerService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository repository;
    private final CompanyService companyService;
    private final InvoicePdfParser pdfParser;
    private final InvoicePdfGenerator pdfGenerator;
    private final InvoiceDocumentRepository documentRepository;
    private final PartnerService partnerService;

    public InvoiceService(InvoiceRepository repository, CompanyService companyService,
                          InvoicePdfParser pdfParser, InvoicePdfGenerator pdfGenerator,
                          InvoiceDocumentRepository documentRepository, PartnerService partnerService) {
        this.repository = repository;
        this.companyService = companyService;
        this.pdfParser = pdfParser;
        this.pdfGenerator = pdfGenerator;
        this.documentRepository = documentRepository;
        this.partnerService = partnerService;
    }

    /**
     * Fișierul de descărcat pentru o factură: originalul încărcat dacă există, altfel PDF-ul generat.
     * Verifică ownership-ul firmei.
     */
    public DownloadFile download(Long id) {
        Invoice invoice = repository.findById(id).orElseThrow();
        Company company = companyService.get(invoice.getCompanyId());   // ownership
        if (invoice.getSourceDocumentId() != null) {
            InvoiceDocument d = documentRepository.findById(invoice.getSourceDocumentId()).orElse(null);
            if (d != null && d.getData() != null) {
                String name = (d.getFilename() != null && !d.getFilename().isBlank())
                        ? d.getFilename() : ("factura-" + id + ".pdf");
                String type = (d.getContentType() != null && !d.getContentType().isBlank())
                        ? d.getContentType() : "application/pdf";
                return new DownloadFile(d.getData(), name, type);
            }
        }
        // Fără original → generăm PDF-ul, cu detaliile clientului trase din Colaboratori.
        var partner = partnerService.lookup(invoice.getCompanyId(),
                invoice.getPartnerCui(), invoice.getPartnerName()).orElse(null);
        return new DownloadFile(pdfGenerator.generate(invoice, company, partner),
                "factura-" + id + ".pdf", "application/pdf");
    }

    /** Extrage TOATE facturile dintr-un PDF (poate conține mai multe). Nu salvează — userul confirmă. */
    public List<ParsedInvoice> parsePdf(Long companyId, MultipartFile file) throws IOException {
        companyService.get(companyId);   // verifică ownership înainte de orice
        return pdfParser.parse(file);
    }

    /**
     * Extrage din mai multe PDF-uri, fiecare putând conține mai multe facturi.
     * Întoarce o listă plată: câte un rezultat per factură găsită (sau un rezultat de eroare per fișier).
     */
    public List<BatchParseResult<ParsedInvoice>> parsePdfBatch(Long companyId, MultipartFile[] files) {
        companyService.get(companyId);   // verifică ownership o singură dată
        List<BatchParseResult<ParsedInvoice>> results = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            try {
                List<ParsedInvoice> invoices = pdfParser.parse(file);
                if (invoices.isEmpty()) {
                    results.add(BatchParseResult.failed(name, "Nicio factură găsită în document."));
                } else {
                    // Stochează fișierul original o singură dată; toate facturile din el îl referă.
                    InvoiceDocument doc = documentRepository.save(
                            new InvoiceDocument(companyId, name, file.getContentType(), file.getBytes()));
                    for (ParsedInvoice inv : invoices) {
                        results.add(BatchParseResult.ok(name, inv, doc.getId()));
                        // Auto-populează colaboratorul cu datele de contact de pe factură.
                        partnerService.upsertFromInvoice(companyId, inv.partnerName(), inv.partnerCui(),
                                inv.partnerRegCom(), inv.partnerIban(), inv.partnerPhone(),
                                inv.partnerEmail(), inv.partnerAddress());
                    }
                }
            } catch (Exception e) {
                results.add(BatchParseResult.failed(name, e.getMessage()));
            }
        }
        return results;
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
