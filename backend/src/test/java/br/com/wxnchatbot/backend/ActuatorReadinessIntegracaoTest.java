package br.com.wxnchatbot.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorReadinessIntegracaoTest {

	@Value("${local.server.port}")
	private int porta;

	private HttpResponse<String> get(String caminho) throws Exception {
		HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build();
		return HttpClient.newHttpClient().send(requisicao, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void readinessResponde200ComOBancoNoAr() throws Exception {
		HttpResponse<String> resposta = get("/actuator/health/readiness");

		assertEquals(200, resposta.statusCode());
		assertTrue(resposta.body().contains("\"UP\""));
	}

	@Test
	void readinessNaoExpoeDetalhes() throws Exception {
		HttpResponse<String> resposta = get("/actuator/health/readiness");

		assertTrue(!resposta.body().contains("PostgreSQL"));
	}

	@Test
	void outrosEndpointsDoActuatorNaoFicamExpostos() throws Exception {
		assertEquals(404, get("/actuator/env").statusCode());
		assertEquals(404, get("/actuator/beans").statusCode());
	}
}
