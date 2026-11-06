package br.com.wxnchatbot.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.wxnchatbot.backend.integracao.wxn.Chamado;
import br.com.wxnchatbot.backend.integracao.wxn.Demanda;
import br.com.wxnchatbot.backend.integracao.wxn.DemandaGateway;
import br.com.wxnchatbot.backend.integracao.wxn.NovoChamado;
import br.com.wxnchatbot.backend.integracao.wxn.StatusDemanda;
import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.NivelUrgencia;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.StatusTicket;
import br.com.wxnchatbot.backend.model.Ticket;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.service.ConversaNaFila;
import br.com.wxnchatbot.backend.service.RespostaChat;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "chat.empresa-padrao=Empresa Teste do Chat")
@AutoConfigureMockMvc
class ChatFluxoIntegracaoTest {

	private static final String EMPRESA = "Empresa Teste do Chat";
	private static final String TELEFONE = "5581988880001";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JsonMapper json;

	@Autowired
	private PlatformTransactionManager transacoes;

	@PersistenceContext
	private EntityManager em;

	@MockitoBean
	private DemandaGateway gateway;

	private TransactionTemplate transacao;
	private UUID atendenteId;

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
					em.remove(em.find(br.com.wxnchatbot.backend.model.Chatbot.class, id));
				}
			});
			if (atendenteId != null) {
				Atendente atendente = em.find(Atendente.class, atendenteId);
				if (atendente != null) {
					em.remove(atendente);
				}
			}
		});
		atendenteId = null;
	}

	private RespostaChat enviar(String telefone, String texto) throws Exception {
		String corpo = mvc.perform(post("/api/chat/mensagens").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("telefone", telefone, "nome", "Teste do Chat", "texto", texto))))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return json.readValue(corpo, RespostaChat.class);
	}

	private <T> T naConversa(UUID conversaId, java.util.function.Function<Conversa, T> leitura) {
		return transacao.execute(status -> leitura.apply(em.find(Conversa.class, conversaId)));
	}

	private int quantidadeDeMensagens(UUID conversaId) {
		return naConversa(conversaId, c -> c.getMensagens().size());
	}

	@Test
	void primeiraMensagemCadastraOClienteAbreAConversaEResponde() throws Exception {
		RespostaChat primeira = enviar(TELEFONE, "Oi, tudo bem?");

		assertNotNull(primeira.conversaId());
		assertEquals(StatusConversa.COM_CHATBOT, primeira.status());
		assertTrue(primeira.resposta().contains("Oi, tudo bem?"));
		assertEquals(2, quantidadeDeMensagens(primeira.conversaId()));
		assertEquals("81988880001", naConversa(primeira.conversaId(), c -> c.getCliente().getTelefone()));
		assertEquals(EMPRESA, naConversa(primeira.conversaId(), c -> c.getEmpresa().getNome()));

		RespostaChat segunda = enviar("(81) 98888-0001", "Mais uma dúvida");

		assertEquals(primeira.conversaId(), segunda.conversaId());
		assertEquals(4, quantidadeDeMensagens(primeira.conversaId()));
	}

	@Test
	void consultaDeDemandaUsaAApiDaWxnESemLlm() throws Exception {
		when(gateway.consultarPorProtocolo(any(), eq("WXN-2026-0001"))).thenReturn(Optional.of(new Demanda("WXN-2026-0001",
				"Ajuste no relatório", StatusDemanda.EM_ANDAMENTO, Instant.parse("2026-10-20T14:30:00Z"), "Em desenvolvimento")));

		RespostaChat resposta = enviar(TELEFONE, "Qual o status da WXN-2026-0001?");

		assertTrue(resposta.resposta().contains("Ajuste no relatório"));
		assertTrue(resposta.resposta().contains("em andamento"));
		assertEquals(StatusConversa.COM_CHATBOT, resposta.status());
	}

	@Test
	void consultaSemProtocoloPedeEDepoisEncaminhaQuandoClienteNaoTem() throws Exception {
		RespostaChat pedido = enviar(TELEFONE, "Quero saber o andamento da minha demanda");
		assertTrue(pedido.resposta().contains("protocolo"));

		RespostaChat semProtocolo = enviar(TELEFONE, "Não lembro, pedi por telefone");

		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, semProtocolo.status());
		assertTrue(semProtocolo.resposta().contains("atendente"));
	}

	@Test
	void chamadoSoEAbertoDepoisDoSimDoCliente() throws Exception {
		RespostaChat pediu = enviar(TELEFONE, "Quero abrir um chamado");
		assertTrue(pediu.resposta().contains("Me conta o que está acontecendo"));

		RespostaChat descreveu = enviar(TELEFONE, "O financeiro não abre depois que troquei a senha. Os outros módulos abrem.");
		assertTrue(descreveu.resposta().contains("Posso confirmar?"));
		verify(gateway, never()).abrirChamado(any(), any(), any());

		when(gateway.abrirChamado(any(), any(), any())).thenReturn(
				new Chamado("WXN-2026-0099", "O financeiro não abre depois que troquei a senha", StatusDemanda.ABERTA, Instant.now()));
		RespostaChat confirmou = enviar(TELEFONE, "sim");

		assertTrue(confirmou.resposta().contains("WXN-2026-0099"));
		ArgumentCaptor<NovoChamado> chamado = ArgumentCaptor.forClass(NovoChamado.class);
		verify(gateway).abrirChamado(any(), chamado.capture(), any());
		assertEquals("O financeiro não abre depois que troquei a senha", chamado.getValue().titulo());
		assertEquals("O financeiro não abre depois que troquei a senha. Os outros módulos abrem.", chamado.getValue().descricao());
	}

	@Test
	void naoDepoisDoPedidoCancelaESemAbrirChamado() throws Exception {
		enviar(TELEFONE, "Quero abrir um chamado");
		enviar(TELEFONE, "A nota fiscal trava na assinatura desde ontem e já tentei dez vezes");

		RespostaChat resposta = enviar(TELEFONE, "não, deixa");

		assertTrue(resposta.resposta().contains("não abri o chamado"));
		verify(gateway, never()).abrirChamado(any(), any(), any());
	}

	@Test
	void pedidoDeAtendenteLevaAFilaOAtendenteAssumeEAsMensagensSeguintesFicamComEle() throws Exception {
		RespostaChat pediu = enviar(TELEFONE, "Quero falar com um atendente, o sistema está fora do ar!");

		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, pediu.status());
		UUID conversaId = pediu.conversaId();
		naConversa(conversaId, c -> {
			Ticket ticket = c.getTicket().orElseThrow();
			assertEquals("O cliente pediu para falar com um atendente", ticket.getMotivo());
			assertEquals(NivelUrgencia.ALTA, ticket.getUrgencia());
			assertEquals(StatusTicket.ABERTO, ticket.getStatus());
			assertTrue(ticket.getResumo().contains("fora do ar"));
			return null;
		});

		String corpoFila = mvc.perform(get("/api/fila")).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		List<ConversaNaFila> minhas = Arrays.stream(json.readValue(corpoFila, ConversaNaFila[].class))
				.filter(item -> item.conversaId().equals(conversaId)).toList();
		assertEquals(1, minhas.size());
		assertEquals("O cliente pediu para falar com um atendente", minhas.get(0).motivo());

		atendenteId = transacao.execute(status -> {
			Atendente atendente = new Atendente("Atendente do Teste", "chat.teste@wxn.example");
			em.persist(atendente);
			return atendente.getId();
		});
		mvc.perform(post("/api/fila/{id}/assumir", conversaId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\":\"" + atendenteId + "\"}")).andExpect(status().isNoContent());

		RespostaChat comAtendente = enviar(TELEFONE, "Obrigado por assumir");

		assertEquals(conversaId, comAtendente.conversaId());
		assertEquals(StatusConversa.COM_ATENDENTE, comAtendente.status());
		assertNull(comAtendente.resposta());
		assertEquals(TipoRemetente.CLIENTE, naConversa(conversaId, c -> {
			List<br.com.wxnchatbot.backend.model.Mensagem> mensagens = c.getMensagens();
			return mensagens.get(mensagens.size() - 1).getTipoRemetente();
		}));

		mvc.perform(post("/api/conversas/{id}/encerrar", conversaId)).andExpect(status().isNoContent());

		RespostaChat nova = enviar(TELEFONE, "Oi de novo");
		assertNotEquals(conversaId, nova.conversaId());
		assertEquals(StatusConversa.COM_CHATBOT, nova.status());
	}

	@Test
	void mensagemNumaConversaExistentePeloIdTambemFunciona() throws Exception {
		UUID conversaId = enviar(TELEFONE, "Oi").conversaId();

		String corpo = mvc.perform(post("/api/conversas/{id}/mensagens", conversaId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"Tudo bem?\"}")).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString(StandardCharsets.UTF_8);
		RespostaChat resposta = json.readValue(corpo, RespostaChat.class);

		assertEquals(conversaId, resposta.conversaId());
		assertTrue(resposta.resposta().contains("Tudo bem?"));
		assertEquals(4, quantidadeDeMensagens(conversaId));
	}

	@Test
	void conversaInexistenteDevolve404() throws Exception {
		mvc.perform(post("/api/conversas/{id}/mensagens", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"Oi\"}")).andExpect(status().isNotFound());
	}
}
