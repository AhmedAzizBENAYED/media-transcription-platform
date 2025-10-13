package com.ahmedaziz.mediatranscriptionplatform.domain.entity;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadEvent {
    private Long mediaFileId;
    private String filename;
    private String storageUrl;
    private String mediaType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}

