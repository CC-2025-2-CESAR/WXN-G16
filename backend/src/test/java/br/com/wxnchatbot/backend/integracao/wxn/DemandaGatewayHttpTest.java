package br.com.wxnchatbot.backend.integracao.wxn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class DemandaGatewayHttpTest {

	private static final String DEMANDA_JSON = """
			{"protocolo":"WXN-2026-0001","titulo":"Ajuste no relatório","status":"EM_ANDAMENTO",
			"atualizadaEm":"2026-10-20T14:30:00Z","observacao":"Em desenvolvimento"}""";

	private final AtomicReference<String> empresaRecebida = new AtomicReference<>();
	private final AtomicReference<String> chaveRecebida = new AtomicReference<>();
	private final AtomicReference<String> corpoRecebido = new AtomicReference<>();

	private HttpServer servidor;
	private DemandaGatewayHttp gateway;
	private final UUID empresaId = UUID.randomUUID();

	@BeforeEach
	void subirServidor() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/demandas/", this::tratarConsulta);
		servidor.createContext("/chamados", this::tratarAbertura);
		servidor.start();
		gateway = new DemandaGatewayHttp(new WxnApiProperties("http://127.0.0.1:" + servidor.getAddress().getPort(),
				Duration.ofMillis(500), Duration.ofMillis(300)));
	}

	@AfterEach
	void pararServidor() {
		servidor.stop(0);
	}

	private void tratarConsulta(HttpExchange troca) throws IOException {
		empresaRecebida.set(troca.getRequestHeaders().getFirst(CabecalhosWxn.EMPRESA_ID));
		String protocolo = troca.getRequestURI().getPath().substring("/demandas/".length());
		switch (protocolo) {
			case "WXN-2026-0001" -> responder(troca, 200, DEMANDA_JSON);
			case "INEXISTENTE" -> responder(troca, 404, "{\"codigo\":\"DEMANDA_NAO_ENCONTRADA\",\"mensagem\":\"nao\"}");
			case "OUTRO-404" -> responder(troca, 404, "{\"codigo\":\"OUTRA_COISA\",\"mensagem\":\"nao\"}");
			case "PAGINA-404" -> responder(troca, 404, "<html>Not Found</html>");
			case "ERRO" -> responder(troca, 500, "{\"codigo\":\"ERRO_INTERNO\",\"mensagem\":\"falhou\"}");
			case "VAZIO" -> responder(troca, 200, "");
			case "LENTO" -> {
				dormir(1000);
				responder(troca, 200, DEMANDA_JSON);
			}
			default -> responder(troca, 404, "");
		}
	}

	private void tratarAbertura(HttpExchange troca) throws IOException {
		empresaRecebida.set(troca.getRequestHeaders().getFirst(CabecalhosWxn.EMPRESA_ID));
		chaveRecebida.set(troca.getRequestHeaders().getFirst(CabecalhosWxn.CHAVE_IDEMPOTENCIA));
		corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		responder(troca, 201, """
				{"protocolo":"WXN-2026-5000","titulo":"Erro ao emitir nota","status":"ABERTA",
				"criadoEm":"2026-10-22T12:00:00Z"}""");
	}

	private static void responder(HttpExchange troca, int status, String corpo) throws IOException {
		byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
		troca.getResponseHeaders().add("Content-Type", "application/json");
		troca.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
		if (bytes.length > 0) {
			troca.getResponseBody().write(bytes);
		}
		troca.close();
	}

	private static void dormir(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Test
	void demandaEncontradaVoltaComTodosOsCamposEEnviaAEmpresaNoCabecalho() {
		Optional<Demanda> demanda = gateway.consultarPorProtocolo(empresaId, "WXN-2026-0001");

		assertTrue(demanda.isPresent());
		assertEquals("WXN-2026-0001", demanda.get().protocolo());
		assertEquals(StatusDemanda.EM_ANDAMENTO, demanda.get().status());
		assertEquals("Em desenvolvimento", demanda.get().observacao());
		assertEquals(empresaId.toString(), empresaRecebida.get());
	}

	@Test
	void naoEncontradaSoQuandoAApiDizEspecificamenteIsso() {
		assertTrue(gateway.consultarPorProtocolo(empresaId, "INEXISTENTE").isEmpty());
	}

	@Test
	void umQuatrocentosEQuatroQualquerEIndisponibilidadeENaoNaoEncontrada() {
		assertThrows(DemandaIndisponivelException.class, () -> gateway.consultarPorProtocolo(empresaId, "OUTRO-404"));
		assertThrows(DemandaIndisponivelException.class, () -> gateway.consultarPorProtocolo(empresaId, "PAGINA-404"));
		assertThrows(DemandaIndisponivelException.class, () -> gateway.consultarPorProtocolo(empresaId, "SEM-ROTA"));
	}

	@Test
	void erroInternoDaApiViraIndisponibilidade() {
		assertThrows(DemandaIndisponivelException.class, () -> gateway.consultarPorProtocolo(empresaId, "ERRO"));
	}

	@Test
	void respostaSemCorpoViraIndisponibilidade() {
		assertThrows(DemandaIndisponivelException.class, () -> gateway.consultarPorProtocolo(empresaId, "VAZIO"));
	}

	@Test
	void timeoutDeLeituraViraIndisponibilidadeSemEsperarAResposta() {
		long inicio = System.nanoTime();

		assertThrows(DemandaIndisponivelException.class, () -> gateway.consultarPorProtocolo(empresaId, "LENTO"));

		long decorridoMs = (System.nanoTime() - inicio) / 1_000_000;
		assertTrue(decorridoMs < 900, "deveria desistir no timeout de 300 ms, levou " + decorridoMs + " ms");
	}

	@Test
	void servidorForaDoArViraIndisponibilidade() throws IOException {
		int portaFechada;
		try (ServerSocket socket = new ServerSocket(0)) {
			portaFechada = socket.getLocalPort();
		}
		DemandaGatewayHttp semServidor = new DemandaGatewayHttp(new WxnApiProperties(
				"http://127.0.0.1:" + portaFechada, Duration.ofMillis(300), Duration.ofMillis(300)));

		assertThrows(DemandaIndisponivelException.class, () -> semServidor.consultarPorProtocolo(empresaId, "WXN-2026-0001"));
	}

	@Test
	void aberturaDeChamadoEnviaEmpresaChaveDeIdempotenciaECorpo() {
		UUID chave = UUID.randomUUID();

		Chamado chamado = gateway.abrirChamado(empresaId, new NovoChamado("Erro ao emitir nota", "Trava na assinatura"),
				chave);

		assertEquals("WXN-2026-5000", chamado.protocolo());
		assertEquals(StatusDemanda.ABERTA, chamado.status());
		assertEquals(empresaId.toString(), empresaRecebida.get());
		assertEquals(chave.toString(), chaveRecebida.get());
		assertTrue(corpoRecebido.get().contains("\"titulo\":\"Erro ao emitir nota\""), corpoRecebido.get());
	}

	@Test
	void falhaNaAberturaViraIndisponibilidade() throws IOException {
		int portaFechada;
		try (ServerSocket socket = new ServerSocket(0)) {
			portaFechada = socket.getLocalPort();
		}
		DemandaGatewayHttp semServidor = new DemandaGatewayHttp(new WxnApiProperties(
				"http://127.0.0.1:" + portaFechada, Duration.ofMillis(300), Duration.ofMillis(300)));

		assertThrows(DemandaIndisponivelException.class,
				() -> semServidor.abrirChamado(empresaId, new NovoChamado("Titulo valido", ""), UUID.randomUUID()));
	}
}
