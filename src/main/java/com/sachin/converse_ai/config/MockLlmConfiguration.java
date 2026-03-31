package com.sachin.converse_ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MockLlmProperties.class)
public class MockLlmConfiguration {}
