package com.ai.assistant.payroll;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.common.PdfTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Extrage textul dintr-un PDF de cheltuială (bon/factură) și pune Claude să scoată datele structurate. */
@Service
public class ExpensePdfParser {

    private final ClaudeClient claudeClient;
    private final PdfTextExtractor pdfTextExtractor;

    public ExpensePdfParser(ClaudeClient claudeClient, PdfTextExtractor pdfTextExtractor) {
        this.claudeClient = claudeClient;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    public ParsedExpense parse(MultipartFile file) throws IOException {
        String text = pdfTextExtractor.extract(file);
        String prompt = """
                Ești un asistent care extrage o cheltuială dintr-un document românesc (bon fiscal,
                chitanță sau factură). Extrage câmpurile cerute din textul de mai jos. Suma e număr
                (fără simbol valutar), punct pentru zecimale. Data în format YYYY-MM-DD. Alege o
                categorie potrivită. Dacă un câmp lipsește, lasă-l gol (0 pentru sumă).

                === TEXT DOCUMENT ===
                """ + text;
        return claudeClient.extractStructured(prompt, ParsedExpense.class);
    }
}
