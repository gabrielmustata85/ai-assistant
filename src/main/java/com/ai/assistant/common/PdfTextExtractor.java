package com.ai.assistant.common;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/** Extrage textul dintr-un PDF încărcat (folosit de extragerea de facturi și cheltuieli). */
@Component
public class PdfTextExtractor {

    public String extract(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new IOException("Nu s-a putut extrage text din PDF (poate e scanat/imagine).");
            }
            return text;
        }
    }
}
