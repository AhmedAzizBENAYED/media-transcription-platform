package com.ahmedaziz.mediatranscriptionplatform.controller;

import com.ahmedaziz.mediatranscriptionplatform.dto.ApiResponse;
import com.ahmedaziz.mediatranscriptionplatform.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchJobController {

    private final BatchJobService batchJobService;

    @PostMapping("/transcription/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startTranscriptionJob() {
        try {
            log.info("Manual trigger: Starting transcription batch job");

            JobExecution jobExecution = batchJobService.startTranscriptionJob();

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobExecution.getJobId());
            response.put("status", jobExecution.getStatus().name());
            response.put("startTime", jobExecution.getStartTime());
            response.put("createTime", jobExecution.getCreateTime());

            return ResponseEntity.ok(ApiResponse.success(response,
                    "Transcription batch job started successfully"));

        } catch (JobExecutionAlreadyRunningException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Job is already running"));

        } catch (JobInstanceAlreadyCompleteException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Job instance already completed"));

        } catch (Exception e) {
            log.error("Error starting transcription job", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start job: " + e.getMessage()));
        }
    }

    @GetMapping("/transcription/status/{jobId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobStatus(@PathVariable Long jobId) {
        try {
            Map<String, Object> status = batchJobService.getJobStatus(jobId);
            return ResponseEntity.ok(ApiResponse.success(status, "Job status retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Job not found: " + e.getMessage()));
        }
    }
}
