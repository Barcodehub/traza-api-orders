package org.barcodev.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.barcodev.orderservice.interceptor.HeaderPropagationInterceptor;
import java.util.Collections;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(HeaderPropagationInterceptor headerPropagationInterceptor) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(headerPropagationInterceptor));
        return restTemplate;
    }
}
