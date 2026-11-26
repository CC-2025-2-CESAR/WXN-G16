package br.com.wxnchatbot.backend.canal.twilio;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static br.com.wxnchatbot.backend.autenticacao.SessaoDeTeste.comoAtendente;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Do webhook do Twilio até a resposta enviada de volta, e da resposta do atendente no dashboard até o WhatsApp do
 * cliente, com banco de verdade e o Twilio de mentira. As chamadas do webhook levam a assinatura calculada de verdade.
 */
@SpringBootTest(properties = { "twilio.habilitado=true", "twilio.account-sid=AC_SID_FALSO_PARA_TESTES",
		"twilio.auth-token=token-secreto-do-twilio", "twilio.numero-de-origem=+14155238886",
		"twilio.url-do-webhook=https://wxn-g16-backend.azurewebsites.net/webhook/twilio/whatsapp",
		"chat.empresa-padrao=Empresa Teste Twilio", "chat.empresa-padrao-id=" })
@AutoConfigureMockMvc
class TwilioIntegracaoTest {

	private static final String EMPRESA = "Empresa Teste Twilio";
	private static final String EMAIL_DO_ATENDENTE = "carlos.teste.twilio@wxn.com.br";
	private static final String URL_DO_WEBHOOK = "https://wxn-g16-backend.azurewebsites.net/webhook/twilio/whatsapp";
	private static final String TOKEN = "token-secreto-do-twilio";
	private static final String DE = "whatsapp:+5581977770003";
	private static final String TELEFONE_NO_CADASTRO = "81977770003";
	private static final AtomicLong PROXIMO_ID = new AtomicLong(1_000);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PlatformTransactionManager transacoes;

	@PersistenceContext
	private EntityManager em;

	@MockitoBean
	private TwilioClient twilio;

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

	private static Map<String, List<String>> formulario(String texto) {
		Map<String, List<String>> campos = new LinkedHashMap<>();
		campos.put("MessageSid", List.of("SM" + PROXIMO_ID.incrementAndGet()));
		campos.put("From", List.of(DE));
		campos.put("To", List.of("whatsapp:+14155238886"));
		campos.put("Body", List.of(texto));
		campos.put("ProfileName", List.of("Cliente do Twilio"));
		campos.put("NumMedia", List.of("0"));
		return campos;
	}

	private ResultActions webhook(Map<String, List<String>> campos, String assinatura) throws Exception {
		MockHttpServletRequestBuilder pedido = post("/webhook/twilio/whatsapp").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.header("X-Twilio-Signature", assinatura);
		campos.forEach((nome, valores) -> pedido.param(nome, valores.toArray(new String[0])));
		return mvc.perform(pedido);
	}

	private void clienteEscreve(String texto) throws Exception {
		Map<String, List<String>> campos = formulario(texto);
		webhook(campos, AssinaturaDoTwilio.assinar(URL_DO_WEBHOOK, campos, TOKEN)).andExpect(status().isOk());
	}

	private Conversa conversaDoCliente() {
		return transacao.execute(status -> {
			Conversa conversa = em.createQuery("select v from Conversa v where v.cliente.empresa.nome = :nome", Conversa.class)
					.setParameter("nome", EMPRESA).getSingleResult();
			conversa.getMensagens().size();
			conversa.getCliente().getTelefone();
			return conversa;
		});
	}

	private int mensagensNoBanco(UUID conversa) {
		return transacao.execute(status -> em.find(Conversa.class, conversa).getMensagens().size());
	}

	private UUID conversaEmAtendimento() throws Exception {
		clienteEscreve("Quero falar com um atendente");
		verify(twilio, timeout(10_000)).enviarTexto(eq(DE), contains("atendente"));
		UUID conversa = conversaDoCliente().getId();
		assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversaDoCliente().getStatus());
		mvc.perform(post("/api/fila/{id}/assumir", conversa).with(csrf()).with(comoAtendente(atendenteId)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"atendenteId\":\"" + atendenteId + "\"}")).andExpect(status().isNoContent());
		clearInvocations(twilio);
		return conversa;
	}

	private ResultActions atendenteResponde(UUID conversa, String texto) throws Exception {
		return mvc.perform(post("/api/conversas/{id}/atendimento/mensagens", conversa).with(csrf()).with(comoAtendente(atendenteId)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"" + texto + "\"}"));
	}

	@Test
	void mensagemDoWhatsAppViraConversaDoClienteERespostaVoltaPeloTwilio() throws Exception {
		clienteEscreve("Oi, tudo bem?");

		verify(twilio, timeout(10_000)).enviarTexto(eq(DE), contains("Oi, tudo bem?"));
		Conversa conversa = conversaDoCliente();
		assertEquals(OrigemConversa.WHATSAPP, conversa.getOrigem());
		assertEquals(TELEFONE_NO_CADASTRO, conversa.getCliente().getTelefone());
		assertEquals("Cliente do Twilio", conversa.getCliente().getNome());
		assertEquals(2, conversa.getMensagens().size());
	}

	@Test
	void chamadaComAssinaturaInvalidaNaoGeraNada() throws Exception {
		Map<String, List<String>> campos = formulario("Oi");

		webhook(campos, "assinatura-errada").andExpect(status().isForbidden());
		webhook(campos, AssinaturaDoTwilio.assinar(URL_DO_WEBHOOK, campos, "outro-token")).andExpect(status().isForbidden());

		verify(twilio, after(500).never()).enviarTexto(anyString(), anyString());
		long conversas = transacao.execute(status -> em
				.createQuery("select count(v) from Conversa v where v.cliente.empresa.nome = :nome", Long.class)
				.setParameter("nome", EMPRESA).getSingleResult());
		assertEquals(0L, conversas);
	}

	@Test
	void respostaDoAtendenteNoDashboardChegaAoClienteNoWhatsApp() throws Exception {
		UUID conversa = conversaEmAtendimento();

		atendenteResponde(conversa, "Oi, sou o Carlos. Como posso ajudar?")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entregue").value(true))
				.andExpect(jsonPath("$.aviso").doesNotExist())
				.andExpect(jsonPath("$.mensagem.tipoRemetente").value("ATENDENTE"));

		verify(twilio).enviarTexto("whatsapp:+5581977770003", "Oi, sou o Carlos. Como posso ajudar?");
		assertEquals(3, mensagensNoBanco(conversa));
	}

	@Test
	void foraDaJanelaDe24HorasAMensagemFicaSalvaEOAtendenteRecebeOAviso() throws Exception {
		UUID conversa = conversaEmAtendimento();
		doThrow(new FalhaNoTwilioException("Outside messaging window", 400, 63016)).when(twilio)
				.enviarTexto(eq("whatsapp:+5581977770003"), eq("Está aí?"));

		atendenteResponde(conversa, "Está aí?")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entregue").value(false))
				.andExpect(jsonPath("$.aviso").value(containsString("24 horas")));

		assertEquals(3, mensagensNoBanco(conversa));
	}

	@Test
	void respostaDoClienteDuranteOAtendimentoFicaNoHistoricoESemRespostaDaLily() throws Exception {
		UUID conversa = conversaEmAtendimento();

		clienteEscreve("É o WXN-2026-0001");

		for (int i = 0; i < 100 && mensagensNoBanco(conversa) < 3; i++) {
			Thread.sleep(100);
		}
		assertEquals(3, mensagensNoBanco(conversa));
		verify(twilio, after(700).never()).enviarTexto(anyString(), anyString());
		assertTrue(conversaDoCliente().getStatus() == StatusConversa.COM_ATENDENTE);
	}
}
