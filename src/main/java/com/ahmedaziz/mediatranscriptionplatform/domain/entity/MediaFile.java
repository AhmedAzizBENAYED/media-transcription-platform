package com.ahmedaziz.mediatranscriptionplatform.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_files", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_uploaded_at", columnList = "uploadedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private String storageUrl;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private LocalDateTime processingStartedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private Integer retryCount;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        status = ProcessingStatus.UPLOADED;
        retryCount = 0;
    }

    public enum MediaType {
        AUDIO,
        VIDEO
    }

    public enum ProcessingStatus {
        UPLOADED,
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
