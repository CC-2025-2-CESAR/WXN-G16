package br.com.wxnchatbot.backend.canal.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class TelegramClientHttpTest {

	private static final String TOKEN = "123456:TOKEN-SECRETO-DO-BOT";

	private final JsonMapper json = JsonMapper.builder().build();
	private final List<String> pedidos = new CopyOnWriteArrayList<>();
	private final List<JsonNode> corpos = new CopyOnWriteArrayList<>();

	private HttpServer servidor;
	private volatile int statusDaResposta = 200;
	private volatile String corpoDaResposta = "{\"ok\":true,\"result\":{}}";
	private TelegramClientHttp cliente;

	@BeforeEach
	void subirServidor() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", this::tratar);
		servidor.start();
		cliente = novoCliente(servidor.getAddress().getPort());
	}

	private TelegramClientHttp novoCliente(int porta) {
		return new TelegramClientHttp(new TelegramProperties(true, "polling", TOKEN, "", "http://127.0.0.1:" + porta,
				Duration.ofMillis(500), Duration.ofMillis(500), 1), json);
	}

	@AfterEach
	void pararServidor() {
		servidor.stop(0);
	}

	private void tratar(HttpExchange troca) throws IOException {
		pedidos.add(troca.getRequestMethod() + " " + troca.getRequestURI());
		String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		corpos.add(corpo.isBlank() ? json.createObjectNode() : json.readTree(corpo));
		byte[] resposta = corpoDaResposta.getBytes(StandardCharsets.UTF_8);
		troca.getResponseHeaders().add("Content-Type", "application/json");
		troca.sendResponseHeaders(statusDaResposta, resposta.length);
		troca.getResponseBody().write(resposta);
		troca.close();
	}

	@Test
	void enviaOTextoNoFormatoDaBotApi() {
		cliente.enviarTexto(1111111L, "Olá, tudo bem?");

		assertEquals(List.of("POST /bot" + TOKEN + "/sendMessage"), pedidos);
		assertEquals(1111111L, corpos.get(0).get("chat_id").asLong());
		assertEquals("Olá, tudo bem?", corpos.get(0).get("text").asString());
	}

	@Test
	void textoMaiorQue4096CaracteresVaiEmVariasMensagens() {
		cliente.enviarTexto(1L, "a".repeat(5000) + " " + "b".repeat(5000));

		assertTrue(corpos.size() >= 3);
		corpos.forEach(corpo -> assertTrue(corpo.get("text").asString().length() <= TelegramClientHttp.LIMITE_DO_TEXTO));
	}

	@Test
	void dividirPrefereCortarNumaQuebraDeLinhaOuNumEspaco() {
		String texto = "x".repeat(3000) + "\n" + "y".repeat(3000);

		List<String> partes = TelegramClientHttp.dividir(texto);

		assertEquals(List.of("x".repeat(3000), "y".repeat(3000)), partes);
		assertEquals(List.of(), TelegramClientHttp.dividir("   "));
		assertEquals(List.of(), TelegramClientHttp.dividir(null));
	}

	@Test
	void recusaDoTelegramViraFalhaComOStatusESemOToken() {
		statusDaResposta = 403;
		corpoDaResposta = "{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}";

		FalhaNoTelegramException erro = assertThrows(FalhaNoTelegramException.class, () -> cliente.enviarTexto(1L, "oi"));

		assertEquals(403, erro.status());
		assertTrue(erro.getMessage().contains("bot was blocked by the user"));
		assertFalse(erro.getMessage().contains(TOKEN));
	}

	@Test
	void falhaDeRedeNaoVazaOTokenNemNaMensagemNemNaCausa() {
		int porta = servidor.getAddress().getPort();
		servidor.stop(0);

		FalhaNoTelegramException erro = assertThrows(FalhaNoTelegramException.class, () -> novoCliente(porta).enviarTexto(1L, "oi"));

		assertEquals(0, erro.status());
		assertFalse(erro.getMessage().contains(TOKEN));
		assertEquals(null, erro.getCause());
	}

	@Test
	void buscarAtualizacoesPedeOOffsetEAEsperaELeAsMensagens() {
		corpoDaResposta = "{\"ok\":true,\"result\":[{\"update_id\":41,\"message\":{\"message_id\":1,\"chat\":{\"id\":9,\"type\":\"private\"},\"text\":\"oi\"}},"
				+ "{\"update_id\":42,\"message\":{\"message_id\":2,\"chat\":{\"id\":9,\"type\":\"private\"},\"text\":\"tudo bem?\"}}]}";

		List<AtualizacaoDoTelegram> atualizacoes = cliente.buscarAtualizacoes(41, 25);

		assertEquals(2, atualizacoes.size());
		assertEquals(42, atualizacoes.get(1).updateId());
		assertEquals("tudo bem?", atualizacoes.get(1).message().text());
		String pedido = pedidos.get(0);
		assertTrue(pedido.startsWith("GET /bot" + TOKEN + "/getUpdates?"), pedido);
		assertTrue(pedido.contains("offset=41") && pedido.contains("timeout=25"), pedido);
		assertTrue(pedido.contains("allowed_updates="), pedido);
	}

	@Test
	void listaVaziaQuandoNaoHaMensagensNovas() {
		corpoDaResposta = "{\"ok\":true,\"result\":[]}";

		assertEquals(List.of(), cliente.buscarAtualizacoes(0, 1));
	}

	@Test
	void conflitoComWebhookGanhaUmaDicaNoErro() {
		statusDaResposta = 409;
		corpoDaResposta = "{\"ok\":false,\"error_code\":409,\"description\":\"Conflict: can't use getUpdates method while webhook is active\"}";

		FalhaNoTelegramException erro = assertThrows(FalhaNoTelegramException.class, () -> cliente.buscarAtualizacoes(0, 1));

		assertEquals(409, erro.status());
		assertTrue(erro.getMessage().contains("webhook"));
		assertFalse(erro.getMessage().contains(TOKEN));
	}

	@Test
	void okFalsoNaRespostaViraFalha() {
		corpoDaResposta = "{\"ok\":false,\"description\":\"Unauthorized\"}";

		FalhaNoTelegramException erro = assertThrows(FalhaNoTelegramException.class, () -> cliente.buscarAtualizacoes(0, 1));

		assertTrue(erro.getMessage().contains("Unauthorized"));
	}

	@Test
	void respostaQueNaoEJsonViraFalha() {
		corpoDaResposta = "isso nao e json";

		assertThrows(FalhaNoTelegramException.class, () -> cliente.buscarAtualizacoes(0, 1));
	}
}
