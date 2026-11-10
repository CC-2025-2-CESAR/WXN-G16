package br.com.wxnchatbot.backend.canal.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class WhatsAppClientHttpTest {

	private static final String TOKEN = "token-secreto-da-meta";

	private final JsonMapper json = JsonMapper.builder().build();
	private final List<String> caminhos = new CopyOnWriteArrayList<>();
	private final List<String> autorizacoes = new CopyOnWriteArrayList<>();
	private final List<JsonNode> corpos = new CopyOnWriteArrayList<>();

	private HttpServer servidor;
	private volatile int statusDaResposta = 200;
	private volatile String corpoDaResposta = "{\"messages\":[{\"id\":\"wamid.OK\"}]}";
	private WhatsAppClientHttp cliente;

	@BeforeEach
	void subirServidor() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", this::tratar);
		servidor.start();
		cliente = new WhatsAppClientHttp(new WhatsAppProperties(true, "v", "s", TOKEN, "106540352242922", "v23.0",
				"http://127.0.0.1:" + servidor.getAddress().getPort(), Duration.ofMillis(500), Duration.ofMillis(500)));
	}

	@AfterEach
	void pararServidor() {
		servidor.stop(0);
	}

	private void tratar(HttpExchange troca) throws IOException {
		caminhos.add(troca.getRequestMethod() + " " + troca.getRequestURI().getPath());
		autorizacoes.add(troca.getRequestHeaders().getFirst("Authorization"));
		corpos.add(json.readTree(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
		byte[] resposta = corpoDaResposta.getBytes(StandardCharsets.UTF_8);
		troca.getResponseHeaders().add("Content-Type", "application/json");
		troca.sendResponseHeaders(statusDaResposta, resposta.length);
		troca.getResponseBody().write(resposta);
		troca.close();
	}

	@Test
	void enviaOTextoNoFormatoDaCloudApi() {
		cliente.enviarTexto("5581999990101", "Olá, tudo bem?");

		assertEquals(List.of("POST /v23.0/106540352242922/messages"), caminhos);
		assertEquals(List.of("Bearer " + TOKEN), autorizacoes);
		JsonNode corpo = corpos.get(0);
		assertEquals("whatsapp", corpo.path("messaging_product").asString());
		assertEquals("individual", corpo.path("recipient_type").asString());
		assertEquals("5581999990101", corpo.path("to").asString());
		assertEquals("text", corpo.path("type").asString());
		assertEquals("Olá, tudo bem?", corpo.path("text").path("body").asString());
		assertFalse(corpo.path("text").path("preview_url").asBoolean(true));
	}

	@Test
	void textoMuitoLongoSaiEmVariasMensagensSemPerderNada() {
		String longo = ("palavra ".repeat(1500)).strip();

		cliente.enviarTexto("5581999990101", longo);

		assertTrue(corpos.size() >= 3);
		List<String> partes = new ArrayList<>();
		corpos.forEach(c -> partes.add(c.path("text").path("body").asString()));
		partes.forEach(p -> assertTrue(p.length() <= WhatsAppClientHttp.LIMITE_DO_TEXTO));
		assertEquals(longo, String.join(" ", partes));
	}

	@Test
	void recusaDaMetaViraExcecaoComOStatusEMotivoESemOToken() {
		statusDaResposta = 401;
		corpoDaResposta = "{\"error\":{\"message\":\"Invalid OAuth access token.\",\"type\":\"OAuthException\",\"code\":190}}";

		EnvioNoWhatsAppFalhouException erro = assertThrows(EnvioNoWhatsAppFalhouException.class,
				() -> cliente.enviarTexto("5581999990101", "Oi"));

		assertTrue(erro.getMessage().contains("HTTP 401"));
		assertTrue(erro.getMessage().contains("Invalid OAuth access token"));
		assertFalse(erro.getMessage().contains(TOKEN));
	}

	@Test
	void servidorForaDoArViraExcecaoSemOToken() {
		servidor.stop(0);

		EnvioNoWhatsAppFalhouException erro = assertThrows(EnvioNoWhatsAppFalhouException.class,
				() -> cliente.enviarTexto("5581999990101", "Oi"));

		assertFalse(erro.getMessage().contains(TOKEN));
	}

	@Test
	void dividirRespeitaOLimiteEPrefereQuebrasDeLinha() {
		String bloco = "a".repeat(3000);
		List<String> partes = WhatsAppClientHttp.dividir(bloco + "\n" + bloco);

		assertEquals(List.of(bloco, bloco), partes);
		assertEquals(List.of(), WhatsAppClientHttp.dividir("   "));
		assertEquals(List.of("curto"), WhatsAppClientHttp.dividir("curto"));
		assertEquals(2, WhatsAppClientHttp.dividir("x".repeat(5000)).size());
	}
}
