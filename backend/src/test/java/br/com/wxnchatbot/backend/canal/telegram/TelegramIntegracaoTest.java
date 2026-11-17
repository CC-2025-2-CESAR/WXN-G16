package br.com.wxnchatbot.backend.canal.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Do webhook do Telegram até a resposta enviada de volta, com banco de verdade e o Telegram de mentira. */
@SpringBootTest(properties = { "telegram.habilitado=true", "telegram.modo=webhook", "telegram.token-do-bot=123:ABC",
		"telegram.segredo-do-webhook=segredo-do-webhook", "chat.empresa-padrao=Empresa Teste Telegram",
		"chat.empresa-padrao-id=" })
@AutoConfigureMockMvc
class TelegramIntegracaoTest {

	private static final String EMPRESA = "Empresa Teste Telegram";
	private static final long CHAT = 7_700_000_001L;
	private static final String SEGREDO = "segredo-do-webhook";
	/** O processador guarda os ids já vistos pelo tempo de vida do contexto, então cada mensagem precisa de um id novo. */
	private static final AtomicLong PROXIMO_ID = new AtomicLong(1_000);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PlatformTransactionManager transacoes;

	@PersistenceContext
	private EntityManager em;

	@MockitoBean
	private TelegramClient telegram;

	private TransactionTemplate transacao;

	@BeforeEach
	void preparar() {
		transacao = new TransactionTemplate(transacoes);
		limpar();
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

	private static String atualizacao(long id, String texto) {
		return "{\"update_id\":" + id + ",\"message\":{\"message_id\":" + id + ",\"from\":{\"id\":" + CHAT
				+ ",\"is_bot\":false,\"first_name\":\"Cliente\",\"last_name\":\"do Teste\"},\"chat\":{\"id\":" + CHAT
				+ ",\"type\":\"private\"},\"date\":1441645532,\"text\":\"" + texto + "\"}}";
	}

	private void entregar(String corpo) throws Exception {
		mvc.perform(post("/webhook/telegram").contentType(MediaType.APPLICATION_JSON)
				.content(corpo.getBytes(StandardCharsets.UTF_8)).header("X-Telegram-Bot-Api-Secret-Token", SEGREDO))
				.andExpect(status().isOk());
	}

	private List<Conversa> conversasDoCliente() {
		return transacao.execute(status -> {
			List<Conversa> conversas = em.createQuery("select v from Conversa v where v.cliente.empresa.nome = :nome", Conversa.class)
					.setParameter("nome", EMPRESA).getResultList();
			conversas.forEach(c -> c.getMensagens().size());
			return conversas;
		});
	}

	@Test
	void mensagemDoTelegramViraConversaDoClienteTgEARespostaVoltaPeloTelegram() throws Exception {
		entregar(atualizacao(PROXIMO_ID.incrementAndGet(), "Oi, tudo bem?"));

		verify(telegram, timeout(10_000)).enviarTexto(eq(CHAT), contains("Oi, tudo bem?"));
		List<Conversa> conversas = conversasDoCliente();
		assertEquals(1, conversas.size());
		assertEquals(2, conversas.get(0).getMensagens().size());
		assertEquals(OrigemConversa.TELEGRAM, conversas.get(0).getOrigem());
		assertEquals("tg:" + CHAT, transacao.execute(s -> em.find(Conversa.class, conversas.get(0).getId()).getCliente().getTelefone()));
		assertEquals("Cliente do Teste", transacao.execute(s -> em.find(Conversa.class, conversas.get(0).getId()).getCliente().getNome()));
	}

	@Test
	void asProximasMensagensContinuamAMesmaConversa() throws Exception {
		entregar(atualizacao(PROXIMO_ID.incrementAndGet(), "Oi"));
		verify(telegram, timeout(10_000)).enviarTexto(eq(CHAT), contains("Oi"));

		entregar(atualizacao(PROXIMO_ID.incrementAndGet(), "Mais uma dúvida"));

		verify(telegram, timeout(10_000)).enviarTexto(eq(CHAT), contains("Mais uma dúvida"));
		List<Conversa> conversas = conversasDoCliente();
		assertEquals(1, conversas.size());
		assertEquals(4, conversas.get(0).getMensagens().size());
	}

	@Test
	void pedidoDeAtendentePeloTelegramColocaAConversaNaFila() throws Exception {
		entregar(atualizacao(PROXIMO_ID.incrementAndGet(), "Quero falar com um atendente"));

		verify(telegram, timeout(10_000)).enviarTexto(eq(CHAT), contains("atendente"));
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversasDoCliente().get(0).getStatus());
	}

	@Test
	void mesmaAtualizacaoReenviadaPeloTelegramSoGeraUmaResposta() throws Exception {
		long repetida = PROXIMO_ID.incrementAndGet();
		entregar(atualizacao(repetida, "Oi"));
		entregar(atualizacao(repetida, "Oi"));

		verify(telegram, timeout(10_000)).enviarTexto(eq(CHAT), anyString());
		verify(telegram, after(500).times(1)).enviarTexto(anyLong(), anyString());
		assertEquals(2, conversasDoCliente().get(0).getMensagens().size());
	}

	@Test
	void startSoRecebeBoasVindasENaoCriaConversa() throws Exception {
		entregar(atualizacao(PROXIMO_ID.incrementAndGet(), "/start"));

		verify(telegram, timeout(10_000)).enviarTexto(eq(CHAT), contains("Lily"));
		assertTrue(conversasDoCliente().isEmpty());
	}

	@Test
	void entregaSemOSegredoNaoGeraNada() throws Exception {
		mvc.perform(post("/webhook/telegram").contentType(MediaType.APPLICATION_JSON)
				.content(atualizacao(1, "Oi").getBytes(StandardCharsets.UTF_8))).andExpect(status().isForbidden());

		verify(telegram, after(500).never()).enviarTexto(anyLong(), anyString());
		assertTrue(conversasDoCliente().isEmpty());
	}
}
