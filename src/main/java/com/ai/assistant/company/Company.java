package com.ai.assistant.company;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "company")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String cui;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 32)
    private CompanyType companyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 32)
    private TaxRegime taxRegime;

    @Column(name = "vat_payer", nullable = false)
    private Boolean vatPayer;

    // Date pentru facturare (vânzător)
    @Column(name = "reg_com", length = 64)
    private String regCom;

    @Column(length = 512)
    private String address;

    @Column(length = 34)
    private String iban;

    @Column(length = 64)
    private String bank;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
