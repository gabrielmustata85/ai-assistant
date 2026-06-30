package com.ai.assistant.invoicing;

import com.ai.assistant.ai.ClaudeClient;
import com.ai.assistant.company.Company;
import com.ai.assistant.company.CompanyService;
import org.springframework.stereotype.Service;

/** Generează o schiță de factură dintr-o instrucțiune în limbaj natural, prin asistentul Marius. */
@Service
public class InvoiceAssistantService {

    private final CompanyService companyService;
    private final ClaudeClient claudeClient;

    public InvoiceAssistantService(CompanyService companyService, ClaudeClient claudeClient) {
        this.companyService = companyService;
        this.claudeClient = claudeClient;
    }

    public InvoiceDraft draft(Long companyId, String instruction) {
        Company c = companyService.get(companyId);   // verifică ownership
        boolean vat = Boolean.TRUE.equals(c.getVatPayer());
        String today = java.time.LocalDate.now().toString();

        String prompt = "Te numești Marius, asistentul fiscal. Pregătești o SCHIȚĂ de factură pe care "
                + "userul o va verifica și emite manual. NU inventa date pe care userul nu le-a dat.\n"
                + "Azi este " + today + ". Firma care emite: " + c.getName()
                + " (plătitor TVA: " + (vat ? "DA" : "NU") + ").\n\n"
                + "Reguli:\n"
                + "- direction = ISSUED (firma emite către client), dacă userul nu spune altfel.\n"
                + "- Pune clientul în partnerName și CUI-ul în partnerCui dacă e dat.\n"
                + "- Sume: dacă firma e plătitoare de TVA și userul dă o sumă FĂRĂ TVA, aplică TVA 19% "
                + "(vatAmount = net*0.19, gross = net+vat). Dacă suma e CU TVA inclus, desparte invers "
                + "(net = gross/1.19). Dacă NU e plătitoare de TVA, vatAmount=0 și gross=net.\n"
                + "- issueDate = data de azi dacă nu se specifică. dueDate doar dacă userul o dă.\n"
                + "- invoiceNumber: lasă gol dacă userul nu l-a dat.\n"
                + "- ready=true DOAR dacă ai cel puțin numele clientului ȘI o sumă. Altfel ready=false și "
                + "pune în `missing` întrebări scurte (ex: „Pentru ce client emit factura?”, „Ce sumă?”).\n"
                + "- `message`: o frază scurtă (ce ai pregătit sau ce îți mai trebuie).\n\n"
                + "Instrucțiunea userului:\n" + instruction;

        return claudeClient.extractStructuredHeavy(prompt, InvoiceDraft.class);
    }
}
