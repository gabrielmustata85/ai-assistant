package com.ai.assistant.invoicing;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.common.PdfTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Extrage textul dintr-un PDF și pune Claude să scoată TOATE facturile (poate fi una sau mai multe). */
@Service
public class InvoicePdfParser {

    private final ClaudeClient claudeClient;
    private final PdfTextExtractor pdfTextExtractor;

    public InvoicePdfParser(ClaudeClient claudeClient, PdfTextExtractor pdfTextExtractor) {
        this.claudeClient = claudeClient;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    /** Întoarce TOATE facturile din PDF (un document poate fi o factură sau un export cu mai multe). */
    public List<ParsedInvoice> parse(MultipartFile file) throws IOException {
        String text = pdfTextExtractor.extract(file);
        String prompt = """
                Ești un asistent care extrage facturi românești dintr-un PDF.
                ATENȚIE: documentul poate conține O SINGURĂ factură sau MAI MULTE (ex. un export/tabel
                cu facturi pe mai multe luni). Extrage TOATE facturile, câte un element pentru fiecare
                factură/rând. Pentru fiecare: număr, partener, CUI partener, data emiterii (YYYY-MM-DD),
                scadența, net, TVA, brut (numere cu punct zecimal, fără simbol valutar), categorie,
                deductibil, și `direction` (ISSUED dacă firma a emis-o, RECEIVED dacă a primit-o).
                Dacă un câmp lipsește, lasă-l gol (0 pentru sume).

                === TEXT DOCUMENT ===
                """ + text;
        ParsedInvoices result = claudeClient.extractStructured(prompt, ParsedInvoices.class);
        return (result == null || result.invoices() == null) ? List.of() : result.invoices();
    }
}
