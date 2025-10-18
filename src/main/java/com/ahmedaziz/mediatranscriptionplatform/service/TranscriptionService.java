package com.ahmedaziz.mediatranscriptionplatform.service;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.repository.TranscriptionResultRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionService {

    private final MinioStorageService minioStorageService;
    private final TranscriptionResultRepository transcriptionResultRepository;
    private final RestTemplate restTemplate;

    @Value("${app.transcription.ai-service-url}")
    private String aiServiceUrl;

    @Transactional
    public TranscriptionResult transcribe(MediaFile mediaFile) {
        log.info("Starting transcription for media file ID: {}", mediaFile.getId());

        long startTime = System.currentTimeMillis();

        try {
            // Download file from MinIO
            log.info("Downloading file from MinIO: {}", mediaFile.getStorageUrl());
            byte[] fileBytes = minioStorageService.downloadFileAsBytes(mediaFile.getStorageUrl());
            log.info("Downloaded {} bytes", fileBytes.length);

            // Call Whisper AI service
            log.info("Calling Whisper AI service at: {}", aiServiceUrl);
            WhisperResponse whisperResponse = callWhisperService(fileBytes, mediaFile.getOriginalFilename());

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("Whisper transcription completed:");
            log.info("  - Language: {}", whisperResponse.getLanguage());
            log.info("  - Confidence: {}", whisperResponse.getConfidence());
            log.info("  - Text length: {} characters", whisperResponse.getText().length());
            log.info("  - Processing time: {}ms", processingTime);

            // Save transcription result
            TranscriptionResult result = TranscriptionResult.builder()
                    .mediaFileId(mediaFile.getId())
                    .transcript(whisperResponse.getText())
                    .language(whisperResponse.getLanguage())
                    .confidence(whisperResponse.getConfidence())
                    .processingTimeMs(processingTime)
                    .completedAt(LocalDateTime.now())
                    .build();

            result = transcriptionResultRepository.save(result);

            log.info("Transcription result saved with ID: {}", result.getId());

            return result;

        } catch (Exception e) {
            log.error("Error transcribing media file ID: {}", mediaFile.getId(), e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }

    private WhisperResponse callWhisperService(byte[] fileBytes, String filename) {
        try {
            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Sending file to Whisper service: {} ({} bytes)", filename, fileBytes.length);

            // Call Whisper API
            ResponseEntity<WhisperResponse> response = restTemplate.postForEntity(
                    aiServiceUrl,
                    requestEntity,
                    WhisperResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("✓ Received transcription from Whisper service");
                return response.getBody();
            } else {
                throw new RuntimeException("Whisper service returned error status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error calling Whisper service", e);
            throw new RuntimeException("Failed to call Whisper AI service: " + e.getMessage(), e);
        }
    }

    @Data
    public static class WhisperResponse {
        private String text;
        private String language;
        private Double confidence;
        private Long processingTimeMs;
        private Integer segments;
        private String model;
        private String filename;
        private Long fileSize;
    }
}