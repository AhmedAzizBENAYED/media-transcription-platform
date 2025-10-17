package com.ahmedaziz.mediatranscriptionplatform.batch;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import com.ahmedaziz.mediatranscriptionplatform.repository.MediaFileRepository;
import com.ahmedaziz.mediatranscriptionplatform.service.TranscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscriptionProcessor implements ItemProcessor<MediaFile, TranscriptionResult> {

    private final TranscriptionService transcriptionService;
    private final MediaFileRepository mediaFileRepository;

    @Override
    public TranscriptionResult process(MediaFile mediaFile) throws Exception {
        log.info("Processing media file ID: {} - {}", mediaFile.getId(), mediaFile.getOriginalFilename());

        try {
            // Update status to PROCESSING
            mediaFile.setStatus(MediaFile.ProcessingStatus.PROCESSING);
            mediaFile.setProcessingStartedAt(LocalDateTime.now());
            mediaFileRepository.save(mediaFile);

            // Perform transcription
            TranscriptionResult result = transcriptionService.transcribe(mediaFile);

            // Update media file status
            mediaFile.setStatus(MediaFile.ProcessingStatus.COMPLETED);
            mediaFile.setCompletedAt(LocalDateTime.now());
            mediaFile.setErrorMessage(null);
            mediaFileRepository.save(mediaFile);

            log.info("Successfully processed media file ID: {}", mediaFile.getId());
            return result;

        } catch (Exception e) {
            log.error("Error processing media file ID: {}", mediaFile.getId(), e);

            // Update media file with error
            mediaFile.setStatus(MediaFile.ProcessingStatus.FAILED);
            mediaFile.setErrorMessage(e.getMessage());
            mediaFile.setCompletedAt(LocalDateTime.now());
            mediaFile.setRetryCount(mediaFile.getRetryCount() + 1);
            mediaFileRepository.save(mediaFile);

            throw e; // Re-throw to let batch framework handle retry/skip
        }
    }
}
