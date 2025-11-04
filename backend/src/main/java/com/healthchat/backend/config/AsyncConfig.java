package com.healthchat.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);    // 동시에 처리할 기본 스레드 수
        executor.setMaxPoolSize(8);     // 최대 스레드 수
        executor.setQueueCapacity(50);  // 대기 큐 용량
        executor.setThreadNamePrefix("MailAsync-");
        executor.initialize();
        return executor;
    }
}
