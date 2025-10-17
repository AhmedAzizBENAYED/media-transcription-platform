package com.ahmedaziz.mediatranscriptionplatform.service;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.repository.MediaFileRepository;
import com.ahmedaziz.mediatranscriptionplatform.repository.TranscriptionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionResultService {

    private final TranscriptionResultRepository transcriptionResultRepository;
    private final MediaFileRepository mediaFileRepository;

    @Cacheable(value = "transcriptions", key = "#mediaFileId")
    @Transactional(readOnly = true)
    public TranscriptionResult getTranscriptionByMediaFileId(Long mediaFileId) {
        log.debug("Fetching transcription for media file ID: {}", mediaFileId);

        TranscriptionResult result = transcriptionResultRepository.findByMediaFileId(mediaFileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transcription not found for media file ID: " + mediaFileId));

        // Force load transcript to avoid lazy loading issues
        result.getTranscript();

        return result;
    }

    @Transactional(readOnly = true)
    public TranscriptionStatusResponse getTranscriptionStatus(Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Media file not found with ID: " + mediaFileId));

        boolean hasTranscription = transcriptionResultRepository.existsByMediaFileId(mediaFileId);

        String message = switch (mediaFile.getStatus()) {
            case UPLOADED -> "File uploaded, waiting to be processed";
            case QUEUED -> "File queued for transcription";
            case PROCESSING -> "Transcription in progress";
            case COMPLETED -> "Transcription completed successfully";
            case FAILED -> "Transcription failed: " + mediaFile.getErrorMessage();
        };

        return TranscriptionStatusResponse.builder()
                .mediaFileId(mediaFileId)
                .status(mediaFile.getStatus().name())
                .hasTranscription(hasTranscription)
                .message(message)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TranscriptionStatusResponse {
        private Long mediaFileId;
        private String status;
        private Boolean hasTranscription;
        private String message;
    }
}