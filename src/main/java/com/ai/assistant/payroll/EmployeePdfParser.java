package com.ai.assistant.payroll;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.common.PdfTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Extrage textul dintr-un stat de plată PDF și pune Claude să scoată TOȚI angajații. */
@Service
public class EmployeePdfParser {

    private final ClaudeClient claudeClient;
    private final PdfTextExtractor pdfTextExtractor;

    public EmployeePdfParser(ClaudeClient claudeClient, PdfTextExtractor pdfTextExtractor) {
        this.claudeClient = claudeClient;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    /** Întoarce TOȚI angajații din PDF (un stat de plată conține de obicei mai mulți). */
    public List<ParsedEmployee> parse(MultipartFile file) throws IOException {
        String text = pdfTextExtractor.extract(file);
        String prompt = """
                Ești un asistent care extrage angajații dintr-un document de salarizare românesc
                (stat de plată, pontaj, fluturaș sau un tabel cu mai mulți angajați).
                ATENȚIE: documentul conține de obicei MAI MULȚI angajați. Extrage-i pe TOȚI,
                câte un element pentru fiecare. Pentru fiecare: numele complet, CNP (dacă apare),
                salariul BRUT lunar (număr cu punct zecimal, fără simbol valutar), funcția (dacă apare)
                și data angajării (YYYY-MM-DD, dacă apare). Dacă în document apare doar salariul net,
                pune-l la grossSalary doar dacă nu există altă variantă. Dacă un câmp lipsește,
                lasă-l gol (0 pentru salariu).

                === TEXT DOCUMENT ===
                """ + text;
        ParsedEmployees result = claudeClient.extractStructured(prompt, ParsedEmployees.class);
        return (result == null || result.employees() == null) ? List.of() : result.employees();
    }
}
