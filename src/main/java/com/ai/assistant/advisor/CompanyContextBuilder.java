package com.ai.assistant.advisor;

import com.ai.assistant.company.Company;
import com.ai.assistant.company.CompanyService;
import com.ai.assistant.invoicing.Invoice;
import com.ai.assistant.invoicing.InvoiceService;
import com.ai.assistant.payroll.Employee;
import com.ai.assistant.payroll.Expense;
import com.ai.assistant.payroll.PayrollService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompanyContextBuilder {

    private final CompanyService companyService;
    private final InvoiceService invoiceService;
    private final PayrollService payrollService;

    public CompanyContextBuilder(CompanyService companyService,
                                 InvoiceService invoiceService,
                                 PayrollService payrollService) {
        this.companyService = companyService;
        this.invoiceService = invoiceService;
        this.payrollService = payrollService;
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
        sb.append("ANGAJAȚI (").append(employees.size()).append("):\n");
        for (Employee e : employees) {
            sb.append("- ").append(e.getFullName())
              .append(" | salariu brut ").append(e.getGrossSalary())
              .append(" | activ ").append(e.isActive())
              .append("\n");
        }
        sb.append("\n");

        List<Expense> expenses = payrollService.expenses(companyId);
        sb.append("ALTE CHELTUIELI (").append(expenses.size()).append("):\n");
        for (Expense x : expenses) {
            sb.append("- ").append(x.getDescription())
              .append(" | ").append(x.getCategory())
              .append(" | sumă ").append(x.getAmount())
              .append(" | deductibil ").append(x.isDeductible())
              .append("\n");
        }

        return sb.toString();
    }
}
