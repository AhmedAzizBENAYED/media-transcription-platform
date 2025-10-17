package com.ahmedaziz.mediatranscriptionplatform.service;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionCompletedEvent;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.repository.MediaFileRepository;
import com.ahmedaziz.mediatranscriptionplatform.service.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionProcessingService {

    private final MediaFileRepository mediaFileRepository;
    private final TranscriptionService transcriptionService;
    private final KafkaProducerService kafkaProducerService;

    @Value("${app.transcription.max-retries}")
    private int maxRetries;

    @Async
    @Transactional
    public void processTranscriptionAsync(Long mediaFileId) {
        log.info("Starting async transcription processing for file ID: {}", mediaFileId);

        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Media file not found with ID: " + mediaFileId));

        try {
            // Update status to PROCESSING
            mediaFile.setStatus(MediaFile.ProcessingStatus.PROCESSING);
            mediaFile.setProcessingStartedAt(LocalDateTime.now());
            mediaFileRepository.save(mediaFile);

            log.info("Processing media file ID: {} - {}", mediaFile.getId(), mediaFile.getOriginalFilename());

            // Perform transcription
            TranscriptionResult result = transcriptionService.transcribe(mediaFile);

            // Update status to COMPLETED
            mediaFile.setStatus(MediaFile.ProcessingStatus.COMPLETED);
            mediaFile.setCompletedAt(LocalDateTime.now());
            mediaFile.setErrorMessage(null);
            mediaFileRepository.save(mediaFile);

            // Send completion event
            TranscriptionCompletedEvent event = TranscriptionCompletedEvent.builder()
                    .mediaFileId(mediaFile.getId())
                    .transcriptionResultId(result.getId())
                    .status("COMPLETED")
                    .completedAt(LocalDateTime.now())
                    .build();

            kafkaProducerService.sendTranscriptionCompletedEvent(event);

            log.info("Transcription completed successfully for file ID: {}", mediaFileId);

        } catch (Exception e) {
            log.error("Error processing transcription for file ID: {}", mediaFileId, e);
            handleTranscriptionError(mediaFile, e);
        }
    }

    @Transactional
    protected void handleTranscriptionError(MediaFile mediaFile, Exception error) {
        mediaFile.setRetryCount(mediaFile.getRetryCount() + 1);

        if (mediaFile.getRetryCount() >= maxRetries) {
            // Max retries reached - mark as failed
            mediaFile.setStatus(MediaFile.ProcessingStatus.FAILED);
            mediaFile.setErrorMessage(error.getMessage());
            mediaFile.setCompletedAt(LocalDateTime.now());

            log.error("Max retries reached for file ID: {}. Marking as FAILED", mediaFile.getId());

            // Send failure event
            TranscriptionCompletedEvent event = TranscriptionCompletedEvent.builder()
                    .mediaFileId(mediaFile.getId())
                    .status("FAILED")
                    .completedAt(LocalDateTime.now())
                    .errorMessage(error.getMessage())
                    .build();

            kafkaProducerService.sendTranscriptionCompletedEvent(event);

        } else {
            // Retry - set back to UPLOADED status
            mediaFile.setStatus(MediaFile.ProcessingStatus.UPLOADED);
            log.warn("Transcription failed for file ID: {}. Retry count: {}/{}",
                    mediaFile.getId(), mediaFile.getRetryCount(), maxRetries);
        }

        mediaFileRepository.save(mediaFile);
    }
}
