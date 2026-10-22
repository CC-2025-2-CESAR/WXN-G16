package br.com.wxnchatbot.backend.integracao.wxn;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WxnApiProperties.class)
public class WxnApiConfig {

	@Bean
	public DemandaGateway demandaGateway(WxnApiProperties propriedades) {
		return new DemandaGatewayHttp(propriedades);
	}
}
