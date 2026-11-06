package br.com.wxnchatbot.backend.canal.whatsapp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {

	@Bean
	@ConditionalOnProperty(name = "whatsapp.habilitado", havingValue = "true")
	public WhatsAppClient whatsAppClient(WhatsAppProperties propriedades) {
		propriedades.exigirConfiguracaoCompleta();
		return new WhatsAppClientHttp(propriedades);
	}
}
