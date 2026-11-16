package br.com.wxnchatbot.backend.canal.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

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

@WebMvcTest(controllers = TelegramWebhookController.class, properties = { "telegram.habilitado=true",
		"telegram.modo=webhook", "telegram.token-do-bot=123:ABC", "telegram.segredo-do-webhook=segredo_do-webhook" })
@Import(TelegramWebhookControllerTest.Configuracao.class)
class TelegramWebhookControllerTest {

	@TestConfiguration
	@EnableConfigurationProperties(TelegramProperties.class)
	static class Configuracao {
	}

	private static final String SEGREDO = "segredo_do-webhook";

	private static final String ATUALIZACAO = """
			{"update_id":10000,"message":{"message_id":1,"from":{"id":1111111,"is_bot":false,"first_name":"Maria"},
			"chat":{"id":1111111,"type":"private"},"date":1441645532,"text":"Olá, tudo bem?"}}""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ProcessadorDeAtualizacoesTelegram processador;

	private static byte[] bytes(String texto) {
		return texto.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void comOSegredoCertoEntregaAAtualizacaoAoProcessadorEDevolve200() throws Exception {
		mvc.perform(post("/webhook/telegram").contentType(MediaType.APPLICATION_JSON).content(bytes(ATUALIZACAO))
				.header("X-Telegram-Bot-Api-Secret-Token", SEGREDO)).andExpect(status().isOk());

		ArgumentCaptor<AtualizacaoDoTelegram> entregue = ArgumentCaptor.forClass(AtualizacaoDoTelegram.class);
		verify(processador).aceitar(entregue.capture());
		assertEquals(10000, entregue.getValue().updateId());
		assertEquals("Olá, tudo bem?", entregue.getValue().message().text());
	}

	@Test
	void semSegredoOuComSegredoErradoDevolve403SemProcessar() throws Exception {
		mvc.perform(post("/webhook/telegram").contentType(MediaType.APPLICATION_JSON).content(bytes(ATUALIZACAO)))
				.andExpect(status().isForbidden());
		mvc.perform(post("/webhook/telegram").contentType(MediaType.APPLICATION_JSON).content(bytes(ATUALIZACAO))
				.header("X-Telegram-Bot-Api-Secret-Token", "outro")).andExpect(status().isForbidden());
		mvc.perform(post("/webhook/telegram").contentType(MediaType.APPLICATION_JSON).content(bytes(ATUALIZACAO))
				.header("X-Telegram-Bot-Api-Secret-Token", "")).andExpect(status().isForbidden());

		verifyNoInteractions(processador);
	}

	@Test
	void corpoIlegivelComOSegredoCertoRecebe200SemProcessar() throws Exception {
		mvc.perform(post("/webhook/telegram").contentType(MediaType.APPLICATION_JSON).content(bytes("isso nao e json"))
				.header("X-Telegram-Bot-Api-Secret-Token", SEGREDO)).andExpect(status().isOk());

		verifyNoInteractions(processador);
	}

	@Test
	void soAceitaPostNoWebhook() throws Exception {
		mvc.perform(get("/webhook/telegram")).andExpect(status().isMethodNotAllowed());
	}
}
