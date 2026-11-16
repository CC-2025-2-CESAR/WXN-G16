package br.com.wxnchatbot.backend.canal.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class AtualizacaoDoTelegramTest {

	private final JsonMapper json = JsonMapper.builder().build();

	private static final String MENSAGEM_PRIVADA = """
			{"update_id":10000,"message":{"message_id":1365,"date":1441645532,
			"from":{"id":1111111,"is_bot":false,"first_name":"Maria","last_name":"Souza","username":"mariasouza","language_code":"pt-br"},
			"chat":{"id":1111111,"first_name":"Maria","last_name":"Souza","username":"mariasouza","type":"private"},
			"text":"Qual o status da WXN-2026-0001?","entities":[]}}""";

	@Test
	void leOsCamposQueALilyUsaDeUmaMensagemDeTexto() {
		AtualizacaoDoTelegram atualizacao = json.readValue(MENSAGEM_PRIVADA, AtualizacaoDoTelegram.class);

		assertEquals(10000, atualizacao.updateId());
		assertEquals(1365, atualizacao.message().messageId());
		assertEquals("Qual o status da WXN-2026-0001?", atualizacao.message().text());
		assertEquals(1111111, atualizacao.message().chat().id());
		assertTrue(atualizacao.message().chat().privado());
		assertEquals(1111111, atualizacao.message().from().id());
		assertFalse(atualizacao.message().from().bot());
		assertEquals("Maria Souza", atualizacao.message().from().nomeCompleto());
	}

	@Test
	void grupoEOutrosTiposDeChatNaoSaoPrivados() {
		for (String tipo : new String[] { "group", "supergroup", "channel" }) {
			AtualizacaoDoTelegram atualizacao = json.readValue(
					"{\"update_id\":1,\"message\":{\"message_id\":2,\"chat\":{\"id\":-100123,\"type\":\"" + tipo + "\",\"title\":\"Time\"},\"text\":\"oi\"}}",
					AtualizacaoDoTelegram.class);

			assertFalse(atualizacao.message().chat().privado(), tipo);
		}
	}

	@Test
	void atualizacaoSemMensagemFicaComMensagemNula() {
		AtualizacaoDoTelegram atualizacao = json.readValue(
				"{\"update_id\":7,\"edited_message\":{\"message_id\":2,\"text\":\"editada\"}}", AtualizacaoDoTelegram.class);

		assertEquals(7, atualizacao.updateId());
		assertNull(atualizacao.message());
	}

	@Test
	void mensagemSemTextoTemTextoNulo() {
		AtualizacaoDoTelegram atualizacao = json.readValue(
				"{\"update_id\":8,\"message\":{\"message_id\":3,\"chat\":{\"id\":5,\"type\":\"private\"},\"photo\":[{\"file_id\":\"x\"}]}}",
				AtualizacaoDoTelegram.class);

		assertNull(atualizacao.message().text());
	}

	@Test
	void ignoraCamposQueANossaClasseNaoConhece() {
		AtualizacaoDoTelegram atualizacao = json.readValue(
				"{\"update_id\":9,\"campo_novo\":{\"a\":1},\"message\":{\"message_id\":3,\"novo\":true,\"chat\":{\"id\":5,\"type\":\"private\",\"x\":1},\"text\":\"oi\"}}",
				AtualizacaoDoTelegram.class);

		assertEquals("oi", atualizacao.message().text());
	}

	@Test
	void nomeCompletoJuntaOsNomesELidaComFaltas() {
		assertEquals("Maria", new AtualizacaoDoTelegram.UsuarioDoTelegram(1, false, "Maria", null, null).nomeCompleto());
		assertEquals("Maria Souza", new AtualizacaoDoTelegram.UsuarioDoTelegram(1, false, " Maria ", " Souza ", "m").nomeCompleto());
		assertNull(new AtualizacaoDoTelegram.UsuarioDoTelegram(1, false, null, null, "so_username").nomeCompleto());
		assertNull(new AtualizacaoDoTelegram.UsuarioDoTelegram(1, false, " ", " ", null).nomeCompleto());
	}
}
