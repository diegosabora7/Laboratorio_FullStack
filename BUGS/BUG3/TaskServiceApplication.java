package com.taskmanager.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }

    // TODO: [LABORATORIO - BUG 3] - Sin timeout configurado en RestTemplate. Si user-service no responde, el hilo se bloquea indefinidamente.
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000); 
        return new RestTemplate(factory);
    }
}
