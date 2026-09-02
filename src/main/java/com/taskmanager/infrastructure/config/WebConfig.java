package com.taskmanager.infrastructure.config;

import com.taskmanager.infrastructure.http.AuthenticatedUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;

    public WebConfig(AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver) {
        this.authenticatedUserArgumentResolver = authenticatedUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://127.0.0.1:5500", "http://localhost:5500",
                        "http://localhost:5173", "http://localhost:5174"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}