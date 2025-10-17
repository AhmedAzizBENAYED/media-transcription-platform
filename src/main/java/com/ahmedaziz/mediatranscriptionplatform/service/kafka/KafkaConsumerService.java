package com.ahmedaziz.mediatranscriptionplatform.service.kafka;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaUploadEvent;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionCompletedEvent;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;

import com.ahmedaziz.mediatranscriptionplatform.repository.MediaFileRepository;
import com.ahmedaziz.mediatranscriptionplatform.service.TranscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final MediaFileRepository mediaFileRepository;
    private final TranscriptionService transcriptionService;
    private final KafkaProducerService kafkaProducerService;
    private final CacheManager cacheManager;

    @KafkaListener(
            topics = "${kafka.topics.media-uploaded}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consumeMediaUploadedEvent(
            @Payload MediaUploadEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("Received media upload event - File ID: {}, Partition: {}",
                event.getMediaFileId(), partition);

        try {
            // Fetch media file from database
            MediaFile mediaFile = mediaFileRepository.findById(event.getMediaFileId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Media file not found with ID: " + event.getMediaFileId()));

            // Update status to PROCESSING
            mediaFile.setStatus(MediaFile.ProcessingStatus.PROCESSING);
            mediaFile.setProcessingStartedAt(LocalDateTime.now());
            mediaFileRepository.save(mediaFile);

            log.info("Processing media file: {}", mediaFile.getOriginalFilename());

            // Perform transcription
            TranscriptionResult result = transcriptionService.transcribe(mediaFile);

            // Update status to COMPLETED
            mediaFile.setStatus(MediaFile.ProcessingStatus.COMPLETED);
            mediaFile.setCompletedAt(LocalDateTime.now());
            mediaFile.setErrorMessage(null);
            mediaFileRepository.save(mediaFile);

            // Cache the result
            cacheTranscriptionResult(result);

            // Send completion event
            TranscriptionCompletedEvent completionEvent = TranscriptionCompletedEvent.builder()
                    .mediaFileId(mediaFile.getId())
                    .transcriptionResultId(result.getId())
                    .status("COMPLETED")
                    .completedAt(LocalDateTime.now())
                    .build();

            kafkaProducerService.sendTranscriptionCompletedEvent(completionEvent);

            log.info("Transcription completed successfully for file ID: {}", mediaFile.getId());

        } catch (Exception e) {
            log.error("Error processing media upload event for file ID: {}",
                    event.getMediaFileId(), e);
            handleTranscriptionError(event.getMediaFileId(), e);
        }
    }

    private void handleTranscriptionError(Long mediaFileId, Exception error) {
        try {
            MediaFile mediaFile = mediaFileRepository.findById(mediaFileId).orElse(null);
            if (mediaFile != null) {
                mediaFile.setStatus(MediaFile.ProcessingStatus.FAILED);
                mediaFile.setErrorMessage(error.getMessage());
                mediaFile.setCompletedAt(LocalDateTime.now());
                mediaFileRepository.save(mediaFile);

                // Send failure event
                TranscriptionCompletedEvent event = TranscriptionCompletedEvent.builder()
                        .mediaFileId(mediaFileId)
                        .status("FAILED")
                        .completedAt(LocalDateTime.now())
                        .errorMessage(error.getMessage())
                        .build();

                kafkaProducerService.sendTranscriptionCompletedEvent(event);
            }
        } catch (Exception e) {
            log.error("Error handling transcription failure", e);
        }
    }

    private void cacheTranscriptionResult(TranscriptionResult result) {
        try {
            var cache = cacheManager.getCache("transcriptions");
            if (cache != null) {
                cache.put(result.getMediaFileId(), result);
                log.debug("Cached transcription result for media file ID: {}", result.getMediaFileId());
            }
        } catch (Exception e) {
            log.warn("Failed to cache transcription result: {}", e.getMessage());
        }
    }
}