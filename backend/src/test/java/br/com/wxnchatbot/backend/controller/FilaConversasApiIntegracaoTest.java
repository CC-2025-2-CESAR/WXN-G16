package br.com.wxnchatbot.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
import br.com.wxnchatbot.backend.model.StatusTicket;
import br.com.wxnchatbot.backend.model.Ticket;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.service.AtendenteResumo;
import br.com.wxnchatbot.backend.service.ConversaNaFila;
import br.com.wxnchatbot.backend.service.DetalheConversaFila;
import br.com.wxnchatbot.backend.service.FilaConversasServico;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class FilaConversasApiIntegracaoTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JsonMapper json;

	@Autowired
	private FilaConversasServico servico;

	@Autowired
	private PlatformTransactionManager transacoes;

	@PersistenceContext
	private EntityManager em;

	private TransactionTemplate transacao;
	private UUID empresaId;
	private UUID chatbotId;
	private UUID primeiraId;
	private UUID segundaId;
	private UUID comChatbotId;
	private UUID disponivelId;
	private UUID indisponivelId;
	private UUID outroDisponivelId;
	private final List<UUID> clientesIds = new ArrayList<>();

	@BeforeEach
	void criarDados() {
		transacao = new TransactionTemplate(transacoes);
		transacao.executeWithoutResult(status -> {
			Empresa empresa = new Empresa("Empresa Demo", "12345678000199");
			em.persist(empresa);
			empresaId = empresa.getId();
			Chatbot lily = new Chatbot("Lily", "Oi, sou a Lily!");
			em.persist(lily);
			chatbotId = lily.getId();

			primeiraId = novaConversa(empresa, lily, "Maria Souza", "81999990001", "Preciso de ajuda com a nota").getId();
			segundaId = novaConversa(empresa, lily, "João Pedro", "81999990002", "O relatório está errado").getId();
			comChatbotId = novaConversa(empresa, lily, "Ana Lima", "81999990003", "Bom dia").getId();

			disponivelId = novoAtendente("Carlos Teste", "carlos@wxn.example", true);
			indisponivelId = novoAtendente("Aline Teste", "aline@wxn.example", false);
			outroDisponivelId = novoAtendente("Bruno Teste", "bruno@wxn.example", true);
		});
		servico.solicitarAtendente(primeiraId, "Quer falar com uma pessoa");
		esperar();
		servico.solicitarAtendente(segundaId, "Problema no relatório de vendas");
	}

	@AfterEach
	void limpar() {
		transacao.executeWithoutResult(status -> {
			List.of(primeiraId, segundaId, comChatbotId).forEach(id -> remover(Conversa.class, id));
			List.of(disponivelId, indisponivelId, outroDisponivelId).forEach(id -> remover(Atendente.class, id));
			clientesIds.forEach(id -> remover(Cliente.class, id));
			remover(Empresa.class, empresaId);
			remover(Chatbot.class, chatbotId);
		});
	}

	private Conversa novaConversa(Empresa empresa, Chatbot lily, String nome, String telefone, String mensagem) {
		Cliente cliente = new Cliente(empresa, nome, telefone);
		em.persist(cliente);
		clientesIds.add(cliente.getId());
		Conversa conversa = new Conversa(lily);
		conversa.vincularCliente(cliente);
		em.persist(conversa);
		lily.saudar(conversa);
		esperar();
		conversa.adicionarMensagem(cliente, mensagem);
		return conversa;
	}

	private UUID novoAtendente(String nome, String email, boolean disponivel) {
		Atendente atendente = new Atendente(nome, email);
		if (!disponivel) {
			atendente.marcarIndisponivel();
		}
		em.persist(atendente);
		return atendente.getId();
	}

	private <T> void remover(Class<T> tipo, UUID id) {
		T entidade = em.find(tipo, id);
		if (entidade != null) {
			em.remove(entidade);
		}
	}

	private static void esperar() {
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private ResultActions assumir(UUID conversaId, UUID atendenteId) throws Exception {
		return mvc.perform(post("/api/fila/{id}/assumir", conversaId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\":\"" + atendenteId + "\"}"));
	}

	private List<ConversaNaFila> minhasNaFila() throws Exception {
		String corpo = mvc.perform(get("/api/fila")).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		return Arrays.stream(json.readValue(corpo, ConversaNaFila[].class))
				.filter(item -> List.of(primeiraId, segundaId, comChatbotId).contains(item.conversaId())).toList();
	}

	private <T> T lerConversa(UUID id, Function<Conversa, T> leitura) {
		return transacao.execute(status -> leitura.apply(em.find(Conversa.class, id)));
	}

	@Test
	void fluxoCompletoDaFilaAoEncerramentoConferindoOBanco() throws Exception {
		List<ConversaNaFila> fila = minhasNaFila();
		assertEquals(List.of(primeiraId, segundaId), fila.stream().map(ConversaNaFila::conversaId).toList());
		assertTrue(fila.get(0).posicao() < fila.get(1).posicao());
		assertEquals("Maria Souza", fila.get(0).clienteNome());
		assertEquals("81999990001", fila.get(0).clienteTelefone());
		assertEquals("Quer falar com uma pessoa", fila.get(0).motivo());
		assertTrue(fila.get(0).abertoEm().isBefore(fila.get(1).abertoEm()));
		assertEquals("João Pedro", fila.get(1).clienteNome());

		String corpoDetalhe = mvc.perform(get("/api/fila/{id}", primeiraId)).andExpect(status().isOk()).andReturn()
				.getResponse().getContentAsString(StandardCharsets.UTF_8);
		DetalheConversaFila detalhe = json.readValue(corpoDetalhe, DetalheConversaFila.class);
		assertEquals("Maria Souza", detalhe.clienteNome());
		assertEquals("Quer falar com uma pessoa", detalhe.motivo());
		assertEquals(List.of(TipoRemetente.CHATBOT, TipoRemetente.CLIENTE),
				detalhe.mensagens().stream().map(DetalheConversaFila.MensagemResumo::tipoRemetente).toList());
		assertEquals(List.of("Oi, sou a Lily!", "Preciso de ajuda com a nota"),
				detalhe.mensagens().stream().map(DetalheConversaFila.MensagemResumo::conteudo).toList());
		mvc.perform(get("/api/fila/{id}", comChatbotId)).andExpect(status().isConflict());
		mvc.perform(get("/api/fila/{id}", UUID.randomUUID())).andExpect(status().isNotFound());

		assumir(primeiraId, indisponivelId).andExpect(status().isConflict());
		assumir(primeiraId, UUID.randomUUID()).andExpect(status().isNotFound());
		assumir(UUID.randomUUID(), disponivelId).andExpect(status().isNotFound());
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, lerConversa(primeiraId, Conversa::getStatus));

		assumir(primeiraId, disponivelId).andExpect(status().isNoContent()).andExpect(content().string(""));

		transacao.executeWithoutResult(status -> {
			Conversa conversa = em.find(Conversa.class, primeiraId);
			assertEquals(StatusConversa.COM_ATENDENTE, conversa.getStatus());
			assertEquals(disponivelId, conversa.getAtendente().orElseThrow().getId());
			Ticket ticket = conversa.getTicket().orElseThrow();
			assertEquals(StatusTicket.EM_ATENDIMENTO, ticket.getStatus());
			assertEquals(disponivelId, ticket.getAtendente().orElseThrow().getId());
		});
		assertEquals(List.of(segundaId), minhasNaFila().stream().map(ConversaNaFila::conversaId).toList());
		mvc.perform(get("/api/fila/{id}", primeiraId)).andExpect(status().isConflict());
		assumir(primeiraId, outroDisponivelId).andExpect(status().isConflict());

		mvc.perform(post("/api/conversas/{id}/encerrar", primeiraId)).andExpect(status().isNoContent())
				.andExpect(content().string(""));

		transacao.executeWithoutResult(status -> {
			Conversa conversa = em.find(Conversa.class, primeiraId);
			assertEquals(StatusConversa.ENCERRADA, conversa.getStatus());
			assertTrue(conversa.getEncerradaEm().isPresent());
			Ticket ticket = conversa.getTicket().orElseThrow();
			assertEquals(StatusTicket.RESOLVIDO, ticket.getStatus());
			assertTrue(ticket.getResolvidoEm().isPresent());
		});
		mvc.perform(post("/api/conversas/{id}/encerrar", primeiraId)).andExpect(status().isConflict());
		mvc.perform(post("/api/conversas/{id}/encerrar", UUID.randomUUID())).andExpect(status().isNotFound());
		assertEquals(List.of(segundaId), minhasNaFila().stream().map(ConversaNaFila::conversaId).toList());
	}

	@Test
	void atendentesVemOrdenadosPorNomeComADisponibilidade() throws Exception {
		String corpo = mvc.perform(get("/api/atendentes")).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString(StandardCharsets.UTF_8);

		List<AtendenteResumo> meus = Arrays.stream(json.readValue(corpo, AtendenteResumo[].class))
				.filter(a -> List.of(disponivelId, indisponivelId, outroDisponivelId).contains(a.id())).toList();

		assertEquals(List.of("Aline Teste", "Bruno Teste", "Carlos Teste"), meus.stream().map(AtendenteResumo::nome).toList());
		assertEquals(List.of(false, true, true), meus.stream().map(AtendenteResumo::disponivel).toList());
		assertEquals("aline@wxn.example", meus.get(0).email());
	}

	@Test
	void doisAssumirSimultaneosNaMesmaConversaDaUmNoContentEUmConflito() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CyclicBarrier largada = new CyclicBarrier(2);
		try {
			List<UUID> candidatos = List.of(disponivelId, outroDisponivelId);
			List<Future<Integer>> respostas = new ArrayList<>();
			for (UUID atendenteId : candidatos) {
				respostas.add(pool.submit(() -> {
					largada.await(15, TimeUnit.SECONDS);
					return assumir(segundaId, atendenteId).andReturn().getResponse().getStatus();
				}));
			}
			List<Integer> codigos = new ArrayList<>();
			for (Future<Integer> resposta : respostas) {
				codigos.add(resposta.get(30, TimeUnit.SECONDS));
			}

			assertEquals(List.of(204, 409), codigos.stream().sorted().toList());
			UUID vencedor = candidatos.get(codigos.indexOf(204));
			transacao.executeWithoutResult(status -> {
				Conversa conversa = em.find(Conversa.class, segundaId);
				assertEquals(StatusConversa.COM_ATENDENTE, conversa.getStatus());
				assertEquals(vencedor, conversa.getAtendente().orElseThrow().getId());
				assertEquals(vencedor, conversa.getTicket().orElseThrow().getAtendente().orElseThrow().getId());
			});
		} finally {
			pool.shutdownNow();
		}
	}
}
