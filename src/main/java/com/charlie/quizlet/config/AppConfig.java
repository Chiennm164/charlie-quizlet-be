package com.charlie.quizlet.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /** Nguồn thời gian dùng chung — inject {@link Clock} thay vì gọi Instant.now() để test cố định được thời gian. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
