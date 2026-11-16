package br.com.wxnchatbot.backend.canal.telegram;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class TelegramClientHttp implements TelegramClient {

	static final int LIMITE_DO_TEXTO = 4096;
	private static final int TAMANHO_MAXIMO_DO_ERRO = 300;
	private static final Duration FOLGA_DO_POLLING = Duration.ofSeconds(10);

	/** O corpo de {@code getUpdates}: {"ok": true, "result": [...]}. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record RespostaDeAtualizacoes(boolean ok, List<AtualizacaoDoTelegram> result, String description) {
	}

	private final RestClient cliente;
	private final String token;
	private final JsonMapper json;

	public TelegramClientHttp(TelegramProperties propriedades, JsonMapper json) {
		HttpClient http = HttpClient.newBuilder().connectTimeout(propriedades.timeoutConexao()).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
		// A leitura precisa durar mais que a espera do long polling, senão toda rodada vazia viraria erro.
		Duration minimo = Duration.ofSeconds(Math.max(0, propriedades.esperaDoPolling())).plus(FOLGA_DO_POLLING);
		fabrica.setReadTimeout(propriedades.timeoutLeitura().compareTo(minimo) < 0 ? minimo : propriedades.timeoutLeitura());
		this.cliente = RestClient.builder().baseUrl(propriedades.urlBase()).requestFactory(fabrica).build();
		this.token = propriedades.tokenDoBot();
		this.json = json;
	}

	@Override
	public void enviarTexto(long chatId, String texto) {
		for (String parte : dividir(texto)) {
			enviarParte(chatId, parte);
		}
	}

	private void enviarParte(long chatId, String parte) {
		Map<String, Object> corpo = Map.of("chat_id", chatId, "text", parte);
		try {
			cliente.post()
					.uri(caminho("sendMessage"))
					.contentType(MediaType.APPLICATION_JSON)
					.body(corpo)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException e) {
			throw new FalhaNoTelegramException("O Telegram recusou a mensagem (HTTP " + e.getStatusCode().value() + "): "
					+ resumir(e.getResponseBodyAsString()), e.getStatusCode().value());
		} catch (RestClientException e) {
			throw falhaDeRede(e);
		}
	}

	@Override
	public List<AtualizacaoDoTelegram> buscarAtualizacoes(long offset, int esperaEmSegundos) {
		String corpo;
		try {
			corpo = cliente.get()
					.uri(construtor -> construtor.path(caminho("getUpdates"))
							.queryParam("offset", offset)
							.queryParam("timeout", esperaEmSegundos)
							.queryParam("allowed_updates", "[\"message\"]")
							.build())
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException e) {
			int status = e.getStatusCode().value();
			String dica = status == 409 ? " (há um webhook configurado para este bot, ou outro processo lendo as mensagens)" : "";
			throw new FalhaNoTelegramException("O Telegram recusou a busca de mensagens (HTTP " + status + ")" + dica + ": "
					+ resumir(e.getResponseBodyAsString()), status);
		} catch (RestClientException e) {
			throw falhaDeRede(e);
		}
		try {
			RespostaDeAtualizacoes resposta = json.readValue(corpo, RespostaDeAtualizacoes.class);
			if (!resposta.ok()) {
				throw new FalhaNoTelegramException("O Telegram respondeu ok=false: " + resumir(resposta.description()), 0);
			}
			return resposta.result() == null ? List.of() : resposta.result();
		} catch (JacksonException e) {
			throw new FalhaNoTelegramException("Resposta ilegível do Telegram: " + e.getOriginalMessage(), 0);
		}
	}

	/**
	 * O endereço de um método da Bot API: {@code /bot<token>/<método>}. O token vai direto no texto, e não como variável
	 * do endereço: o Spring codificaria o ":" do token como "%3A", e a Bot API espera o token como ele é.
	 */
	private String caminho(String metodo) {
		return "/bot" + token + "/" + metodo;
	}

	/** Não repassa a exceção original: a mensagem dela repete o endereço da chamada, e nele vai o token do bot. */
	private FalhaNoTelegramException falhaDeRede(RestClientException e) {
		String mensagem = e.getMessage() == null ? "" : e.getMessage().replace(token, "***");
		return new FalhaNoTelegramException("Falha ao falar com o Telegram: " + resumir(mensagem), 0);
	}

	/** Parte o texto em pedaços que cabem numa mensagem, preferindo cortar numa quebra de linha ou num espaço. */
	static List<String> dividir(String texto) {
		List<String> partes = new ArrayList<>();
		String restante = texto == null ? "" : texto.strip();
		while (restante.length() > LIMITE_DO_TEXTO) {
			int corte = restante.lastIndexOf('\n', LIMITE_DO_TEXTO);
			if (corte < LIMITE_DO_TEXTO / 2) {
				corte = restante.lastIndexOf(' ', LIMITE_DO_TEXTO);
			}
			if (corte < LIMITE_DO_TEXTO / 2) {
				corte = LIMITE_DO_TEXTO;
			}
			partes.add(restante.substring(0, corte).strip());
			restante = restante.substring(corte).strip();
		}
		if (!restante.isEmpty()) {
			partes.add(restante);
		}
		return partes;
	}

	private static String resumir(String corpo) {
		if (corpo == null) {
			return "";
		}
		String linha = corpo.replaceAll("\\s+", " ").strip();
		return linha.length() > TAMANHO_MAXIMO_DO_ERRO ? linha.substring(0, TAMANHO_MAXIMO_DO_ERRO) + "..." : linha;
	}
}
