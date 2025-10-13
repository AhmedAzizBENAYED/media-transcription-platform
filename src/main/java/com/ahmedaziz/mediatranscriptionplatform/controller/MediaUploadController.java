package com.ahmedaziz.mediatranscriptionplatform.controller;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.dto.ApiResponse;
import com.ahmedaziz.mediatranscriptionplatform.dto.MediaFileResponse;
import com.ahmedaziz.mediatranscriptionplatform.service.MediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MediaUploadController {

    private final MediaUploadService mediaUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MediaFileResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Received upload request for file: {}", file.getOriginalFilename());

            MediaFile mediaFile = mediaUploadService.uploadMedia(file);
            MediaFileResponse response = mapToResponse(mediaFile);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "File uploaded successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Validation error during upload", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaFileResponse>> getMediaFile(@PathVariable Long id) {
        try {
            MediaFile mediaFile = mediaUploadService.getMediaFile(id);
            MediaFileResponse response = mapToResponse(mediaFile);

            return ResponseEntity.ok(ApiResponse.success(response, "Media file retrieved successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaFileResponse>>> getAllMediaFiles() {
        List<MediaFile> mediaFiles = mediaUploadService.getAllMediaFiles();
        List<MediaFileResponse> responses = mediaFiles.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses, "Media files retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<MediaFileResponse>>> getMediaFilesByStatus(
            @PathVariable String status) {
        try {
            MediaFile.ProcessingStatus processingStatus = MediaFile.ProcessingStatus.valueOf(status.toUpperCase());
            List<MediaFile> mediaFiles = mediaUploadService.getMediaFilesByStatus(processingStatus);
            List<MediaFileResponse> responses = mediaFiles.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(responses, "Media files retrieved successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status: " + status));
        }
    }

    private MediaFileResponse mapToResponse(MediaFile mediaFile) {
        return MediaFileResponse.builder()
                .id(mediaFile.getId())
                .originalFilename(mediaFile.getOriginalFilename())
                .mediaType(mediaFile.getMediaType().name())
                .status(mediaFile.getStatus().name())
                .fileSize(mediaFile.getFileSize())
                .uploadedAt(mediaFile.getUploadedAt())
                .completedAt(mediaFile.getCompletedAt())
                .errorMessage(mediaFile.getErrorMessage())
                .build();
    }
}
