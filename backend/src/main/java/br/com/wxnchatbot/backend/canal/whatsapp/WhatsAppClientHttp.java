package br.com.wxnchatbot.backend.canal.whatsapp;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class WhatsAppClientHttp implements WhatsAppClient {

	static final int LIMITE_DO_TEXTO = 4096;
	private static final int TAMANHO_MAXIMO_DO_ERRO = 300;

	private final RestClient cliente;
	private final String versaoDaApi;
	private final String idDoNumero;
	private final String tokenDeAcesso;

	public WhatsAppClientHttp(WhatsAppProperties propriedades) {
		HttpClient http = HttpClient.newBuilder().connectTimeout(propriedades.timeoutConexao()).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
		fabrica.setReadTimeout(propriedades.timeoutLeitura());
		this.cliente = RestClient.builder().baseUrl(propriedades.urlBase()).requestFactory(fabrica).build();
		this.versaoDaApi = propriedades.versaoDaApi();
		this.idDoNumero = propriedades.idDoNumero();
		this.tokenDeAcesso = propriedades.tokenDeAcesso();
	}

	@Override
	public void enviarTexto(String para, String texto) {
		for (String parte : dividir(texto)) {
			enviarParte(para, parte);
		}
	}

	private void enviarParte(String para, String parte) {
		Map<String, Object> corpo = Map.of(
				"messaging_product", "whatsapp",
				"recipient_type", "individual",
				"to", para,
				"type", "text",
				"text", Map.of("preview_url", false, "body", parte));
		try {
			cliente.post()
					.uri("/{versao}/{numero}/messages", versaoDaApi, idDoNumero)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeAcesso)
					.contentType(MediaType.APPLICATION_JSON)
					.body(corpo)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException e) {
			throw new EnvioNoWhatsAppFalhouException("A Meta recusou a mensagem (HTTP " + e.getStatusCode().value() + "): "
					+ resumir(e.getResponseBodyAsString()), e);
		} catch (RestClientException e) {
			throw new EnvioNoWhatsAppFalhouException("Falha ao falar com a Meta: " + e.getMessage(), e);
		}
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
