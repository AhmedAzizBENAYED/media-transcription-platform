package com.ahmedaziz.mediatranscriptionplatform.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResultResponse {
    private Long id;
    private Long mediaFileId;
    private String transcript;
    private String language;
    private Double confidence;
    private Integer wordCount;
    private Long processingTimeMs;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime completedAt;
}