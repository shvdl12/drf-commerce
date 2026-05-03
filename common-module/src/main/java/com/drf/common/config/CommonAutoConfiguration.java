package com.drf.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan("com.drf.common")
@EnableScheduling
public class CommonAutoConfiguration {
}
