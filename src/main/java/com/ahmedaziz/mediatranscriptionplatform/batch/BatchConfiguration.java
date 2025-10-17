package com.ahmedaziz.mediatranscriptionplatform.batch;

import com.ahmedaziz.mediatranscriptionplatform.domain.entity.MediaFile;
import com.ahmedaziz.mediatranscriptionplatform.domain.entity.TranscriptionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class BatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job transcriptionJob(Step transcriptionStep) {
        return new JobBuilder("transcriptionJob", jobRepository)
                .start(transcriptionStep)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        log.info("Starting transcription batch job: {}", jobExecution.getJobId());
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        log.info("Completed transcription batch job: {} with status: {}",
                                jobExecution.getJobId(), jobExecution.getStatus());
                    }
                })
                .build();
    }

    @Bean
    public Step transcriptionStep(
            ItemReader<MediaFile> mediaFileReader,
            ItemProcessor<MediaFile, TranscriptionResult> transcriptionProcessor,
            ItemWriter<TranscriptionResult> transcriptionWriter) {

        return new StepBuilder("transcriptionStep", jobRepository)
                .<MediaFile, TranscriptionResult>chunk(5, transactionManager)
                .reader(mediaFileReader)
                .processor(transcriptionProcessor)
                .writer(transcriptionWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(10)
                .skip(Exception.class)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("Starting transcription step");
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.info("Transcription step completed. Read: {}, Written: {}, Skipped: {}",
                                stepExecution.getReadCount(),
                                stepExecution.getWriteCount(),
                                stepExecution.getSkipCount());
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }
}
