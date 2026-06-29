package com.ai.assistant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_response_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResponseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_query", nullable = false, columnDefinition = "TEXT")
    private String userQuery;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "was_corrected")
    private Boolean wasCorrected = false;

    @Column(name = "corrected_response", columnDefinition = "TEXT")
    private String correctedResponse;

    @Column(name = "correction_timestamp")
    private LocalDateTime correctionTimestamp;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "data_gaps", columnDefinition = "TEXT")
    private String dataGaps;

    @Column(name = "embedding_vector", columnDefinition = "TEXT")
    private String embeddingVector; // Optional: store embedding for similarity search

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata; // Additional context as JSON
}
