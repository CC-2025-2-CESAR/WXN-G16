package br.com.wxnchatbot.backend.llm;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
 */
public class LlmClientGemini implements LlmClient {

	private static final int TAMANHO_MAXIMO_DO_ERRO = 300;

	private final RestClient cliente;
	private final JsonMapper json;
	private final String chave;
	private final String modelo;
	private final String nivelDePensamento;

	public LlmClientGemini(GeminiProperties propriedades, JsonMapper json) {
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
		String resposta;
		try {
			resposta = cliente.post()
					.uri("/models/{modelo}:generateContent", modelo)
					.header("x-goog-api-key", chave)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(corpo)
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException e) {
			throw new LlmIndisponivelException("O Gemini respondeu HTTP " + e.getStatusCode().value() + ": "
					+ resumirErro(e.getResponseBodyAsString()), e);
		} catch (RestClientException e) {
			throw new LlmIndisponivelException("Falha ao falar com o Gemini: " + e.getMessage(), e);
		}
		return extrairTexto(resposta);
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
