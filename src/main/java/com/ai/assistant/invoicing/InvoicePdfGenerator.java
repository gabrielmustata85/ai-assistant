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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;

/**
 * Generează un PDF de factură în stil clasic (factureaza.ro): vânzător + cumpărător cu date complete,
 * bandă serie/dată/termen, tabel cu linii (UM/cantitate/preț unitar/valoare) și total.
 * Vânzătorul (la factură emisă) se ia din firma selectată; cumpărătorul din Colaboratori.
 * Fonturile standard PDF nu au diacritice, deci textul e transliterat ASCII.
 */
@Component
public class InvoicePdfGenerator {

    private static final PDFont FONT = PDType1Font.HELVETICA;
    private static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;
    private static final float[] TEAL = {0.24f, 0.60f, 0.63f};
    private static final float[] GREY = {0.45f, 0.45f, 0.45f};
    private static final float LEFT = 50, RIGHT = 545;

    public byte[] generate(Invoice inv, Company company) {
        return generate(inv, company, null);
    }

    public byte[] generate(Invoice inv, Company company, Partner partner) {
        boolean issued = inv.getDirection() == Direction.ISSUED;
        Party seller = issued ? Party.of(company) : Party.of(partner, inv);
        Party buyer = issued ? Party.of(partner, inv) : Party.of(company);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ---- Vânzător (dreapta sus) ----
                float y = 810;
                right(cs, BOLD, 9, RIGHT, y, "Vanzator:", null); y -= 15;
                right(cs, BOLD, 15, RIGHT, y, seller.name, TEAL); y -= 15;
                for (String l : seller.lines(true)) { right(cs, FONT, 8, RIGHT, y, l, null); y -= 10.5f; }

                // ---- Cumpărător (stânga) ----
                y -= 24;
                text(cs, BOLD, 9, LEFT, y, "Cumparator:", null); y -= 15;
                text(cs, BOLD, 15, LEFT, y, buyer.name, TEAL); y -= 15;
                for (String l : buyer.lines(false)) { text(cs, FONT, 8, LEFT, y, l, null); y -= 10.5f; }

                // ---- Banda factură (serie/dată/termen) ----
                y -= 20;
                line(cs, LEFT, y, RIGHT, y); y -= 16;
                text(cs, BOLD, 11, LEFT, y, "Factura " + nz(inv.getInvoiceNumber(), ""), null);
                right(cs, FONT, 9, RIGHT, y + 1, "Seria si numarul facturii: " + nz(inv.getInvoiceNumber(), "-"), null);
                right(cs, FONT, 9, RIGHT, y - 11, "Data facturii (zi.luna.an): " + roDate(inv.getIssueDate()), null);
                right(cs, FONT, 9, RIGHT, y - 22, "Termen de plata (zi.luna.an): " + roDate(inv.getDueDate()), null);
                y -= 34;
                line(cs, LEFT, y, RIGHT, y); y -= 18;

                // ---- Tabel linii ----
                float cDesc = 82, cUM = 300, cQtyR = 372, cPriceR = 455, cValR = RIGHT;
                fill(cs, LEFT, y - 4, RIGHT - LEFT, 20, 0.93f, 0.96f, 0.96f);
                text(cs, BOLD, 8, LEFT + 3, y, "Nr.", null);
                text(cs, BOLD, 8, cDesc, y, "Denumirea produselor sau a serviciilor", null);
                text(cs, BOLD, 8, cUM, y, "U.M.", null);
                right(cs, BOLD, 8, cQtyR, y, "Cantitate", null);
                right(cs, BOLD, 8, cPriceR, y, "Pret unitar", null);
                right(cs, BOLD, 8, cValR, y, "Valoare (RON)", null);
                y -= 18;

                BigDecimal qty = inv.getQuantity();
                BigDecimal price = inv.getUnitPrice();
                BigDecimal net = inv.getNetAmount() == null ? BigDecimal.ZERO : inv.getNetAmount();
                text(cs, FONT, 9, LEFT + 3, y, "1", null);
                text(cs, FONT, 9, cDesc, y, nz(inv.getCategory(), "Produse / servicii"), null);
                text(cs, FONT, 9, cUM, y, nz(inv.getUnit(), "-"), null);
                right(cs, FONT, 9, cQtyR, y, qty == null ? "-" : num(qty), null);
                right(cs, FONT, 9, cPriceR, y, price == null ? "-" : money(price), null);
                right(cs, FONT, 9, cValR, y, money(net), null);
                y -= 22;
                line(cs, LEFT, y, RIGHT, y); y -= 8;

                // ---- TVA + total ----
                BigDecimal vat = inv.getVatAmount() == null ? BigDecimal.ZERO : inv.getVatAmount();
                BigDecimal gross = inv.getGrossAmount() == null ? net.add(vat) : inv.getGrossAmount();
                if (vat.signum() > 0) {
                    y -= 6;
                    right(cs, FONT, 9, cValR - 90, y, "Valoare fara TVA:", null);
                    right(cs, FONT, 9, cValR, y, money(net), null); y -= 13;
                    right(cs, FONT, 9, cValR - 90, y, "TVA:", null);
                    right(cs, FONT, 9, cValR, y, money(vat), null); y -= 16;
                }
                fill(cs, LEFT, y - 5, RIGHT - LEFT, 22, 0.93f, 0.96f, 0.96f);
                text(cs, BOLD, 11, LEFT + 10, y, "Valoare totala de plata factura curenta  -RON-", null);
                right(cs, BOLD, 12, RIGHT - 8, y, money(gross), null);

                // ---- Footer ----
                float fy = 70;
                line(cs, LEFT, fy + 22, RIGHT, fy + 22);
                text(cs, FONT, 7, LEFT, fy + 10, seller.name, GREY);
                text(cs, FONT, 7, LEFT, fy, val("Nr. ord. reg. com. / an", seller.regCom), GREY);
                text(cs, FONT, 7, LEFT, fy - 10, val("CIF", seller.cui), GREY);
                text(cs, FONT, 7, 250, fy + 10, val("Adresa", seller.address), GREY);
                text(cs, FONT, 7, 250, fy, val("Telefon", seller.phone), GREY);
                text(cs, FONT, 7, 420, fy + 10, val("Email", seller.email), GREY);
                text(cs, FONT, 7, 420, fy, val("Banca", seller.bank), GREY);
                text(cs, FONT, 7, 420, fy - 10, val("IBAN", seller.iban), GREY);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Nu am putut genera PDF-ul facturii: " + e.getMessage(), e);
        }
    }

    /** Datele unei părți (vânzător/cumpărător) de pe factură. */
    private static final class Party {
        String name = "-", cui, regCom, address, phone, email, bank, iban;

        static Party of(Company c) {
            Party p = new Party();
            p.name = nz(c.getName(), "-");
            p.cui = c.getCui();
            p.regCom = c.getRegCom();
            p.address = c.getAddress();
            p.phone = c.getPhone();
            p.email = c.getEmail();
            p.bank = c.getBank();
            p.iban = c.getIban();
            return p;
        }

        static Party of(Partner partner, Invoice inv) {
            Party p = new Party();
            if (partner != null) {
                p.name = nz(partner.getName(), nz(inv.getPartnerName(), "-"));
                p.cui = blank(partner.getCui()) ? inv.getPartnerCui() : partner.getCui();
                p.regCom = partner.getRegCom();
                p.address = partner.getAddress();
                p.phone = partner.getPhone();
                p.email = partner.getEmail();
                p.iban = partner.getIban();
            } else {
                p.name = nz(inv.getPartnerName(), "-");
                p.cui = inv.getPartnerCui();
            }
            return p;
        }

        java.util.List<String> lines(boolean withBank) {
            java.util.List<String> out = new java.util.ArrayList<>();
            if (!blank(regCom)) out.add("Nr. ord. reg. com. / an: " + regCom);
            if (!blank(cui)) out.add("CIF: " + cui);
            if (!blank(address)) out.add("Adresa: " + address);
            if (!blank(phone)) out.add("Telefon: " + phone);
            if (!blank(email)) out.add("Email: " + email);
            if (withBank && !blank(bank)) out.add("Banca: " + bank);
            if (withBank && !blank(iban)) out.add("IBAN(RON): " + iban);
            return out;
        }
    }

    // ---------- helpers de desen ----------

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String s, float[] rgb) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        if (rgb != null) cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(s));
        cs.endText();
        if (rgb != null) cs.setNonStrokingColor(0f, 0f, 0f);
    }

    private static void right(PDPageContentStream cs, PDFont font, float size, float rightX, float y, String s, float[] rgb) throws IOException {
        String t = ascii(s);
        float w = font.getStringWidth(t) / 1000f * size;
        text(cs, font, size, rightX - w, y, s, rgb);
    }

    private static void line(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.setStrokingColor(0.8f, 0.83f, 0.83f);
        cs.setLineWidth(0.6f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
        cs.setStrokingColor(0f, 0f, 0f);
    }

    private static void fill(PDPageContentStream cs, float x, float y, float w, float h, float r, float g, float b) throws IOException {
        cs.setNonStrokingColor(r, g, b);
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.setNonStrokingColor(0f, 0f, 0f);
    }

    // ---------- helpers de text ----------

    private static String val(String label, String v) {
        return label + ": " + (blank(v) ? "-" : v);
    }

    private static String money(BigDecimal v) {
        if (v == null) return "0,00";
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", s).format(v);
    }

    private static String num(BigDecimal v) {
        if (v == null) return "-";
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.###", s).format(v);
    }

    private static String roDate(Object o) {
        if (o == null) return "-";
        try {
            LocalDate d = LocalDate.parse(String.valueOf(o));
            return String.format("%02d.%02d.%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static String nz(String s, String fallback) {
        return blank(s) ? fallback : s;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** Fonturile standard PDF nu au diacritice — le transliterăm ca să nu crape encoding-ul. */
    private static String ascii(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replace('ș', 's').replace('Ș', 'S')
                .replace('ț', 't').replace('Ț', 'T')
                .replaceAll("[^\\x00-\\x7F]", "");
    }
}
