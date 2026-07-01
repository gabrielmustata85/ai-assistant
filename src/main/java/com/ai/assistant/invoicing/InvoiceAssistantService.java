package com.ai.assistant.invoicing;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.company.Company;
import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Generează o schiță de factură dintr-o instrucțiune în limbaj natural, prin asistentul Marius. */
@Service
public class InvoiceAssistantService {

    private static final int MAX_REF = 20;

    private final CompanyService companyService;
    private final InvoiceService invoiceService;
    private final ClaudeClient claudeClient;

    public InvoiceAssistantService(CompanyService companyService, InvoiceService invoiceService, ClaudeClient claudeClient) {
        this.companyService = companyService;
        this.invoiceService = invoiceService;
        this.claudeClient = claudeClient;
    }

    public InvoiceDraft draft(Long companyId, String instruction) {
        Company c = companyService.get(companyId);   // verifică ownership
        boolean vat = Boolean.TRUE.equals(c.getVatPayer());
        String today = java.time.LocalDate.now().toString();

        List<Invoice> invoices = invoiceService.listForCompany(companyId);
        String reference = buildReference(invoices);

        String prompt = "Te numești Marius, asistentul fiscal. Pregătești o SCHIȚĂ de factură pe care "
                + "userul o va verifica și emite manual. NU inventa date pe care userul nu le-a dat, "
                + "DAR refolosește datele existente ale firmei (mai jos) pentru consecvență.\n"
                + "Azi este " + today + ". Firma care emite: " + c.getName()
                + " (plătitor TVA: " + (vat ? "DA" : "NU") + ").\n\n"
                + reference + "\n"
                + "Reguli:\n"
                + "- direction = ISSUED (firma emite către client), dacă userul nu spune altfel.\n"
                + "- Dacă clientul cerut apare deja în „Parteneri cunoscuți”, folosește EXACT același "
                + "partnerName și partnerCui de acolo (nu rescrie, nu prescurta).\n"
                + "- invoiceNumber: CONTINUĂ seria facturilor emise — următorul număr după ultimul, "
                + "păstrând exact același format/serie (ex: dacă ultima e FCT-0007, pune FCT-0008). "
                + "Dacă nu există nicio factură emisă, lasă invoiceNumber gol.\n"
                + "- Sume: dacă firma e plătitoare de TVA și userul dă o sumă FĂRĂ TVA, aplică TVA 19% "
                + "(vatAmount = net*0.19, gross = net+vat). Dacă suma e CU TVA inclus, desparte invers "
                + "(net = gross/1.19). Dacă NU e plătitoare de TVA, vatAmount=0 și gross=net.\n"
                + "- issueDate = data de azi dacă nu se specifică. dueDate doar dacă userul o dă.\n"
                + "- ready=true DOAR dacă ai cel puțin numele clientului ȘI o sumă. Altfel ready=false și "
                + "pune în `missing` întrebări scurte (ex: „Pentru ce client emit factura?”, „Ce sumă?”).\n"
                + "- `message`: o frază scurtă (ce ai pregătit sau ce îți mai trebuie).\n\n"
                + "Instrucțiunea userului:\n" + instruction;

        return claudeClient.extractStructuredHeavy(prompt, InvoiceDraft.class);
    }

    /** Construiește contextul de referință: parteneri cunoscuți + facturi emise (pentru numerotare). */
    private String buildReference(List<Invoice> invoices) {
        Set<String> partners = new LinkedHashSet<>();
        StringBuilder issued = new StringBuilder();
        String lastIssuedNumber = null;
        long lastTrailing = -1;
        int issuedCount = 0;

        for (Invoice inv : invoices) {
            if (inv.getPartnerName() != null && !inv.getPartnerName().isBlank()) {
                partners.add(inv.getPartnerName() + " | CUI: "
                        + (inv.getPartnerCui() == null ? "-" : inv.getPartnerCui()));
            }
            if (inv.getDirection() == Direction.ISSUED && inv.getInvoiceNumber() != null
                    && !inv.getInvoiceNumber().isBlank()) {
                if (issuedCount < MAX_REF) {
                    issued.append("  - ").append(inv.getInvoiceNumber())
                          .append(" | ").append(inv.getPartnerName())
                          .append(" | ").append(inv.getIssueDate()).append("\n");
                }
                issuedCount++;
                long t = trailingNumber(inv.getInvoiceNumber());
                if (t >= lastTrailing) {   // seria cu cel mai mare număr de final
                    lastTrailing = t;
                    lastIssuedNumber = inv.getInvoiceNumber();
                }
            }
        }

        StringBuilder sb = new StringBuilder("FACTURI EXISTENTE (referință):\n");
        sb.append("Parteneri cunoscuți:\n");
        if (partners.isEmpty()) {
            sb.append("  (niciunul încă)\n");
        } else {
            int i = 0;
            for (String p : partners) {
                if (i++ >= MAX_REF) break;
                sb.append("  - ").append(p).append("\n");
            }
        }
        sb.append("Facturi EMISE (număr | client | data):\n");
        sb.append(issued.length() == 0 ? "  (niciuna încă)\n" : issued);
        if (lastIssuedNumber != null) {
            sb.append("Ultimul număr de factură emis: ").append(lastIssuedNumber)
              .append(" (continuă de aici)\n");
        }
        return sb.toString();
    }

    /** Numărul de la finalul unui identificator de factură (ex: „FCT-0007” -> 7), sau -1 dacă nu are. */
    private long trailingNumber(String number) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*$").matcher(number);
        try {
            return m.find() ? Long.parseLong(m.group(1)) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

