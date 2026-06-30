package com.ai.assistant.bank;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.common.PdfTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Extrage tranzacțiile dintr-un extras bancar PDF folosind Claude. */
@Service
public class BankStatementParser {

    private final ClaudeClient claudeClient;
    private final PdfTextExtractor pdfTextExtractor;

    public BankStatementParser(ClaudeClient claudeClient, PdfTextExtractor pdfTextExtractor) {
        this.claudeClient = claudeClient;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    public ParsedStatement parse(MultipartFile file) throws IOException {
        String text = pdfTextExtractor.extract(file);
        String prompt = """
                Ești un asistent care extrage tranzacțiile dintr-un extras de cont bancar românesc.
                Extrage TOATE tranzacțiile din textul de mai jos, în ordinea în care apar.
                Pentru fiecare: data (YYYY-MM-DD), descrierea, contrapartea (dacă apare),
                sensul (IN = încasare/credit, OUT = plată/debit) și suma (număr pozitiv, punct
                pentru zecimale, fără simbol valutar). Ignoră soldurile și rândurile de total.

                === TEXT EXTRAS BANCAR ===
                """ + text;
        return claudeClient.extractStructured(prompt, ParsedStatement.class);
    }
}
