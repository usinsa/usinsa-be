package com.usinsa.backend.domain.search.elastic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@Profile("dev")
@EnableElasticsearchRepositories(basePackages = "com.usinsa.backend.domain.search.elastic.repository")
public class ElasticsearchConfig {
}