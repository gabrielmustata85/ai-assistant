package com.ai.assistant.invoicing;

import com.ai.assistant.ai.ClaudeClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/** Extrage textul dintr-un PDF de factură și pune Claude să scoată datele structurate. */
@Service
public class InvoicePdfParser {

    private final ClaudeClient claudeClient;

    public InvoicePdfParser(ClaudeClient claudeClient) {
        this.claudeClient = claudeClient;
    }

    public ParsedInvoice parse(MultipartFile file) throws IOException {
        String text = extractText(file);
        if (text == null || text.isBlank()) {
            throw new IOException("Nu s-a putut extrage text din PDF (poate e scanat/imagine).");
        }
        String prompt = """
                Ești un asistent care extrage datele unei facturi românești dintr-un PDF.
                Extrage câmpurile cerute din textul de mai jos. Sumele sunt numere (fără simbol valutar),
                folosește punct pentru zecimale. Datele în format YYYY-MM-DD. Dacă un câmp lipsește,
                lasă-l gol (sau 0 pentru sume). Ghicește `direction` din context.

                === TEXT FACTURĂ ===
                """ + text;
        return claudeClient.extractStructured(prompt, ParsedInvoice.class);
    }

    private String extractText(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }
}
