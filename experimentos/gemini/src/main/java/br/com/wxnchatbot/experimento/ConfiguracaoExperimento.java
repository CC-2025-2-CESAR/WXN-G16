package br.com.wxnchatbot.experimento;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "experimento")
public record ConfiguracaoExperimento(List<String> modelos, Duration pausa, boolean rapido, String pastaResultados) {
}
