package com.ahmedaziz.mediatranscriptionplatform.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler to trigger batch transcription jobs periodically
 * This provides an alternative to Kafka-driven processing
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.batch.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class TranscriptionScheduler {

    private final JobLauncher jobLauncher;
    private final Job transcriptionJob;

    /**
     * Scheduled batch job to process uploaded files
     * Runs every 5 minutes by default
     */
    @Scheduled(cron = "${app.batch.scheduler.cron:0 */5 * * * *}")
    public void runBatchTranscription() {
        log.info("======================================");
        log.info("Starting scheduled batch transcription job");
        log.info("Timestamp: {}", LocalDateTime.now());
        log.info("======================================");

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("trigger", "scheduled")
                    .toJobParameters();

            var execution = jobLauncher.run(transcriptionJob, params);

            log.info("Batch job completed with status: {}", execution.getStatus());
            log.info("Read count: {}", execution.getStepExecutions().stream()
                    .mapToLong(step -> step.getReadCount())
                    .sum());
            log.info("Write count: {}", execution.getStepExecutions().stream()
                    .mapToLong(step -> step.getWriteCount())
                    .sum());

        } catch (Exception e) {
            log.error("Error running scheduled batch job", e);
        }

        log.info("======================================");
        log.info("Scheduled batch transcription job finished");
        log.info("======================================");
    }

    /**
     * Cleanup old completed/failed records
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "${app.batch.cleanup.cron:0 0 2 * * *}")
    public void cleanupOldRecords() {
        log.info("Running cleanup job for old records");
        // Implement cleanup logic if needed
        // For example: delete transcriptions older than 30 days
    }
}