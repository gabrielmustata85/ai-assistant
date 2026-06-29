package com.ai.assistant.payroll;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 13)
    private String cnp;

    @Column(name = "gross_salary", nullable = false)
    private BigDecimal grossSalary;

    @Column(length = 128)
    private String position;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(nullable = false)
    private boolean active = true;
}
