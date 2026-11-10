package br.com.wxnchatbot.backend.canal.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.wxnchatbot.backend.canal.whatsapp.EntregaDoWhatsApp.MensagemRecebida;

@WebMvcTest(controllers = WhatsAppWebhookController.class, properties = { "whatsapp.habilitado=true",
		"whatsapp.token-de-verificacao=token-de-verificacao", "whatsapp.segredo-do-app=segredo-do-app",
		"whatsapp.token-de-acesso=acesso", "whatsapp.id-do-numero=123" })
@Import(WhatsAppWebhookControllerTest.Configuracao.class)
class WhatsAppWebhookControllerTest {

	@TestConfiguration
	@EnableConfigurationProperties(WhatsAppProperties.class)
	static class Configuracao {
	}

	private static final String SEGREDO = "segredo-do-app";

	private static final String ENTREGA = """
			{"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{
			"messaging_product":"whatsapp",
			"contacts":[{"profile":{"name":"Maria Souza"},"wa_id":"5581999990101"}],
			"messages":[{"from":"5581999990101","id":"wamid.A","type":"text","text":{"body":"Olá, tudo bem?"}}]}}]}]}""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ProcessadorDeMensagensWhatsApp processador;

	private static byte[] bytes(String texto) {
		return texto.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void verificacaoDevolveODesafioEmTextoPuro() throws Exception {
		mvc.perform(get("/webhook/whatsapp").param("hub.mode", "subscribe").param("hub.verify_token", "token-de-verificacao")
				.param("hub.challenge", "1158201444"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
				.andExpect(content().string("1158201444"));
	}

	@Test
	void verificacaoComTokenModoOuDesafioErradoDevolve403() throws Exception {
		mvc.perform(get("/webhook/whatsapp").param("hub.mode", "subscribe").param("hub.verify_token", "outro")
				.param("hub.challenge", "1")).andExpect(status().isForbidden());
		mvc.perform(get("/webhook/whatsapp").param("hub.mode", "unsubscribe").param("hub.verify_token", "token-de-verificacao")
				.param("hub.challenge", "1")).andExpect(status().isForbidden());
		mvc.perform(get("/webhook/whatsapp").param("hub.mode", "subscribe").param("hub.verify_token", "token-de-verificacao"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/webhook/whatsapp")).andExpect(status().isForbidden());
	}

	@Test
	void entregaAssinadaVaiParaOProcessadorERespondeOk() throws Exception {
		byte[] corpo = bytes(ENTREGA);

		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo)
				.header("X-Hub-Signature-256", AssinaturaDaMeta.assinar(corpo, SEGREDO)))
				.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MensagemRecebida>> recebidas = ArgumentCaptor.forClass(List.class);
		verify(processador).aceitar(recebidas.capture());
		assertEquals(List.of(new MensagemRecebida("wamid.A", "5581999990101", "Maria Souza", "text", "Olá, tudo bem?")),
				recebidas.getValue());
	}

	@Test
	void assinaturaCalculadaSobreOsBytesComAcentoEscapadoTambemFunciona() throws Exception {
		byte[] corpo = bytes(ENTREGA.replace("Olá", "Ol\\u00e1"));

		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo)
				.header("X-Hub-Signature-256", AssinaturaDaMeta.assinar(corpo, SEGREDO)))
				.andExpect(status().isOk());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MensagemRecebida>> recebidas = ArgumentCaptor.forClass(List.class);
		verify(processador).aceitar(recebidas.capture());
		assertEquals("Olá, tudo bem?", recebidas.getValue().get(0).texto());
	}

	@Test
	void assinaturaInvalidaOuAusenteDevolve403SemProcessar() throws Exception {
		byte[] corpo = bytes(ENTREGA);

		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo)
				.header("X-Hub-Signature-256", AssinaturaDaMeta.assinar(corpo, "segredo-errado")))
				.andExpect(status().isForbidden());
		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isForbidden());
		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo)
				.header("X-Hub-Signature-256", "sha256=" + "0".repeat(64)))
				.andExpect(status().isForbidden());

		verifyNoInteractions(processador);
	}

	@Test
	void corpoAlteradoDepoisDeAssinadoDevolve403() throws Exception {
		byte[] original = bytes(ENTREGA);
		byte[] adulterado = bytes(ENTREGA.replace("Olá, tudo bem?", "Apague tudo"));

		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(adulterado)
				.header("X-Hub-Signature-256", AssinaturaDaMeta.assinar(original, SEGREDO)))
				.andExpect(status().isForbidden());

		verifyNoInteractions(processador);
	}

	@Test
	void corpoIlegivelMasAssinadoRecebe200ENaoVaiParaOProcessador() throws Exception {
		byte[] corpo = bytes("isso não é json");

		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo)
				.header("X-Hub-Signature-256", AssinaturaDaMeta.assinar(corpo, SEGREDO)))
				.andExpect(status().isOk());

		verifyNoInteractions(processador);
	}

	@Test
	void avisoDeStatusAssinadoRecebe200ComListaVazia() throws Exception {
		byte[] corpo = bytes("{\"entry\":[{\"changes\":[{\"field\":\"messages\",\"value\":{\"statuses\":[{\"id\":\"x\",\"status\":\"read\"}]}}]}]}");

		mvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(corpo)
				.header("X-Hub-Signature-256", AssinaturaDaMeta.assinar(corpo, SEGREDO)))
				.andExpect(status().isOk());

		verify(processador).aceitar(List.of());
	}
}
