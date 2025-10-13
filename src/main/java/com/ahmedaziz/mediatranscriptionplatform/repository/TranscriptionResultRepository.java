package com.ahmedaziz.mediatranscriptionplatform.repository;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranscriptionResultRepository extends JpaRepository<TranscriptionResult, Long> {

    Optional<TranscriptionResult> findByMediaFileId(Long mediaFileId);

    boolean existsByMediaFileId(Long mediaFileId);

    @Query("SELECT t FROM TranscriptionResult t WHERE t.mediaFileId = :mediaFileId")
    Optional<TranscriptionResult> findTranscriptionByMediaFileId(Long mediaFileId);

    @Query("SELECT AVG(t.processingTimeMs) FROM TranscriptionResult t")
    Double getAverageProcessingTime();

    @Query("SELECT AVG(t.confidence) FROM TranscriptionResult t WHERE t.confidence IS NOT NULL")
    Double getAverageConfidence();
}