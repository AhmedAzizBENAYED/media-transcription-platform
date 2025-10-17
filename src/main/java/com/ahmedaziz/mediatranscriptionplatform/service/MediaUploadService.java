package com.ahmedaziz.mediatranscriptionplatform.service;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaUploadEvent;
import com.ahmedaziz.mediatranscriptionplatform.repository.MediaFileRepository;
import com.ahmedaziz.mediatranscriptionplatform.service.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaUploadService {

    private final MediaFileRepository mediaFileRepository;
    private final MinioStorageService minioStorageService;
    private final KafkaProducerService kafkaProducerService;

    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList(
            "audio/mpeg", "audio/wav", "audio/mp3", "audio/mp4", "audio/ogg",
            "audio/webm", "audio/x-wav", "audio/x-m4a"
    );

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/mpeg", "video/quicktime", "video/webm",
            "video/x-msvideo", "video/x-matroska", "video/avi"
    );

    private static final List<String> ALLOWED_AUDIO_EXTENSIONS = Arrays.asList(
            ".mp3", ".wav", ".m4a", ".ogg", ".webm", ".aac", ".flac"
    );

    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
            ".mp4", ".avi", ".mov", ".webm", ".mkv", ".mpeg", ".mpg"
    );

    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB

    @Transactional
    public MediaFile uploadMedia(MultipartFile file) throws IOException {
        log.info("Starting media upload process for file: {}", file.getOriginalFilename());

        // Validation
        validateFile(file);

        // Determine media type
        MediaFile.MediaType mediaType = determineMediaType(file.getContentType(), file.getOriginalFilename());
        // Upload to MinIO
        String storageUrl = minioStorageService.uploadFile(file);
        log.info("File uploaded to MinIO: {}", storageUrl);

        // Save metadata to database
        MediaFile mediaFile = MediaFile.builder()
                .filename(storageUrl)
                .originalFilename(file.getOriginalFilename())
                .mediaType(mediaType)
                .storageUrl(storageUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .status(MediaFile.ProcessingStatus.UPLOADED)
                .retryCount(0)
                .build();

        mediaFile = mediaFileRepository.save(mediaFile);
        log.info("Media file metadata saved with ID: {}", mediaFile.getId());

        // Send Kafka event
        MediaUploadEvent event = MediaUploadEvent.builder()
                .mediaFileId(mediaFile.getId())
                .filename(storageUrl)
                .storageUrl(storageUrl)
                .mediaType(mediaType.name())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build();

        kafkaProducerService.sendMediaUploadedEvent(event);
        log.info("Media upload event sent for file ID: {}", mediaFile.getId());

        return mediaFile;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 500MB");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        boolean validFile = false;

        // Check by content type
        if (contentType != null &&
                (ALLOWED_AUDIO_TYPES.contains(contentType) || ALLOWED_VIDEO_TYPES.contains(contentType))) {
            validFile = true;
        }

        // Check by extension if content-type is generic
        if (!validFile && filename != null) {
            String lowerFilename = filename.toLowerCase();
            for (String ext : ALLOWED_AUDIO_EXTENSIONS) {
                if (lowerFilename.endsWith(ext)) {
                    validFile = true;
                    break;
                }
            }
            if (!validFile) {
                for (String ext : ALLOWED_VIDEO_EXTENSIONS) {
                    if (lowerFilename.endsWith(ext)) {
                        validFile = true;
                        break;
                    }
                }
            }
        }

        if (!validFile) {
            throw new IllegalArgumentException(
                    String.format("Unsupported file type: %s. Supported: audio (mp3, wav, m4a) and video (mp4, avi, mov)",
                            contentType)
            );
        }
    }

    private MediaFile.MediaType determineMediaType(String contentType, String filename) {
        // Check content type first
        if (contentType != null) {
            if (ALLOWED_AUDIO_TYPES.contains(contentType)) {
                return MediaFile.MediaType.AUDIO;
            } else if (ALLOWED_VIDEO_TYPES.contains(contentType)) {
                return MediaFile.MediaType.VIDEO;
            }
        }

        // Fallback to extension
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            for (String ext : ALLOWED_AUDIO_EXTENSIONS) {
                if (lowerFilename.endsWith(ext)) {
                    return MediaFile.MediaType.AUDIO;
                }
            }
            for (String ext : ALLOWED_VIDEO_EXTENSIONS) {
                if (lowerFilename.endsWith(ext)) {
                    return MediaFile.MediaType.VIDEO;
                }
            }
        }

        throw new IllegalArgumentException("Cannot determine media type");
    }

    public MediaFile getMediaFile(Long id) {
        return mediaFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media file not found with ID: " + id));
    }

    public List<MediaFile> getAllMediaFiles() {
        return mediaFileRepository.findAll();
    }

    public List<MediaFile> getMediaFilesByStatus(MediaFile.ProcessingStatus status) {
        return mediaFileRepository.findByStatus(status);
    }
}

