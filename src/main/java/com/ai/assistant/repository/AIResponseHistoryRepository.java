package com.ai.assistant.repository;

import com.ai.assistant.model.AIResponseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIResponseHistoryRepository extends JpaRepository<AIResponseHistory, Long> {

    List<AIResponseHistory> findBySessionIdOrderByTimestampDesc(String sessionId);

    List<AIResponseHistory> findByUserQueryContainingIgnoreCaseOrderByTimestampDesc(String query);

    @Query("SELECT h FROM AIResponseHistory h WHERE h.sessionId = :sessionId AND h.timestamp > :since ORDER BY h.timestamp DESC")
    List<AIResponseHistory> findRecentBySessionId(
            @Param("sessionId") String sessionId,
            @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM AIResponseHistory h WHERE h.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT h FROM AIResponseHistory h WHERE h.wasCorrected = true ORDER BY h.correctionTimestamp DESC")
    List<AIResponseHistory> findAllCorrectedResponses();

    @Query(value = """
            
                SELECT h.* FROM ai_response_history h
            WHERE h.session_id = :sessionId
            ORDER BY h.timestamp DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<AIResponseHistory> findLatestBySessionId(
            @Param("sessionId") String sessionId,
            @Param("limit") int limit);
}
