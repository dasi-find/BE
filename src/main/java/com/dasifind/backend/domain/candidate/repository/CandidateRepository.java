package com.dasifind.backend.domain.candidate.repository;

import com.dasifind.backend.domain.candidate.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    String CURRENT = """
            c.assessedAnalysisId = c.searchCard.analysisId
            and c.assessedPoliceItemVersion = c.policeItem.version
            """;
    String INCLUDED = "(c.feedback is null or c.feedback <> com.dasifind.backend.domain.candidate.model.CandidateFeedback.NOT_MINE)";

    Optional<Candidate> findBySearchCardIdAndPoliceItemId(Long searchCardId, Long policeItemId);

    @EntityGraph(attributePaths = {"searchCard", "policeItem"})
    @Query(value = "select c from Candidate c where c.searchCard.id = :cardId and " + CURRENT
            + " and c.totalScore is not null and (:minScore is null or c.totalScore >= :minScore)"
            + " and (:includeExcluded = true or " + INCLUDED + ")"
            + " order by c.totalScore desc, case when c.policeItem.foundDate is null then 1 else 0 end,"
            + " c.policeItem.foundDate desc, c.id asc",
            countQuery = "select count(c) from Candidate c where c.searchCard.id = :cardId and " + CURRENT
            + " and c.totalScore is not null and (:minScore is null or c.totalScore >= :minScore)"
            + " and (:includeExcluded = true or " + INCLUDED + ")")
    Page<Candidate> findVisiblePage(@Param("cardId") Long cardId, @Param("minScore") BigDecimal minScore,
                                   @Param("includeExcluded") boolean includeExcluded, Pageable pageable);

    @EntityGraph(attributePaths = {"searchCard", "policeItem"})
    @Query("select c from Candidate c where c.id = :id")
    Optional<Candidate> findWithItemsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Candidate c where c.id = :id")
    Optional<Candidate> findByIdForUpdate(@Param("id") Long id);

    @Query("select count(c) from Candidate c where c.searchCard.id = :cardId and " + CURRENT
            + " and " + INCLUDED + """
             and (c.totalScore > :score or (c.totalScore = :score and (
                 (:date is null and c.policeItem.foundDate is not null)
                 or c.policeItem.foundDate > :date
                 or (((:date is null and c.policeItem.foundDate is null) or c.policeItem.foundDate = :date)
                     and c.id < :id))))
            """)
    long countAhead(@Param("cardId") Long cardId, @Param("score") BigDecimal score,
                    @Param("date") LocalDate date, @Param("id") Long id);
}
