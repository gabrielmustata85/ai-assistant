package com.ai.assistant.payroll;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.common.PdfTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Extrage textul dintr-un PDF (bon/factură/export) și pune Claude să scoată TOATE cheltuielile. */
@Service
public class ExpensePdfParser {

    private final ClaudeClient claudeClient;
    private final PdfTextExtractor pdfTextExtractor;

    public ExpensePdfParser(ClaudeClient claudeClient, PdfTextExtractor pdfTextExtractor) {
        this.claudeClient = claudeClient;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    /** Întoarce TOATE cheltuielile din PDF (poate fi un bon sau un export cu mai multe). */
    public List<ParsedExpense> parse(MultipartFile file) throws IOException {
        String text = pdfTextExtractor.extract(file);
        String prompt = """
                Ești un asistent care extrage cheltuieli dintr-un document românesc (bon fiscal,
                chitanță, factură sau un export/tabel cu mai multe cheltuieli).
                ATENȚIE: documentul poate conține O SINGURĂ cheltuială sau MAI MULTE. Extrage-le pe TOATE,
                câte un element pentru fiecare. Pentru fiecare: descriere scurtă, categorie, suma (număr
                cu punct zecimal, fără simbol valutar), data (YYYY-MM-DD) și dacă pare deductibilă.
                Dacă un câmp lipsește, lasă-l gol (0 pentru sumă).

                === TEXT DOCUMENT ===
                """ + text;
        ParsedExpenses result = claudeClient.extractStructured(prompt, ParsedExpenses.class);
        return (result == null || result.expenses() == null) ? List.of() : result.expenses();
    }
}
