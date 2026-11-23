package br.com.wxnchatbot.backend.canal.twilio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.json.JsonMapper;

class TwilioClientHttpTest {

	private static final String SID = "AC_SID_FALSO_PARA_TESTES";
	private static final String TOKEN = "token-secreto-do-twilio";

	private final JsonMapper json = JsonMapper.builder().build();
	private final List<String> pedidos = new CopyOnWriteArrayList<>();
	private final List<String> autorizacoes = new CopyOnWriteArrayList<>();
	private final List<String> tiposDeConteudo = new CopyOnWriteArrayList<>();
	private final List<Map<String, String>> formularios = new CopyOnWriteArrayList<>();

	private HttpServer servidor;
	private volatile int statusDaResposta = 201;
	private volatile String corpoDaResposta = "{\"sid\":\"SM0123\",\"status\":\"queued\",\"error_code\":null}";
	private TwilioClientHttp cliente;

	@BeforeEach
	void subirServidor() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", this::tratar);
		servidor.start();
		cliente = novoCliente(servidor.getAddress().getPort());
	}

	private TwilioClientHttp novoCliente(int porta) {
		return new TwilioClientHttp(new TwilioProperties(true, SID, TOKEN, "+14155238886",
				"https://exemplo.com/webhook/twilio/whatsapp", "http://127.0.0.1:" + porta, Duration.ofMillis(500),
				Duration.ofMillis(500)), json);
	}

	@AfterEach
	void pararServidor() {
		servidor.stop(0);
	}

	private void tratar(HttpExchange troca) throws IOException {
		pedidos.add(troca.getRequestMethod() + " " + troca.getRequestURI());
		autorizacoes.add(String.valueOf(troca.getRequestHeaders().getFirst("Authorization")));
		tiposDeConteudo.add(String.valueOf(troca.getRequestHeaders().getFirst("Content-Type")));
		formularios.add(formulario(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
		byte[] resposta = corpoDaResposta.getBytes(StandardCharsets.UTF_8);
		troca.getResponseHeaders().add("Content-Type", "application/json");
		troca.sendResponseHeaders(statusDaResposta, resposta.length);
		troca.getResponseBody().write(resposta);
		troca.close();
	}

	private static Map<String, String> formulario(String corpo) {
		Map<String, String> campos = new LinkedHashMap<>();
		for (String par : corpo.split("&")) {
			if (par.isBlank()) {
				continue;
			}
			String[] partes = par.split("=", 2);
			campos.put(URLDecoder.decode(partes[0], StandardCharsets.UTF_8),
					partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "");
		}
		return campos;
	}

	@Test
	void enviaOFormularioComToFromEBodyEAutenticacaoBasica() {
		cliente.enviarTexto("whatsapp:+5581999990001", "Olá, tudo bem?");

		assertEquals(List.of("POST /2010-04-01/Accounts/" + SID + "/Messages.json"), pedidos);
		assertEquals("Basic " + Base64.getEncoder().encodeToString((SID + ":" + TOKEN).getBytes(StandardCharsets.UTF_8)),
				autorizacoes.get(0));
		assertTrue(tiposDeConteudo.get(0).startsWith("application/x-www-form-urlencoded"));
		assertEquals(Map.of("To", "whatsapp:+5581999990001", "From", "whatsapp:+14155238886", "Body", "Olá, tudo bem?"),
				formularios.get(0));
	}

	@Test
	void textoComCaracteresEspeciaisChegaIntacto() {
		String texto = "Linha 1\nLinha 2: R$ 10 + 5 = 15 & mais 100% ação?";

		cliente.enviarTexto("whatsapp:+5581999990001", texto);

		assertEquals(texto, formularios.get(0).get("Body"));
	}

	@Test
	void textoMaiorQue1600CaracteresVaiEmVariasMensagens() {
		cliente.enviarTexto("whatsapp:+5581999990001", "a".repeat(2000) + " " + "b".repeat(2000));

		assertTrue(formularios.size() >= 3);
		formularios.forEach(f -> assertTrue(f.get("Body").length() <= TwilioClientHttp.LIMITE_DO_TEXTO));
	}

	@Test
	void dividirPrefereCortarNumaQuebraDeLinhaOuNumEspaco() {
		String texto = "x".repeat(1000) + "\n" + "y".repeat(1000);

		assertEquals(List.of("x".repeat(1000), "y".repeat(1000)), TwilioClientHttp.dividir(texto));
		assertEquals(List.of(), TwilioClientHttp.dividir("   "));
		assertEquals(List.of(), TwilioClientHttp.dividir(null));
	}

	@Test
	void erroDoTwilioViraFalhaComStatusCodigoEMotivo() {
		statusDaResposta = 400;
		corpoDaResposta = "{\"code\":21211,\"message\":\"The 'To' number is not a valid phone number.\",\"status\":400}";

		FalhaNoTwilioException erro = assertThrows(FalhaNoTwilioException.class,
				() -> cliente.enviarTexto("whatsapp:+5581999990001", "Oi"));

		assertEquals(400, erro.status());
		assertEquals(21211, erro.codigo());
		assertTrue(erro.getMessage().contains("21211"));
		assertTrue(erro.getMessage().contains("not a valid phone number"));
	}

	@Test
	void foraDaJanelaDe24HorasChegaComOCodigo63016() {
		statusDaResposta = 400;
		corpoDaResposta = "{\"code\":63016,\"message\":\"Outside messaging window\",\"status\":400}";

		FalhaNoTwilioException erro = assertThrows(FalhaNoTwilioException.class,
				() -> cliente.enviarTexto("whatsapp:+5581999990001", "Oi"));

		assertEquals(63016, erro.codigo());
	}

	@Test
	void respostaDeErroQueNaoEJsonTambemViraFalhaSemCodigo() {
		statusDaResposta = 401;
		corpoDaResposta = "<html>Unauthorized</html>";

		FalhaNoTwilioException erro = assertThrows(FalhaNoTwilioException.class,
				() -> cliente.enviarTexto("whatsapp:+5581999990001", "Oi"));

		assertEquals(401, erro.status());
		assertEquals(0, erro.codigo());
	}

	@Test
	void falhasNuncaRevelamAsCredenciais() {
		statusDaResposta = 401;
		corpoDaResposta = "{\"code\":20003,\"message\":\"Authenticate\",\"status\":401}";
		FalhaNoTwilioException recusa = assertThrows(FalhaNoTwilioException.class,
				() -> cliente.enviarTexto("whatsapp:+5581999990001", "Oi"));

		int porta = servidor.getAddress().getPort();
		servidor.stop(0);
		FalhaNoTwilioException rede = assertThrows(FalhaNoTwilioException.class,
				() -> novoCliente(porta).enviarTexto("whatsapp:+5581999990001", "Oi"));

		for (FalhaNoTwilioException erro : List.of(recusa, rede)) {
			assertFalse(erro.getMessage().contains(TOKEN));
			assertFalse(erro.getMessage().contains(SID));
			assertEquals(null, erro.getCause());
		}
		assertEquals(0, rede.status());
	}
}
