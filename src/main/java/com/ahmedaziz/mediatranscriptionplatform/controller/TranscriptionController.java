package com.ahmedaziz.mediatranscriptionplatform.controller;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.dto.ApiResponse;
import com.ahmedaziz.mediatranscriptionplatform.dto.TranscriptionResultResponse;
import com.ahmedaziz.mediatranscriptionplatform.service.TranscriptionResultService;
import com.ahmedaziz.mediatranscriptionplatform.service.TranscriptionResultService.TranscriptionStatusResponse;
import com.ahmedaziz.mediatranscriptionplatform.service.TranscriptionResultService.TranscriptionStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
            TranscriptionResult result = transcriptionResultService
                    .getTranscriptionByMediaFileId(mediaFileId);

            TranscriptionResultResponse response = toTranscriptionResultResponse(result);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "Transcription retrieved successfully"

            ));

        } catch (IllegalArgumentException e) {
            log.warn("Transcription not found for media file ID: {}", mediaFileId);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TranscriptionResultResponse>> getTranscriptionById(
            @PathVariable Long id) {

        try {
            TranscriptionResult result = transcriptionResultService.getTranscriptionById(id);
            TranscriptionResultResponse response = toTranscriptionResultResponse(result);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "Transcription retrieved successfully"

            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/media/{mediaFileId}/status")
    public ResponseEntity<ApiResponse<TranscriptionStatusResponse>> getTranscriptionStatus(
            @PathVariable Long mediaFileId) {

        log.info("Checking transcription status for media file ID: {}", mediaFileId);

        try {
            TranscriptionStatusResponse status = transcriptionResultService
                    .getTranscriptionStatus(mediaFileId);

            return ResponseEntity.ok(ApiResponse.success(
                    status,
                    "Transcription status retrieved successfully"

            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/media/{mediaFileId}/text")
    public ResponseEntity<String> getTranscriptionText(@PathVariable Long mediaFileId) {
        try {
            TranscriptionResult result = transcriptionResultService
                    .getTranscriptionByMediaFileId(mediaFileId);

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(result.getTranscript());

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Transcription not found");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TranscriptionResultResponse>>> getAllTranscriptions() {
        List<TranscriptionResult> results = transcriptionResultService.getAllTranscriptions();

        List<TranscriptionResultResponse> responses = results.stream()
                .map(this::toTranscriptionResultResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                responses,
                "All transcriptions retrieved successfully"

        ));
    }

    @DeleteMapping("/media/{mediaFileId}")
    public ResponseEntity<ApiResponse<Void>> deleteTranscription(@PathVariable Long mediaFileId) {
        try {
            transcriptionResultService.deleteTranscription(mediaFileId);
            return ResponseEntity.ok(ApiResponse.success(
                    null,
                    "Transcription deleted successfully"

            ));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete transcription: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<TranscriptionStatistics>> getStatistics() {
        TranscriptionStatistics statistics = transcriptionResultService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(
                statistics,
                "Statistics retrieved successfully"

        ));
    }

    private TranscriptionResultResponse toTranscriptionResultResponse(TranscriptionResult result) {
        return TranscriptionResultResponse.builder()
                .id(result.getId())
                .mediaFileId(result.getMediaFileId())
                .transcript(result.getTranscript())
                .language(result.getLanguage())
                .confidence(result.getConfidence())
                .wordCount(result.getWordCount())
                .processingTimeMs(result.getProcessingTimeMs())
                .completedAt(result.getCompletedAt())
                .build();
    }
}