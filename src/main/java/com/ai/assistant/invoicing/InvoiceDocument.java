package com.ai.assistant.invoicing;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Fișierul PDF original încărcat pentru una sau mai multe facturi. */
@Entity
@Table(name = "invoice_document")
@Data
@NoArgsConstructor
public class InvoiceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(length = 255)
    private String filename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    // byte[] simplu -> coloană bytea în Postgres (nu large object).
    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public InvoiceDocument(Long companyId, String filename, String contentType, byte[] data) {
        this.companyId = companyId;
        this.filename = filename;
        this.contentType = contentType;
        this.data = data;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
