package com.ai.assistant.invoicing;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.common.PdfTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Extrage textul dintr-un PDF de factură și pune Claude să scoată datele structurate. */
@Service
public class InvoicePdfParser {

    private final ClaudeClient claudeClient;
    private final PdfTextExtractor pdfTextExtractor;

    public InvoicePdfParser(ClaudeClient claudeClient, PdfTextExtractor pdfTextExtractor) {
        this.claudeClient = claudeClient;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    public ParsedInvoice parse(MultipartFile file) throws IOException {
        String text = pdfTextExtractor.extract(file);
        String prompt = """
                Ești un asistent care extrage datele unei facturi românești dintr-un PDF.
                Extrage câmpurile cerute din textul de mai jos. Sumele sunt numere (fără simbol valutar),
                folosește punct pentru zecimale. Datele în format YYYY-MM-DD. Dacă un câmp lipsește,
                lasă-l gol (sau 0 pentru sume). Ghicește `direction` din context.

                === TEXT FACTURĂ ===
                """ + text;
        return claudeClient.extractStructured(prompt, ParsedInvoice.class);
    }
}
