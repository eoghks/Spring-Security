package com.example.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄 작업(연체 표시) 활성화.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
