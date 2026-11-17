package br.com.wxnchatbot.backend.canal.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * O modo polling de ponta a ponta: o cliente HTTP de verdade conversa com um "Telegram" de mentira (um servidor local),
 * que entrega uma mensagem; a Lily responde e o servidor confere o {@code sendMessage} que chegou.
 */
@SpringBootTest(properties = { "telegram.habilitado=true", "telegram.modo=polling", "telegram.token-do-bot=teste",
		"telegram.espera-do-polling=1", "chat.empresa-padrao=Empresa Teste Polling Telegram", "chat.empresa-padrao-id=" })
class TelegramPollingIntegracaoTest {

	private static final String EMPRESA = "Empresa Teste Polling Telegram";
	private static final long CHAT = 7_700_000_002L;
	private static final HttpServer TELEGRAM_DE_MENTIRA = subir();
	private static final List<String> ENVIADOS = new CopyOnWriteArrayList<>();
	private static final AtomicBoolean ENTREGUE = new AtomicBoolean(false);

	@Autowired
	private PlatformTransactionManager transacoes;

	@PersistenceContext
	private EntityManager em;

	private TransactionTemplate transacao;

	private static HttpServer subir() {
		try {
			HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			servidor.createContext("/botteste/getUpdates", TelegramPollingIntegracaoTest::buscar);
			servidor.createContext("/botteste/sendMessage", TelegramPollingIntegracaoTest::enviar);
			servidor.start();
			return servidor;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@DynamicPropertySource
	static void apontarParaOTelegramDeMentira(DynamicPropertyRegistry registro) {
		registro.add("telegram.url-base", () -> "http://127.0.0.1:" + TELEGRAM_DE_MENTIRA.getAddress().getPort());
	}

	private static void buscar(HttpExchange troca) throws IOException {
		String corpo = "{\"ok\":true,\"result\":[]}";
		if (ENTREGUE.compareAndSet(false, true)) {
			corpo = "{\"ok\":true,\"result\":[{\"update_id\":900,\"message\":{\"message_id\":1,\"from\":{\"id\":" + CHAT
					+ ",\"is_bot\":false,\"first_name\":\"Cliente\"},\"chat\":{\"id\":" + CHAT
					+ ",\"type\":\"private\"},\"text\":\"Oi, tudo bem?\"}}]}";
		} else {
			esperar(200);
		}
		responder(troca, corpo);
	}

	private static void enviar(HttpExchange troca) throws IOException {
		ENVIADOS.add(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		responder(troca, "{\"ok\":true,\"result\":{}}");
	}

	private static void responder(HttpExchange troca, String corpo) throws IOException {
		byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
		troca.getResponseHeaders().add("Content-Type", "application/json");
		troca.sendResponseHeaders(200, bytes.length);
		troca.getResponseBody().write(bytes);
		troca.close();
	}

	private static void esperar(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@BeforeEach
	void preparar() {
		transacao = new TransactionTemplate(transacoes);
	}

	@AfterEach
	void limpar() {
		transacao.executeWithoutResult(status -> {
			Set<UUID> chatbots = new HashSet<>();
			for (Cliente cliente : em.createQuery("select c from Cliente c where c.empresa.nome = :nome", Cliente.class)
					.setParameter("nome", EMPRESA).getResultList()) {
				for (Conversa conversa : em.createQuery("select v from Conversa v where v.cliente.id = :id", Conversa.class)
						.setParameter("id", cliente.getId()).getResultList()) {
					chatbots.add(conversa.getChatbot().getId());
					em.remove(conversa);
				}
				em.remove(cliente);
			}
			em.flush();
			em.createQuery("select e from Empresa e where e.nome = :nome", Empresa.class).setParameter("nome", EMPRESA)
					.getResultList().forEach(em::remove);
			chatbots.forEach(id -> {
				Long usos = em.createQuery("select count(v) from Conversa v where v.chatbot.id = :id", Long.class)
						.setParameter("id", id).getSingleResult();
				if (usos == 0) {
					em.remove(em.find(Chatbot.class, id));
				}
			});
		});
	}

	@Test
	void mensagemBuscadaPeloPollingViraConversaERespostaEnviadaAoTelegram() throws Exception {
		long limite = System.currentTimeMillis() + 15_000;
		while (ENVIADOS.isEmpty() && System.currentTimeMillis() < limite) {
			Thread.sleep(100);
		}

		assertEquals(1, ENVIADOS.size(), "o Telegram de mentira devia ter recebido uma resposta");
		assertTrue(ENVIADOS.get(0).contains("\"chat_id\":" + CHAT), ENVIADOS.get(0));
		assertTrue(ENVIADOS.get(0).contains("Oi, tudo bem?"), ENVIADOS.get(0));
		List<Conversa> conversas = transacao.execute(status -> em
				.createQuery("select v from Conversa v where v.cliente.empresa.nome = :nome", Conversa.class)
				.setParameter("nome", EMPRESA).getResultList());
		assertEquals(1, conversas.size());
		assertEquals(OrigemConversa.TELEGRAM, conversas.get(0).getOrigem());
	}
}
