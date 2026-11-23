package br.com.wxnchatbot.backend.canal.twilio;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Envia mensagens pela API REST do Twilio ({@code POST /2010-04-01/Accounts/{sid}/Messages.json}, formulário com To,
 * From e Body, autenticação básica com o Account SID e o Auth Token). As credenciais vão só no cabeçalho.
 */
public class TwilioClientHttp implements TwilioClient {

	static final int LIMITE_DO_TEXTO = 1600;
	private static final int TAMANHO_MAXIMO_DO_ERRO = 300;

	private final RestClient cliente;
	private final JsonMapper json;
	private final String accountSid;
	private final String authToken;
	private final String remetente;
	private final String autorizacao;

	public TwilioClientHttp(TwilioProperties propriedades, JsonMapper json) {
		HttpClient http = HttpClient.newBuilder().connectTimeout(propriedades.timeoutConexao()).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
		fabrica.setReadTimeout(propriedades.timeoutLeitura());
		this.cliente = RestClient.builder().baseUrl(propriedades.urlBase()).requestFactory(fabrica).build();
		this.json = json;
		this.accountSid = propriedades.accountSid().strip();
		this.authToken = propriedades.authToken();
		this.remetente = propriedades.remetente();
		this.autorizacao = "Basic " + Base64.getEncoder()
				.encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public void enviarTexto(String para, String texto) {
		for (String parte : dividir(texto)) {
			enviarParte(para, parte);
		}
	}

	private void enviarParte(String para, String parte) {
		MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
		formulario.add("To", para);
		formulario.add("From", remetente);
		formulario.add("Body", parte);
		try {
			cliente.post()
					.uri("/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
					.header(HttpHeaders.AUTHORIZATION, autorizacao)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.accept(MediaType.APPLICATION_JSON)
					.body(formulario)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException e) {
			int status = e.getStatusCode().value();
			int codigo = codigoDoErro(e.getResponseBodyAsString());
			throw new FalhaNoTwilioException("O Twilio recusou a mensagem (HTTP " + status
					+ (codigo > 0 ? ", código " + codigo : "") + "): " + descricaoDoErro(e.getResponseBodyAsString()), status,
					codigo);
		} catch (RestClientException e) {
			throw falhaDeRede(e);
		}
	}

	/** Os erros da API REST do Twilio trazem {"code": 21211, "message": "...", "status": 400}. */
	private int codigoDoErro(String corpo) {
		try {
			JsonNode no = json.readTree(corpo == null ? "" : corpo).path("code");
			return no.isNumber() ? no.asInt() : 0;
		} catch (JacksonException e) {
			return 0;
		}
	}

	private String descricaoDoErro(String corpo) {
		if (corpo == null) {
			return "";
		}
		try {
			JsonNode mensagem = json.readTree(corpo).path("message");
			return resumir(mensagem.isString() ? mensagem.asString("") : corpo);
		} catch (JacksonException e) {
			return resumir(corpo);
		}
	}

	/** Não repassa a exceção original: a mensagem dela repete o endereço da chamada, e nele vai o Account SID. */
	private FalhaNoTwilioException falhaDeRede(RestClientException e) {
		String mensagem = e.getMessage() == null ? "" : e.getMessage().replace(accountSid, "***").replace(authToken, "***");
		return new FalhaNoTwilioException("Falha ao falar com o Twilio: " + resumir(mensagem), 0, 0);
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
