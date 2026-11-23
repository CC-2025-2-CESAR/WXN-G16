package br.com.wxnchatbot.backend.canal.twilio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = TwilioWebhookController.class, properties = { "twilio.habilitado=true",
		"twilio.account-sid=AC_SID_FALSO_PARA_TESTES", "twilio.auth-token=token-secreto-do-twilio",
		"twilio.numero-de-origem=+14155238886",
		"twilio.url-do-webhook=https://wxn-g16-backend.azurewebsites.net/webhook/twilio/whatsapp" })
@Import(TwilioWebhookControllerTest.Configuracao.class)
class TwilioWebhookControllerTest {

	@TestConfiguration
	@EnableConfigurationProperties(TwilioProperties.class)
	static class Configuracao {
	}

	private static final String URL = "https://wxn-g16-backend.azurewebsites.net/webhook/twilio/whatsapp";
	private static final String TOKEN = "token-secreto-do-twilio";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ProcessadorDeMensagensTwilio processador;

	private static Map<String, List<String>> formulario() {
		Map<String, List<String>> campos = new LinkedHashMap<>();
		campos.put("MessageSid", List.of("SM_MENSAGEM_FALSA_PARA_TESTES"));
		campos.put("From", List.of("whatsapp:+5581999990001"));
		campos.put("To", List.of("whatsapp:+14155238886"));
		campos.put("Body", List.of("Olá, tudo bem?"));
		campos.put("ProfileName", List.of("Maria Souza"));
		campos.put("NumMedia", List.of("0"));
		return campos;
	}

	private static MockHttpServletRequestBuilder requisicao(Map<String, List<String>> campos, String assinatura) {
		MockHttpServletRequestBuilder pedido = post("/webhook/twilio/whatsapp").contentType(MediaType.APPLICATION_FORM_URLENCODED);
		campos.forEach((nome, valores) -> pedido.param(nome, valores.toArray(new String[0])));
		if (assinatura != null) {
			pedido.header("X-Twilio-Signature", assinatura);
		}
		return pedido;
	}

	@Test
	void comAAssinaturaCertaEntregaAMensagemAoProcessadorEDevolveUmTwimlVazio() throws Exception {
		Map<String, List<String>> campos = formulario();

		mvc.perform(requisicao(campos, AssinaturaDoTwilio.assinar(URL, campos, TOKEN)))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
				.andExpect(content().string("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response/>"));

		ArgumentCaptor<MensagemDoTwilio> entregue = ArgumentCaptor.forClass(MensagemDoTwilio.class);
		verify(processador).aceitar(entregue.capture());
		assertEquals(new MensagemDoTwilio("SM_MENSAGEM_FALSA_PARA_TESTES", "whatsapp:+5581999990001", "Maria Souza",
				"Olá, tudo bem?"), entregue.getValue());
	}

	@Test
	void semAssinaturaOuComAssinaturaErradaDevolve403SemProcessar() throws Exception {
		Map<String, List<String>> campos = formulario();

		mvc.perform(requisicao(campos, null)).andExpect(status().isForbidden());
		mvc.perform(requisicao(campos, "")).andExpect(status().isForbidden());
		mvc.perform(requisicao(campos, "assinatura-errada")).andExpect(status().isForbidden());
		mvc.perform(requisicao(campos, AssinaturaDoTwilio.assinar(URL, campos, "outro-token"))).andExpect(status().isForbidden());

		verifyNoInteractions(processador);
	}

	@Test
	void mensagemAdulteradaDepoisDeAssinadaEhRecusada() throws Exception {
		Map<String, List<String>> assinada = formulario();
		String assinatura = AssinaturaDoTwilio.assinar(URL, assinada, TOKEN);
		Map<String, List<String>> adulterada = formulario();
		adulterada.put("Body", List.of("Outra mensagem"));

		mvc.perform(requisicao(adulterada, assinatura)).andExpect(status().isForbidden());

		verifyNoInteractions(processador);
	}

	@Test
	void soAceitaPostDeFormulario() throws Exception {
		mvc.perform(get("/webhook/twilio/whatsapp")).andExpect(status().isMethodNotAllowed());
		mvc.perform(post("/webhook/twilio/whatsapp").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnsupportedMediaType());

		verifyNoInteractions(processador);
	}
}
