package br.com.wxnchatbot.backend.canal.telegram;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * O atendente assume uma conversa que veio do Telegram, responde pelo dashboard e a mensagem chega ao cliente pelo
 * Telegram. Com banco de verdade e o Telegram de mentira.
 */
@SpringBootTest(properties = { "telegram.habilitado=true", "telegram.modo=webhook", "telegram.token-do-bot=123:ABC",
		"telegram.segredo-do-webhook=segredo-do-webhook", "chat.empresa-padrao=Empresa Teste Atendimento",
		"chat.empresa-padrao-id=" })
@AutoConfigureMockMvc
class RespostaDoAtendentePeloTelegramIntegracaoTest {

	private static final String EMPRESA = "Empresa Teste Atendimento";
	private static final String EMAIL_DO_ATENDENTE = "carlos.teste.atendimento@wxn.com.br";
	private static final long CHAT = 7_700_000_002L;
	private static final String SEGREDO = "segredo-do-webhook";
	/** O processador guarda os ids já vistos pelo tempo de vida do contexto, então cada mensagem precisa de um id novo. */
	private static final AtomicLong PROXIMO_ID = new AtomicLong(50_000);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PlatformTransactionManager transacoes;

	@PersistenceContext
	private EntityManager em;

	@MockitoBean
	private TelegramClient telegram;

	private TransactionTemplate transacao;
	private UUID atendenteId;

	@BeforeEach
	void preparar() {
		transacao = new TransactionTemplate(transacoes);
		limpar();
		atendenteId = transacao.execute(status -> {
			Atendente atendente = new Atendente("Carlos Lima", EMAIL_DO_ATENDENTE);
			em.persist(atendente);
			return atendente.getId();
		});
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
			em.createQuery("select a from Atendente a where a.email = :email", Atendente.class)
					.setParameter("email", EMAIL_DO_ATENDENTE).getResultList().forEach(em::remove);
		});
	}

	private static String atualizacao(long id, String texto) {
		return "{\"update_id\":" + id + ",\"message\":{\"message_id\":" + id + ",\"from\":{\"id\":" + CHAT
				+ ",\"is_bot\":false,\"first_name\":\"Cliente\",\"last_name\":\"do Atendimento\"},\"chat\":{\"id\":" + CHAT
				+ ",\"type\":\"private\"},\"date\":1441645532,\"text\":\"" + texto + "\"}}";
	}

	private void clienteEscreve(String texto) throws Exception {
		mvc.perform(post("/webhook/telegram").contentType(MediaType.APPLICATION_JSON)
				.content(atualizacao(PROXIMO_ID.incrementAndGet(), texto).getBytes(StandardCharsets.UTF_8))
				.header("X-Telegram-Bot-Api-Secret-Token", SEGREDO)).andExpect(status().isOk());
	}

	private UUID conversaDoCliente() {
		return transacao.execute(status -> em
				.createQuery("select v from Conversa v where v.cliente.empresa.nome = :nome", Conversa.class)
				.setParameter("nome", EMPRESA).getSingleResult().getId());
	}

	private int mensagensNoBanco(UUID conversa) {
		return transacao.execute(status -> em.find(Conversa.class, conversa).getMensagens().size());
	}

	private StatusConversa statusNoBanco(UUID conversa) {
		return transacao.execute(status -> em.find(Conversa.class, conversa).getStatus());
	}

	private void esperarAteHaMensagens(UUID conversa, int quantas) throws InterruptedException {
		for (int i = 0; i < 100 && mensagensNoBanco(conversa) < quantas; i++) {
			Thread.sleep(100);
		}
		assertEquals(quantas, mensagensNoBanco(conversa));
	}

	/** O cliente pede um atendente pelo Telegram, a Lily encaminha e o atendente assume. Devolve a conversa. */
	private UUID conversaNaFila() throws Exception {
		clienteEscreve("Quero falar com um atendente");
		verify(telegram, timeout(10_000)).enviarTexto(eq(CHAT), contains("atendente"));
		UUID conversa = conversaDoCliente();
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, statusNoBanco(conversa));
		clearInvocations(telegram);
		return conversa;
	}

	private UUID conversaEmAtendimento() throws Exception {
		UUID conversa = conversaNaFila();
		mvc.perform(post("/api/fila/{id}/assumir", conversa).contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\":\"" + atendenteId + "\"}")).andExpect(status().isNoContent());
		assertEquals(StatusConversa.COM_ATENDENTE, statusNoBanco(conversa));
		return conversa;
	}

	private ResultActions responder(UUID conversa, String texto) throws Exception {
		return mvc.perform(post("/api/conversas/{id}/atendimento/mensagens", conversa).contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"" + texto + "\"}"));
	}

	@Test
	void respostaDoAtendenteChegaAoClienteNoTelegramEFicaNoHistorico() throws Exception {
		UUID conversa = conversaEmAtendimento();

		responder(conversa, "Oi, sou o Carlos. Como posso ajudar?")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entregue").value(true))
				.andExpect(jsonPath("$.aviso").doesNotExist())
				.andExpect(jsonPath("$.mensagem.tipoRemetente").value("ATENDENTE"))
				.andExpect(jsonPath("$.mensagem.conteudo").value("Oi, sou o Carlos. Como posso ajudar?"));

		verify(telegram).enviarTexto(CHAT, "Oi, sou o Carlos. Como posso ajudar?");
		mvc.perform(get("/api/conversas/{id}/atendimento/mensagens", conversa))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].tipoRemetente").value("CLIENTE"))
				.andExpect(jsonPath("$[1].tipoRemetente").value("CHATBOT"))
				.andExpect(jsonPath("$[2].tipoRemetente").value("ATENDENTE"))
				.andExpect(jsonPath("$[2].enviadaEm").isNotEmpty());
	}

	@Test
	void mensagemNovaDoClienteDuranteOAtendimentoApareceNoHistoricoESemRespostaDaLily() throws Exception {
		UUID conversa = conversaEmAtendimento();
		responder(conversa, "Pode me passar o protocolo?").andExpect(status().isOk());
		clearInvocations(telegram);

		clienteEscreve("É o WXN-2026-0001");

		esperarAteHaMensagens(conversa, 4);
		verify(telegram, after(700).never()).enviarTexto(anyLong(), anyString());
		assertEquals(StatusConversa.COM_ATENDENTE, statusNoBanco(conversa));
		mvc.perform(get("/api/conversas/{id}/atendimento/mensagens", conversa))
				.andExpect(jsonPath("$[3].tipoRemetente").value("CLIENTE"))
				.andExpect(jsonPath("$[3].conteudo").value("É o WXN-2026-0001"));
	}

	@Test
	void seOTelegramRecusarAMensagemFicaSalvaEOAtendenteRecebeOAviso() throws Exception {
		UUID conversa = conversaEmAtendimento();
		doThrow(new FalhaNoTelegramException("Forbidden: bot was blocked by the user", 403)).when(telegram)
				.enviarTexto(eq(CHAT), eq("Está aí?"));

		responder(conversa, "Está aí?")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entregue").value(false))
				.andExpect(jsonPath("$.aviso").value(containsString("bloqueado")))
				.andExpect(jsonPath("$.mensagem.conteudo").value("Está aí?"));

		assertEquals(3, mensagensNoBanco(conversa));
		mvc.perform(get("/api/conversas/{id}/atendimento/mensagens", conversa))
				.andExpect(jsonPath("$[2].conteudo").value("Está aí?"));
	}

	@Test
	void conversaQueAindaEstaNaFilaNaoAceitaResposta() throws Exception {
		UUID conversa = conversaNaFila();

		responder(conversa, "Oi").andExpect(status().isConflict());

		verify(telegram, after(300).never()).enviarTexto(anyLong(), anyString());
		assertEquals(2, mensagensNoBanco(conversa));
	}

	@Test
	void conversaEncerradaNaoAceitaMaisRespostas() throws Exception {
		UUID conversa = conversaEmAtendimento();
		mvc.perform(post("/api/conversas/{id}/encerrar", conversa)).andExpect(status().isNoContent());

		responder(conversa, "Mais uma coisa").andExpect(status().isConflict());

		verify(telegram, after(300).never()).enviarTexto(anyLong(), anyString());
		assertEquals(2, mensagensNoBanco(conversa));
	}

	@Test
	void conversaInexistenteDevolve404() throws Exception {
		responder(UUID.randomUUID(), "Oi").andExpect(status().isNotFound());
		mvc.perform(get("/api/conversas/{id}/atendimento/mensagens", UUID.randomUUID())).andExpect(status().isNotFound());
	}
}
