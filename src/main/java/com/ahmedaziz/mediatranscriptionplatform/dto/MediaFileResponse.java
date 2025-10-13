package com.ahmedaziz.mediatranscriptionplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileResponse {
    private Long id;
    private String originalFilename;
    private String mediaType;
    private String status;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}
