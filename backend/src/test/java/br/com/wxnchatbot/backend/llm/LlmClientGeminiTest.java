package br.com.wxnchatbot.backend.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class LlmClientGeminiTest {

	private static final String CHAVE = "chave-secreta-do-gemini";

	private final JsonMapper json = JsonMapper.builder().build();
	private final List<String> caminhos = new CopyOnWriteArrayList<>();
	private final List<String> chaves = new CopyOnWriteArrayList<>();
	private final List<JsonNode> corpos = new CopyOnWriteArrayList<>();

	private HttpServer servidor;
	private volatile int statusDaResposta = 200;
	private volatile String corpoDaResposta = resposta("Olá! Como posso ajudar?");

	@BeforeEach
	void subirServidor() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", this::tratar);
		servidor.start();
	}

	@AfterEach
	void pararServidor() {
		servidor.stop(0);
	}

	private LlmClientGemini gemini(String nivelDePensamento) {
		return new LlmClientGemini(new GeminiProperties(CHAVE, "gemini-3.5-flash-lite", nivelDePensamento,
				"http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta", Duration.ofMillis(500), Duration.ofMillis(800)), json);
	}

	private void tratar(HttpExchange troca) throws IOException {
		caminhos.add(troca.getRequestMethod() + " " + troca.getRequestURI());
		chaves.add(troca.getRequestHeaders().getFirst("x-goog-api-key"));
		corpos.add(json.readTree(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
		byte[] bytes = corpoDaResposta.getBytes(StandardCharsets.UTF_8);
		troca.getResponseHeaders().add("Content-Type", "application/json");
		troca.sendResponseHeaders(statusDaResposta, bytes.length);
		troca.getResponseBody().write(bytes);
		troca.close();
	}

	private static String resposta(String texto) {
		return "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"" + texto
				+ "\"}]},\"finishReason\":\"STOP\"}],\"usageMetadata\":{\"promptTokenCount\":10}}";
	}

	private static MensagemLlm cliente(String texto) {
		return new MensagemLlm(Papel.USUARIO, texto);
	}

	private static MensagemLlm lily(String texto) {
		return new MensagemLlm(Papel.ASSISTENTE, texto);
	}

	@Test
	void chamaOGenerateContentComAChaveNoCabecalhoENaoNaUrl() {
		String resposta = gemini("low").responder("Prompt da Lily", List.of(cliente("Oi")));

		assertEquals("Olá! Como posso ajudar?", resposta);
		assertEquals(List.of("POST /v1beta/models/gemini-3.5-flash-lite:generateContent"), caminhos);
		assertEquals(List.of(CHAVE), chaves);
		assertFalse(caminhos.get(0).contains(CHAVE));
	}

	@Test
	void corpoTemSystemInstructionTurnosNormalizadosENivelDePensamentoEmMaiusculas() {
		gemini("low").responder("Prompt da Lily", List.of(lily("Oi, sou a Lily"), cliente("Primeira"), cliente("Segunda"),
				lily("Resposta"), cliente("Terceira")));

		JsonNode corpo = corpos.get(0);
		assertEquals("Prompt da Lily", corpo.path("systemInstruction").path("parts").path(0).path("text").asString());
		JsonNode contents = corpo.path("contents");
		assertEquals(3, contents.size());
		assertEquals("user", contents.path(0).path("role").asString());
		assertEquals("Primeira\nSegunda", contents.path(0).path("parts").path(0).path("text").asString());
		assertEquals("model", contents.path(1).path("role").asString());
		assertEquals("user", contents.path(2).path("role").asString());
		assertEquals("Terceira", contents.path(2).path("parts").path(0).path("text").asString());
		JsonNode geracao = corpo.path("generationConfig");
		assertEquals("LOW", geracao.path("thinkingConfig").path("thinkingLevel").asString());
		assertTrue(geracao.path("temperature").isMissingNode());
		assertTrue(geracao.path("responseMimeType").isMissingNode());
	}

	@Test
	void semNivelDePensamentoNaoMandaGenerationConfig() {
		gemini("  ").responder("Prompt", List.of(cliente("Oi")));

		assertTrue(corpos.get(0).path("generationConfig").isMissingNode());
	}

	@Test
	void respostaEmJsonPedeOMimeTypeEOEsquema() {
		corpoDaResposta = resposta("{\\\"intencao\\\":\\\"DUVIDA\\\",\\\"confianca\\\":0.9}");
		Map<String, Object> esquema = Map.of("type", "object", "properties", Map.of("intencao", Map.of("type", "string")));

		String texto = gemini("low").responderEmJson("Classifique", List.of(cliente("Oi")), esquema);

		assertEquals("{\"intencao\":\"DUVIDA\",\"confianca\":0.9}", texto);
		JsonNode geracao = corpos.get(0).path("generationConfig");
		assertEquals("application/json", geracao.path("responseMimeType").asString());
		assertEquals("object", geracao.path("responseJsonSchema").path("type").asString());
	}

	@Test
	void partesDePensamentoNaoEntramNaResposta() {
		corpoDaResposta = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"raciocinando\",\"thought\":true},"
				+ "{\"text\":\"Resposta \"},{\"text\":\"final\"}]}}]}";

		assertEquals("Resposta final", gemini("low").responder("Prompt", List.of(cliente("Oi"))));
	}

	@Test
	void limiteDeUsoViraLlmIndisponivelComStatusEMotivoESemAChave() {
		statusDaResposta = 429;
		corpoDaResposta = "{\"error\":{\"code\":429,\"message\":\"Quota exceeded for requests per minute\",\"status\":\"RESOURCE_EXHAUSTED\"}}";

		LlmIndisponivelException erro = assertThrows(LlmIndisponivelException.class,
				() -> gemini("low").responder("Prompt", List.of(cliente("Oi"))));

		assertTrue(erro.getMessage().contains("HTTP 429"));
		assertTrue(erro.getMessage().contains("RESOURCE_EXHAUSTED"));
		assertTrue(erro.getMessage().contains("Quota exceeded"));
		assertFalse(erro.getMessage().contains(CHAVE));
	}

	@Test
	void erroComCorpoQueNaoEJsonTambemViraLlmIndisponivel() {
		statusDaResposta = 503;
		corpoDaResposta = "<html>Service Unavailable</html>";

		LlmIndisponivelException erro = assertThrows(LlmIndisponivelException.class,
				() -> gemini("low").responder("Prompt", List.of(cliente("Oi"))));

		assertTrue(erro.getMessage().contains("HTTP 503"));
	}

	@Test
	void pedidoBloqueadoSemCandidatosViraLlmIndisponivel() {
		corpoDaResposta = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";

		LlmIndisponivelException erro = assertThrows(LlmIndisponivelException.class,
				() -> gemini("low").responder("Prompt", List.of(cliente("Oi"))));

		assertTrue(erro.getMessage().contains("SAFETY"));
	}

	@Test
	void respostaQueNaoEJsonViraLlmIndisponivel() {
		corpoDaResposta = "isto não é json";

		assertThrows(LlmIndisponivelException.class, () -> gemini("low").responder("Prompt", List.of(cliente("Oi"))));
	}

	@Test
	void servidorForaDoArViraLlmIndisponivelSemAChave() {
		LlmClientGemini semServidor = gemini("low");
		servidor.stop(0);

		LlmIndisponivelException erro = assertThrows(LlmIndisponivelException.class,
				() -> semServidor.responder("Prompt", List.of(cliente("Oi"))));

		assertFalse(erro.getMessage().contains(CHAVE));
	}

	@Test
	void respostaLentaDemaisViraLlmIndisponivel() {
		servidor.removeContext("/");
		servidor.createContext("/", troca -> {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			troca.sendResponseHeaders(200, -1);
			troca.close();
		});

		assertThrows(LlmIndisponivelException.class, () -> gemini("low").responder("Prompt", List.of(cliente("Oi"))));
	}

	@Test
	void conversaVaziaNaoChamaOServidor() {
		assertThrows(IllegalArgumentException.class, () -> gemini("low").responder("Prompt", List.of()));
		assertTrue(caminhos.isEmpty());
	}

	@Test
	void propriedadesExigemAChaveQuandoOGeminiEstaLigado() {
		GeminiProperties semChave = new GeminiProperties(" ", "m", "low", "http://x", Duration.ofSeconds(1), Duration.ofSeconds(1));

		assertTrue(assertThrows(IllegalStateException.class, semChave::exigirChave).getMessage().contains("GEMINI_API_KEY"));
	}
}
