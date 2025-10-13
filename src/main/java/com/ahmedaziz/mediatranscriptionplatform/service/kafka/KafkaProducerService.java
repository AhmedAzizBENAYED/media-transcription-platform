package com.ahmedaziz.mediatranscriptionplatform.service.kafka;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaUploadEvent;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.media-uploaded}")
    private String mediaUploadedTopic;

    @Value("${kafka.topics.media-transcribed}")
    private String mediaTranscribedTopic;

    @Value("${kafka.topics.media-failed}")
    private String mediaFailedTopic;

    public void sendMediaUploadedEvent(MediaUploadEvent event) {
        log.info("Sending media uploaded event for file ID: {}", event.getMediaFileId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                mediaUploadedTopic,
                event.getMediaFileId().toString(),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Media upload event sent successfully. Topic: {}, Partition: {}, Offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send media upload event for file ID: {}",
                        event.getMediaFileId(), ex);
            }
        });
    }

    public void sendTranscriptionCompletedEvent(TranscriptionCompletedEvent event) {
        log.info("Sending transcription completed event for file ID: {}", event.getMediaFileId());

        String topic = "FAILED".equals(event.getStatus()) ? mediaFailedTopic : mediaTranscribedTopic;

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                topic,
                event.getMediaFileId().toString(),
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transcription completed event sent successfully. Topic: {}, Status: {}",
                        result.getRecordMetadata().topic(),
                        event.getStatus());
            } else {
                log.error("Failed to send transcription completed event for file ID: {}",
                        event.getMediaFileId(), ex);
            }
        });
    }
}