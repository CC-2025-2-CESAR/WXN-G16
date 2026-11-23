package br.com.wxnchatbot.backend.canal.twilio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(TwilioProperties.class)
public class TwilioConfig {

	@Bean
	@ConditionalOnProperty(name = "twilio.habilitado", havingValue = "true")
	public TwilioClient twilioClient(TwilioProperties propriedades, JsonMapper json) {
		propriedades.exigirConfiguracaoCompleta();
		return new TwilioClientHttp(propriedades, json);
	}
}
