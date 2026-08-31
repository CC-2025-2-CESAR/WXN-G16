package br.com.wxnchatbot.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera o backend para ser chamado pelo frontend (React/Vite) durante o
 * desenvolvimento local e, futuramente, pela URL de producao do frontend.
 *
 * As origens permitidas vem de "app.cors.allowed-origins" (application.properties),
 * que por sua vez le a variavel de ambiente CORS_ALLOWED_ORIGINS. Assim, em
 * deploy basta configurar essa variavel na plataforma de hospedagem sem
 * precisar alterar codigo.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
