package com.ahmedaziz.mediatranscriptionplatform.batch;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionCompletedEvent;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.service.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscriptionWriter implements ItemWriter<TranscriptionResult> {

    private final KafkaProducerService kafkaProducerService;
    private final CacheManager cacheManager;

    @Override
    public void write(Chunk<? extends TranscriptionResult> chunk) {
        for (TranscriptionResult result : chunk) {
            log.info("Writing transcription result for media file ID: {}", result.getMediaFileId());

            // Cache the result
            cacheTranscriptionResult(result);

            // Send completion event to Kafka
            TranscriptionCompletedEvent event = TranscriptionCompletedEvent.builder()
                    .mediaFileId(result.getMediaFileId())
                    .transcriptionResultId(result.getId())
                    .status("COMPLETED")
                    .completedAt(LocalDateTime.now())
                    .build();

            kafkaProducerService.sendTranscriptionCompletedEvent(event);

            log.info("Transcription result written and cached for media file ID: {}",
                    result.getMediaFileId());
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
