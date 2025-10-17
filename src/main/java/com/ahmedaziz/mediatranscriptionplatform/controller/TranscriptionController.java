package com.ahmedaziz.mediatranscriptionplatform.controller;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.dto.ApiResponse;
import com.ahmedaziz.mediatranscriptionplatform.dto.TranscriptionResultResponse;
import com.ahmedaziz.mediatranscriptionplatform.service.TranscriptionResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/transcription")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TranscriptionController {

    private final TranscriptionResultService transcriptionResultService;

    @GetMapping("/media/{mediaFileId}")
    public ResponseEntity<ApiResponse<TranscriptionResultResponse>> getTranscription(
            @PathVariable Long mediaFileId) {

        log.info("Fetching transcription for media file ID: {}", mediaFileId);

        try {
            TranscriptionResult result = transcriptionResultService.getTranscriptionByMediaFileId(mediaFileId);

            TranscriptionResultResponse response = TranscriptionResultResponse.builder()
                    .id(result.getId())
                    .mediaFileId(result.getMediaFileId())
                    .transcript(result.getTranscript())
                    .language(result.getLanguage())
                    .confidence(result.getConfidence())
                    .wordCount(result.getWordCount())
                    .processingTimeMs(result.getProcessingTimeMs())
                    .completedAt(result.getCompletedAt())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response,
                    "Transcription retrieved successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Transcription not found for media file ID: {}", mediaFileId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/media/{mediaFileId}/status")
    public ResponseEntity<ApiResponse<TranscriptionResultService.TranscriptionStatusResponse>>
    getTranscriptionStatus(@PathVariable Long mediaFileId) {

        log.info("Checking transcription status for media file ID: {}", mediaFileId);

        try {
            TranscriptionResultService.TranscriptionStatusResponse status =
                    transcriptionResultService.getTranscriptionStatus(mediaFileId);

            return ResponseEntity.ok(ApiResponse.success(status,
                    "Transcription status retrieved successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/media/{mediaFileId}/download")
    public ResponseEntity<byte[]> downloadTranscription(@PathVariable Long mediaFileId) {
        log.info("Downloading transcription for media file ID: {}", mediaFileId);

        try {
            TranscriptionResult result = transcriptionResultService.getTranscriptionByMediaFileId(mediaFileId);

            byte[] content = result.getTranscript().getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment",
                    "transcription_" + mediaFileId + ".txt");
            headers.setContentLength(content.length);

            return new ResponseEntity<>(content, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}