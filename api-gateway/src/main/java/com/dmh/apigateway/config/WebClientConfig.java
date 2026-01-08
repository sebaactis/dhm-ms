package com.dmh.apigateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración de WebClient para hacer llamadas HTTP a otros servicios.
 * 
 * Usa @LoadBalanced para integrar con Eureka y balanceo de carga.
 */
@Configuration
public class WebClientConfig {

    /**
     * Bean de WebClient con Load Balancer.
     * 
     * Permite hacer llamadas como:
     * webClient.get().uri("http://user-service/api/users/...")
     * 
     * Eureka resuelve "user-service" a la IP/puerto correspondiente.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
