package com.ahmedaziz.mediatranscriptionplatform.service;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.repository.MediaFileRepository;
import com.ahmedaziz.mediatranscriptionplatform.repository.TranscriptionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionResultService {

    private final TranscriptionResultRepository transcriptionResultRepository;
    private final MediaFileRepository mediaFileRepository;

    @Cacheable(value = "transcriptions", key = "#mediaFileId")
    @Transactional(readOnly = true)
    public TranscriptionResult getTranscriptionByMediaFileId(Long mediaFileId) {
        log.info("Fetching transcription for media file ID: {}", mediaFileId);

        TranscriptionResult result = transcriptionResultRepository.findByMediaFileId(mediaFileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transcription not found for media file ID: " + mediaFileId));

        log.debug("Transcription found: {} words, language: {}",
                result.getWordCount(), result.getLanguage());

        return result;
    }

    @Transactional(readOnly = true)
    public TranscriptionResult getTranscriptionById(Long id) {
        return transcriptionResultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transcription not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<TranscriptionResult> getAllTranscriptions() {
        return transcriptionResultRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean hasTranscription(Long mediaFileId) {
        return transcriptionResultRepository.existsByMediaFileId(mediaFileId);
    }

    @CacheEvict(value = "transcriptions", key = "#mediaFileId")
    @Transactional
    public void deleteTranscription(Long mediaFileId) {
        transcriptionResultRepository.findByMediaFileId(mediaFileId)
                .ifPresent(result -> {
                    transcriptionResultRepository.delete(result);
                    log.info("Deleted transcription for media file ID: {}", mediaFileId);
                });
    }

    @Transactional(readOnly = true)
    public TranscriptionStatusResponse getTranscriptionStatus(Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Media file not found with ID: " + mediaFileId));

        boolean hasTranscription = transcriptionResultRepository.existsByMediaFileId(mediaFileId);

        String message = switch (mediaFile.getStatus()) {
            case UPLOADED -> "File uploaded, waiting for processing";
            case QUEUED -> "File queued for processing";
            case PROCESSING -> "Transcription in progress";
            case COMPLETED -> hasTranscription ?
                    "Transcription completed successfully" :
                    "Processing completed but transcription not found";
            case FAILED -> "Transcription failed: " +
                    (mediaFile.getErrorMessage() != null ? mediaFile.getErrorMessage() : "Unknown error");
        };

        return TranscriptionStatusResponse.builder()
                .mediaFileId(mediaFileId)
                .status(mediaFile.getStatus().name())
                .hasTranscription(hasTranscription)
                .message(message)
                .processingStartedAt(mediaFile.getProcessingStartedAt())
                .completedAt(mediaFile.getCompletedAt())
                .errorMessage(mediaFile.getErrorMessage())
                .retryCount(mediaFile.getRetryCount())
                .build();
    }

    @Transactional(readOnly = true)
    public TranscriptionStatistics getStatistics() {
        long totalTranscriptions = transcriptionResultRepository.count();
        Double avgProcessingTime = transcriptionResultRepository.getAverageProcessingTime();
        Double avgConfidence = transcriptionResultRepository.getAverageConfidence();

        return TranscriptionStatistics.builder()
                .totalTranscriptions(totalTranscriptions)
                .averageProcessingTimeMs(avgProcessingTime != null ? avgProcessingTime.longValue() : 0L)
                .averageConfidence(avgConfidence != null ? avgConfidence : 0.0)
                .build();
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class TranscriptionStatusResponse {
        private Long mediaFileId;
        private String status;
        private boolean hasTranscription;
        private String message;
        private java.time.LocalDateTime processingStartedAt;
        private java.time.LocalDateTime completedAt;
        private String errorMessage;
        private Integer retryCount;
    }

    @lombok.Data
    @lombok.Builder
    public static class TranscriptionStatistics {
        private Long totalTranscriptions;
        private Long averageProcessingTimeMs;
        private Double averageConfidence;
    }
}