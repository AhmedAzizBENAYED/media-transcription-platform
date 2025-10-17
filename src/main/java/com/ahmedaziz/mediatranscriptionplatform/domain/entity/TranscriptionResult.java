package com.ahmedaziz.mediatranscriptionplatform.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcription_results", indexes = {
        @Index(name = "idx_media_file_id", columnList = "mediaFileId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long mediaFileId;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    @Basic(fetch = FetchType.EAGER)
    private String transcript;

    @Column
    private String language;

    @Column
    private Double confidence;

    @Column
    private Integer wordCount;

    @Column
    private Long processingTimeMs;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
        if (transcript != null) {
            wordCount = transcript.split("\\s+").length;
        }
    }
}