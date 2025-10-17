package com.ahmedaziz.mediatranscriptionplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchJobService {

    private final JobLauncher jobLauncher;
    private final Job transcriptionJob;

    public JobExecution startTranscriptionJob()
            throws JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException,
            JobParametersInvalidException {

        log.info("Starting transcription batch job");

        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDateTime("startTime", LocalDateTime.now())
                .toJobParameters();

        return jobLauncher.run(transcriptionJob, jobParameters);
    }

    public Map<String, Object> getJobStatus(Long jobId) {
        // Implementation would query JobRepository for job status
        // This is a simplified version
        Map<String, Object> status = new HashMap<>();
        status.put("jobId", jobId);
        status.put("message", "Job status retrieval not fully implemented");
        return status;
    }
}
