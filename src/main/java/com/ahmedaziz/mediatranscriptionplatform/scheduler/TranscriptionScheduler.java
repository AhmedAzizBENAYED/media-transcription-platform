package com.ahmedaziz.mediatranscriptionplatform.scheduler;

import com.ahmedaziz.mediatranscriptionplatform.batch.MediaFileReader;
import com.ahmedaziz.mediatranscriptionplatform.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscriptionScheduler {

    private final BatchJobService batchJobService;
    private final MediaFileReader mediaFileReader;

    /**
     * Run transcription batch job every 5 minutes
     * Cron: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void runScheduledTranscriptionJob() {
        log.info("Scheduled transcription job triggered");

        try {
            // Reset the reader before each scheduled run
            mediaFileReader.reset();

            batchJobService.startTranscriptionJob();
            log.info("Scheduled transcription job started successfully");

        } catch (Exception e) {
            log.error("Error running scheduled transcription job", e);
        }
    }

    /**
     * Alternative: Run on fixed delay (30 seconds after previous completion)
     */
    // @Scheduled(fixedDelay = 30000)
    public void runFixedDelayTranscriptionJob() {
        log.debug("Fixed delay transcription job check");
        // This can be enabled as an alternative to cron scheduling
    }
}
