package com.ahmedaziz.mediatranscriptionplatform.service;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.repository.TranscriptionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionService {

    private final MinioStorageService minioStorageService;
    private final TranscriptionResultRepository transcriptionResultRepository;

    @Transactional
    public TranscriptionResult transcribe(MediaFile mediaFile) {
        log.info("Starting transcription for media file ID: {}", mediaFile.getId());

        long startTime = System.currentTimeMillis();

        try {
            // Download file from MinIO
            byte[] fileBytes = minioStorageService.downloadFileAsBytes(mediaFile.getStorageUrl());
            log.info("Downloaded file from MinIO: {} bytes", fileBytes.length);

            // Call mock transcription (replace with real AI service later)
            String transcript = generateMockTranscription(mediaFile.getOriginalFilename(), fileBytes.length);

            long processingTime = System.currentTimeMillis() - startTime;

            // Save transcription result
            TranscriptionResult result = TranscriptionResult.builder()
                    .mediaFileId(mediaFile.getId())
                    .transcript(transcript)
                    .language("en")
                    .confidence(0.95)
                    .processingTimeMs(processingTime)
                    .completedAt(LocalDateTime.now())
                    .build();

            // Calculate word count in prePersist
            result = transcriptionResultRepository.save(result);

            log.info("Transcription completed for media file ID: {} in {}ms",
                    mediaFile.getId(), processingTime);

            return result;

        } catch (Exception e) {
            log.error("Error transcribing media file ID: {}", mediaFile.getId(), e);
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }
    }

    private String generateMockTranscription(String filename, int fileSize) {
        return String.format(
                "This is a mock transcription for file '%s' (size: %d bytes). " +
                        "The file was successfully uploaded and processed. " +
                        "In production, this would contain the actual speech-to-text transcription " +
                        "from an AI service like OpenAI Whisper or HuggingFace. " +
                        "The transcription was generated at %s. " +
                        "This mock text contains enough words to demonstrate word counting and caching functionality. " +
                        "The system supports both audio and video files in various formats including MP3, WAV, MP4, and more.",
                filename,
                fileSize,
                LocalDateTime.now()
        );
    }
}