package com.drf.coupon.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {"com.drf.coupon", "com.drf.common"})
@EnableJpaRepositories(basePackages = {"com.drf.coupon", "com.drf.common"})
public class JpaConfig {
}
