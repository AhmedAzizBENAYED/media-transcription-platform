package com.ahmedaziz.mediatranscriptionplatform.domain.entity;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptionCompletedEvent {
    private Long mediaFileId;
    private Long transcriptionResultId;
    private String status;
    private LocalDateTime completedAt;
    private String errorMessage;
}