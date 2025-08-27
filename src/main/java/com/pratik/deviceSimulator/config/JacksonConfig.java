package com.pratik.deviceSimulator.config; // use your package

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 strings
        return om;
    }

    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        RestTemplate rt = new RestTemplate();
        MappingJackson2HttpMessageConverter jacksonConv = new MappingJackson2HttpMessageConverter(objectMapper);

        List converters = new ArrayList<>(rt.getMessageConverters());
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        converters.add(jacksonConv);
        rt.setMessageConverters(converters);
        return rt;
    }
}
