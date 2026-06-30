package com.ai.assistant.advisor;

import com.ai.assistant.bank.BankService;
import com.ai.assistant.bank.BankTransaction;
import com.ai.assistant.bank.TxnDirection;
import com.ai.assistant.company.Company;
import com.ai.assistant.company.CompanyService;
import com.ai.assistant.invoicing.Invoice;
import com.ai.assistant.invoicing.InvoiceService;
import com.ai.assistant.payroll.Employee;
import com.ai.assistant.payroll.Expense;
import com.ai.assistant.payroll.PayrollService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CompanyContextBuilder {

    private static final int MAX_BANK_TXN = 20;

    private final CompanyService companyService;
    private final InvoiceService invoiceService;
    private final PayrollService payrollService;
    private final BankService bankService;

    public CompanyContextBuilder(CompanyService companyService,
                                 InvoiceService invoiceService,
                                 PayrollService payrollService,
                                 BankService bankService) {
        this.companyService = companyService;
        this.invoiceService = invoiceService;
        this.payrollService = payrollService;
        this.bankService = bankService;
    }

    public String build(Long companyId) {
        Company c = companyService.get(companyId);
        StringBuilder sb = new StringBuilder();

        sb.append("FIRMA:\n");
        sb.append("- CUI: ").append(c.getCui()).append("\n");
        sb.append("- Denumire: ").append(c.getName()).append("\n");
        sb.append("- Tip: ").append(c.getCompanyType()).append("\n");
        sb.append("- Regim fiscal: ").append(c.getTaxRegime()).append("\n");
        sb.append("- Plătitor TVA: ").append(Boolean.TRUE.equals(c.getVatPayer()) ? "DA" : "NU").append("\n\n");

        List<Invoice> invoices = invoiceService.listForCompany(companyId);
        sb.append("FACTURI (").append(invoices.size()).append("):\n");
        for (Invoice inv : invoices) {
            sb.append("- ").append(inv.getDirection())
              .append(" | ").append(inv.getIssueDate())
              .append(" | net ").append(inv.getNetAmount())
              .append(" | TVA ").append(inv.getVatAmount())
              .append(" | brut ").append(inv.getGrossAmount())
              .append(" | categorie ").append(inv.getCategory())
              .append(" | deductibil ").append(inv.isDeductible())
              .append("\n");
        }
        sb.append("\n");

        List<Employee> employees = payrollService.employees(companyId);
        BigDecimal grossPayroll = BigDecimal.ZERO;
        int activeCount = 0;
        for (Employee e : employees) {
            if (e.isActive() && e.getGrossSalary() != null) {
                grossPayroll = grossPayroll.add(e.getGrossSalary());
                activeCount++;
            }
        }
        sb.append("ANGAJAȚI (").append(employees.size())
          .append(" | activi ").append(activeCount)
          .append(" | fond salarii brut lunar ").append(grossPayroll)
          .append(" lei):\n");
        for (Employee e : employees) {
            sb.append("- ").append(e.getFullName())
              .append(" | salariu brut ").append(e.getGrossSalary())
              .append(" | activ ").append(e.isActive())
              .append("\n");
        }
        sb.append("Pe salarii se datorează lunar contribuții și impozit (CAS, CASS, impozit pe venit, ")
          .append("CAM), declarate prin Declarația 112, cu scadența pe 25 a lunii următoare.\n\n");

        List<Expense> expenses = payrollService.expenses(companyId);
        sb.append("ALTE CHELTUIELI (").append(expenses.size()).append("):\n");
        for (Expense x : expenses) {
            sb.append("- ").append(x.getDescription())
              .append(" | ").append(x.getCategory())
              .append(" | sumă ").append(x.getAmount())
              .append(" | deductibil ").append(x.isDeductible())
              .append("\n");
        }
        sb.append("\n");

        List<BankTransaction> txns = bankService.list(companyId);
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        for (BankTransaction t : txns) {
            if (t.getDirection() == TxnDirection.IN) totalIn = totalIn.add(t.getAmount());
            else totalOut = totalOut.add(t.getAmount());
        }
        sb.append("TRANZACȚII BANCARE (").append(txns.size())
          .append(" | total intrări ").append(totalIn)
          .append(" | total ieșiri ").append(totalOut).append("):\n");
        for (BankTransaction t : txns.stream().limit(MAX_BANK_TXN).toList()) {
            sb.append("- ").append(t.getTxnDate())
              .append(" | ").append(t.getDirection())
              .append(" | sumă ").append(t.getAmount())
              .append(" | ").append(t.getDescription())
              .append(t.getCounterparty() != null ? " | " + t.getCounterparty() : "")
              .append("\n");
        }

        return sb.toString();
    }
}
