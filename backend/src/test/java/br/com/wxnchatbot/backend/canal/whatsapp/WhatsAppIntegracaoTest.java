package br.com.wxnchatbot.backend.canal.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

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
import br.com.wxnchatbot.backend.model.TipoRemetente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Do webhook assinado da Meta até a resposta enviada pelo WhatsApp, com banco de verdade e a Meta de mentira. */
@SpringBootTest(properties = { "whatsapp.habilitado=true", "whatsapp.token-de-verificacao=verificacao",
		"whatsapp.segredo-do-app=segredo-do-app", "whatsapp.token-de-acesso=acesso", "whatsapp.id-do-numero=123",
		"chat.empresa-padrao=Empresa Teste WhatsApp", "chat.empresa-padrao-id=" })
@AutoConfigureMockMvc
class WhatsAppIntegracaoTest {

	private static final String EMPRESA = "Empresa Teste WhatsApp";
	private static final String WA_ID = "5581977771111";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PlatformTransactionManager transacoes;

	@PersistenceContext
	private EntityManager em;

	@MockitoBean
	private WhatsAppClient whatsApp;

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

	private static String entrega(String idDaMensagem, String texto) {
		return "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"1\",\"changes\":[{\"field\":\"messages\",\"value\":{"
				+ "\"messaging_product\":\"whatsapp\",\"contacts\":[{\"profile\":{\"name\":\"Cliente do Teste\"},\"wa_id\":\"" + WA_ID + "\"}],"
				+ "\"messages\":[{\"from\":\"" + WA_ID + "\",\"id\":\"" + idDaMensagem + "\",\"type\":\"text\",\"text\":{\"body\":\"" + texto + "\"}}]}}]}]}";
	}

	private void entregar(String corpo) throws Exception {
		byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(bytes)
				.header("X-Hub-Signature-256", AssinaturaDaMeta.assinar(bytes, "segredo-do-app"))).andExpect(status().isOk());
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
	void mensagemDoWhatsAppViraConversaERespostaEnviadaDeVolta() throws Exception {
		entregar(entrega("wamid.INT1", "Oi, tudo bem?"));

		verify(whatsApp, timeout(10_000)).enviarTexto(eq(WA_ID), contains("Oi, tudo bem?"));
		List<Conversa> conversas = conversasDoCliente();
		assertEquals(1, conversas.size());
		assertEquals(2, conversas.get(0).getMensagens().size());
		assertEquals(TipoRemetente.CLIENTE, conversas.get(0).getMensagens().get(0).getTipoRemetente());
		assertEquals(OrigemConversa.WHATSAPP, conversas.get(0).getOrigem());
	}

	@Test
	void mesmaEntregaReenviadaPelaMetaSoGeraUmaResposta() throws Exception {
		String corpo = entrega("wamid.INT2", "Oi");

		entregar(corpo);
		entregar(corpo);

		verify(whatsApp, after(1500).times(1)).enviarTexto(eq(WA_ID), anyString());
		assertEquals(2, conversasDoCliente().get(0).getMensagens().size());
	}

	@Test
	void pedidoDeAtendentePeloWhatsAppColocaAConversaNaFila() throws Exception {
		entregar(entrega("wamid.INT3", "Quero falar com um atendente"));

		verify(whatsApp, timeout(10_000)).enviarTexto(eq(WA_ID), contains("atendente"));
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversasDoCliente().get(0).getStatus());
	}

	@Test
	void entregaSemAssinaturaNaoGeraNada() throws Exception {
		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(entrega("wamid.INT4", "Oi")))
				.andExpect(status().isForbidden());

		verify(whatsApp, after(500).never()).enviarTexto(anyString(), anyString());
		assertTrue(conversasDoCliente().isEmpty());
	}
}
