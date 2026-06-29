package com.ai.assistant.invoicing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Direction direction;

    @Column(name = "invoice_number", length = 64)
    private String invoiceNumber;

    @Column(name = "partner_name")
    private String partnerName;

    @Column(name = "partner_cui", length = 32)
    private String partnerCui;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(length = 64)
    private String category;

    @Column(nullable = false)
    private boolean deductible;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
