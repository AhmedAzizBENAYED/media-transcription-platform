package com.ahmedaziz.mediatranscriptionplatform.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.media-uploaded}")
    private String mediaUploadedTopic;

    @Value("${kafka.topics.media-transcribed}")
    private String mediaTranscribedTopic;

    @Value("${kafka.topics.media-failed}")
    private String mediaFailedTopic;

    @Bean
    public NewTopic mediaUploadedTopic() {
        return TopicBuilder.name(mediaUploadedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic mediaTranscribedTopic() {
        return TopicBuilder.name(mediaTranscribedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic mediaFailedTopic() {
        return TopicBuilder.name(mediaFailedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
