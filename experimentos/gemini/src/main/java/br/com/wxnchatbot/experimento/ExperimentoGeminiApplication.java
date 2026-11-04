package br.com.wxnchatbot.experimento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExperimentoGeminiApplication {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(ExperimentoGeminiApplication.class, args)));
	}
}
