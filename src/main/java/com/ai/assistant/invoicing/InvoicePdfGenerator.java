package com.ai.assistant.invoicing;

import com.ai.assistant.company.Company;
import com.ai.assistant.partner.Partner;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Generează un PDF de factură într-un șablon unic, ca toate facturile (încărcate sau generate cu AI)
 * să arate identic la descărcare. Fonturile standard nu au diacritice, deci textul e transliterat ASCII.
 */
@Component
public class InvoicePdfGenerator {

    private static final PDFont FONT = PDType1Font.HELVETICA;
    private static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;

    public byte[] generate(Invoice inv, Company company) {
        return generate(inv, company, null);
    }

    /** partner = datele colaboratorului (IBAN/adresă/telefon/email) preluate din Colaboratori. */
    public byte[] generate(Invoice inv, Company company, Partner partner) {
        boolean issued = inv.getDirection() == Direction.ISSUED;
        String supplierName = issued ? company.getName() : inv.getPartnerName();
        String supplierCui = issued ? company.getCui() : inv.getPartnerCui();
        String clientName = issued ? inv.getPartnerName() : company.getName();
        String clientCui = issued ? inv.getPartnerCui() : company.getCui();

        // Detaliile de contact ale PARTENERULUI (nu ale firmei noastre) — din Colaboratori.
        String pIban = partner == null ? null : partner.getIban();
        String pAddress = partner == null ? null : partner.getAddress();
        String pPhone = partner == null ? null : partner.getPhone();
        String pEmail = partner == null ? null : partner.getEmail();
        boolean partnerIsClient = issued;   // la factură emisă, partenerul e clientul (coloana dreapta)

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float w = page.getMediaBox().getWidth();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float left = 50, right = w - 50;
                float y = 800;

                text(cs, BOLD, 20, left, y, "FACTURA");
                text(cs, FONT, 10, right - 220, y + 4,
                        "Seria/Nr: " + nz(inv.getInvoiceNumber(), "-"));
                text(cs, FONT, 10, right - 220, y - 10,
                        "Data emiterii: " + nz(str(inv.getIssueDate()), "-"));
                text(cs, FONT, 10, right - 220, y - 24,
                        "Scadenta: " + nz(str(inv.getDueDate()), "-"));

                y -= 50;
                line(cs, left, y, right, y);
                y -= 22;

                // Furnizor (stânga) / Client (dreapta)
                float colR = left + 270;
                text(cs, BOLD, 10, left, y, "Furnizor");
                text(cs, BOLD, 10, colR, y, "Client");
                y -= 15;
                text(cs, FONT, 11, left, y, nz(supplierName, "-"));
                text(cs, FONT, 11, colR, y, nz(clientName, "-"));
                y -= 14;
                text(cs, FONT, 9, left, y, "CUI: " + nz(supplierCui, "-"));
                text(cs, FONT, 9, colR, y, "CUI: " + nz(clientCui, "-"));

                // Detaliile partenerului (din Colaboratori), sub coloana lui.
                float px = partnerIsClient ? colR : left;
                float py = y - 12;
                if (!blank(pIban))    { text(cs, FONT, 8, px, py, "IBAN: " + pIban); py -= 11; }
                if (!blank(pAddress)) { text(cs, FONT, 8, px, py, "Adresa: " + pAddress); py -= 11; }
                if (!blank(pPhone))   { text(cs, FONT, 8, px, py, "Tel: " + pPhone); py -= 11; }
                if (!blank(pEmail))   { text(cs, FONT, 8, px, py, "Email: " + pEmail); py -= 11; }

                y = Math.min(y - 34, py - 8);
                line(cs, left, y, right, y);
                y -= 16;

                // Antet tabel
                text(cs, BOLD, 9, left, y, "Descriere");
                text(cs, BOLD, 9, right - 300, y, "Net (lei)");
                text(cs, BOLD, 9, right - 190, y, "TVA (lei)");
                text(cs, BOLD, 9, right - 80, y, "Total (lei)");
                y -= 6;
                line(cs, left, y, right, y);
                y -= 16;

                // Linia (o singură poziție)
                text(cs, FONT, 10, left, y, nz(inv.getCategory(), "Produse / servicii"));
                text(cs, FONT, 10, right - 300, y, money(inv.getNetAmount()));
                text(cs, FONT, 10, right - 190, y, money(inv.getVatAmount()));
                text(cs, FONT, 10, right - 80, y, money(inv.getGrossAmount()));

                y -= 30;
                line(cs, right - 200, y, right, y);
                y -= 16;
                text(cs, FONT, 10, right - 200, y, "Total net:");
                text(cs, FONT, 10, right - 80, y, money(inv.getNetAmount()));
                y -= 14;
                text(cs, FONT, 10, right - 200, y, "TVA:");
                text(cs, FONT, 10, right - 80, y, money(inv.getVatAmount()));
                y -= 16;
                text(cs, BOLD, 12, right - 200, y, "TOTAL:");
                text(cs, BOLD, 12, right - 80, y, money(inv.getGrossAmount()) + " lei");

                text(cs, FONT, 8, left, 60,
                        "Document generat de Marius, asistentul fiscal. Sumele sunt orientative.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Nu am putut genera PDF-ul facturii: " + e.getMessage(), e);
        }
    }

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String s) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(s));
        cs.endText();
    }

    private static void line(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static String money(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String nz(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** Fonturile standard PDF nu au diacritice — le transliterăm ca să nu crape encoding-ul. */
    private static String ascii(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return n.replace('ș', 's').replace('Ș', 'S')
                .replace('ț', 't').replace('Ț', 'T')
                .replaceAll("[^\\x00-\\x7F]", "");
    }
}
