package br.com.wxnchatbot.backend.llm;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fala com o Gemini pela API REST (generateContent), sem SDK. A chave vai no cabeçalho x-goog-api-key, nunca na URL,
 * para não aparecer em mensagens de erro nem em logs.
 *
 * <p>O plano grátis do Gemini oscila (503 "alta demanda" e 429 por minuto são comuns), e cada falha vira o plano B da Lily
 * (encaminhar para um atendente). Por isso as falhas passageiras (429, 500, 502, 503 e 504) são tentadas de novo, até
 * duas vezes, com uma pausa curta. Erro do pedido (400, 403, 404) e resposta lenta demais não são repetidos.
 */
public class LlmClientGemini implements LlmClient {

	private static final Logger log = LoggerFactory.getLogger(LlmClientGemini.class);
	private static final int TAMANHO_MAXIMO_DO_ERRO = 300;
	private static final List<Duration> PAUSAS_PADRAO = List.of(Duration.ofMillis(700), Duration.ofMillis(1500));
	private static final Set<Integer> STATUS_PASSAGEIROS = Set.of(429, 500, 502, 503, 504);

	private final RestClient cliente;
	private final List<Duration> pausas;
	private final JsonMapper json;
	private final String chave;
	private final String modelo;
	private final String nivelDePensamento;

	public LlmClientGemini(GeminiProperties propriedades, JsonMapper json) {
		this(propriedades, json, PAUSAS_PADRAO);
	}

	/** {@code pausas}: quanto esperar antes de cada nova tentativa; o tamanho da lista é o número de tentativas extras. */
	LlmClientGemini(GeminiProperties propriedades, JsonMapper json, List<Duration> pausas) {
		this.pausas = List.copyOf(pausas);
		HttpClient http = HttpClient.newBuilder().connectTimeout(propriedades.timeoutConexao()).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
		fabrica.setReadTimeout(propriedades.timeoutLeitura());
		this.cliente = RestClient.builder().baseUrl(propriedades.urlBase()).requestFactory(fabrica).build();
		this.json = json;
		this.chave = propriedades.apiKey();
		this.modelo = propriedades.modelo();
		this.nivelDePensamento = propriedades.nivelDePensamento() == null ? "" : propriedades.nivelDePensamento().strip();
	}

	@Override
	public String responder(String promptDoSistema, List<MensagemLlm> conversa) {
		return chamar(promptDoSistema, conversa, null);
	}

	@Override
	public String responderEmJson(String promptDoSistema, List<MensagemLlm> conversa, Map<String, Object> esquema) {
		return chamar(promptDoSistema, conversa, esquema);
	}

	private String chamar(String promptDoSistema, List<MensagemLlm> conversa, Map<String, Object> esquema) {
		if (conversa.isEmpty()) {
			throw new IllegalArgumentException("conversa não pode ser vazia");
		}
		Map<String, Object> corpo = montarCorpo(promptDoSistema, conversa, esquema);
		for (int tentativa = 0;; tentativa++) {
			try {
				return extrairTexto(enviar(corpo));
			} catch (RestClientResponseException e) {
				int status = e.getStatusCode().value();
				if (tentativa < pausas.size() && STATUS_PASSAGEIROS.contains(status)) {
					log.warn("O Gemini respondeu HTTP {}; tentando de novo ({}/{})", status, tentativa + 1, pausas.size());
					esperar(pausas.get(tentativa));
					continue;
				}
				throw new LlmIndisponivelException("O Gemini respondeu HTTP " + status + ": "
						+ resumirErro(e.getResponseBodyAsString()), e);
			} catch (RestClientException e) {
				throw new LlmIndisponivelException("Falha ao falar com o Gemini: " + e.getMessage(), e);
			}
		}
	}

	private String enviar(Map<String, Object> corpo) {
		return cliente.post()
				.uri("/models/{modelo}:generateContent", modelo)
				.header("x-goog-api-key", chave)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(corpo)
				.retrieve()
				.body(String.class);
	}

	private static void esperar(Duration pausa) {
		try {
			Thread.sleep(pausa);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmIndisponivelException("Interrompido enquanto esperava para falar de novo com o Gemini", e);
		}
	}

	/**
	 * Monta o corpo do generateContent. Não manda temperature (o Gemini 3 ignora e vai recusar). Turnos seguidos do
	 * mesmo papel viram um só, e a conversa começa sempre por uma fala do cliente.
	 */
	Map<String, Object> montarCorpo(String promptDoSistema, List<MensagemLlm> conversa, Map<String, Object> esquema) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("systemInstruction", Map.of("parts", List.of(Map.of("text", promptDoSistema))));
		corpo.put("contents", turnos(conversa));
		Map<String, Object> geracao = new LinkedHashMap<>();
		if (!nivelDePensamento.isEmpty()) {
			geracao.put("thinkingConfig", Map.of("thinkingLevel", nivelDePensamento.toUpperCase(Locale.ROOT)));
		}
		if (esquema != null) {
			geracao.put("responseMimeType", "application/json");
			geracao.put("responseJsonSchema", esquema);
		}
		if (!geracao.isEmpty()) {
			corpo.put("generationConfig", geracao);
		}
		return corpo;
	}

	private static List<Map<String, Object>> turnos(List<MensagemLlm> conversa) {
		List<String> papeis = new ArrayList<>();
		List<StringBuilder> textos = new ArrayList<>();
		for (MensagemLlm mensagem : conversa) {
			String papel = mensagem.papel() == Papel.USUARIO ? "user" : "model";
			if (papeis.isEmpty() && papel.equals("model")) {
				continue;
			}
			if (!papeis.isEmpty() && papeis.get(papeis.size() - 1).equals(papel)) {
				textos.get(textos.size() - 1).append('\n').append(mensagem.conteudo());
			} else {
				papeis.add(papel);
				textos.add(new StringBuilder(mensagem.conteudo()));
			}
		}
		List<Map<String, Object>> turnos = new ArrayList<>();
		for (int i = 0; i < papeis.size(); i++) {
			turnos.add(Map.of("role", papeis.get(i), "parts", List.of(Map.of("text", textos.get(i).toString()))));
		}
		return turnos;
	}

	private String extrairTexto(String resposta) {
		try {
			JsonNode raiz = json.readTree(resposta);
			JsonNode candidato = raiz.path("candidates").path(0);
			StringBuilder texto = new StringBuilder();
			for (JsonNode parte : candidato.path("content").path("parts")) {
				if (!parte.path("thought").asBoolean(false)) {
					texto.append(parte.path("text").asString(""));
				}
			}
			if (texto.toString().isBlank()) {
				throw new LlmIndisponivelException("O Gemini não devolveu texto (bloqueio do pedido: "
						+ raiz.path("promptFeedback").path("blockReason").asString("nenhum") + ", fim da resposta: "
						+ candidato.path("finishReason").asString("desconhecido") + ")", null);
			}
			return texto.toString().strip();
		} catch (JacksonException e) {
			throw new LlmIndisponivelException("O Gemini devolveu uma resposta que não é JSON", e);
		}
	}

	private String resumirErro(String corpo) {
		if (corpo == null) {
			return "";
		}
		try {
			JsonNode erro = json.readTree(corpo).path("error");
			String descricao = erro.path("status").asString("") + " " + erro.path("message").asString("");
			return abreviar(descricao.isBlank() ? corpo : descricao);
		} catch (JacksonException e) {
			return abreviar(corpo);
		}
	}

	private static String abreviar(String texto) {
		String linha = texto.replaceAll("\\s+", " ").strip();
		return linha.length() > TAMANHO_MAXIMO_DO_ERRO ? linha.substring(0, TAMANHO_MAXIMO_DO_ERRO) + "..." : linha;
	}
}
