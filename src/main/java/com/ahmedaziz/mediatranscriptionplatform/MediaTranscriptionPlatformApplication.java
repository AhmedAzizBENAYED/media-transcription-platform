package com.ahmedaziz.mediatranscriptionplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
@EnableCaching
public class MediaTranscriptionPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaTranscriptionPlatformApplication.class, args);
    }

}
