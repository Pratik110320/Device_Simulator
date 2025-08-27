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


    @Bean(name = "customRestTemplate")
    public RestTemplate restTemplate(ObjectMapper mapper) {
        // Ensure ObjectMapper is configured for Java time
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(mapper);

        RestTemplate rt = new RestTemplate();
        List<HttpMessageConverter<?>> converters = rt.getMessageConverters();

        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        converters.add(0, converter);

        return rt;
    }
}