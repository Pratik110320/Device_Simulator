package com.pratik.deviceSimulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    private final MappingJackson2HttpMessageConverter jacksonConverter;

    public RestTemplateConfig(MappingJackson2HttpMessageConverter jacksonConverter) {
        this.jacksonConverter = jacksonConverter;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();

        // Remove default Jackson converter
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);

        // Add custom one with JavaTimeModule
        converters.add(jacksonConverter);

        return restTemplate;
    }
}

