package br.com.wxnchatbot.backend.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;

class LlmClientFakeTest {

	private final LlmClientFake cliente = new LlmClientFake();

	@Test
	void respondeComOTextoDaPerguntaAtual() {
		List<MensagemLlm> conversa = List.of(new MensagemLlm(Papel.USUARIO, "Quero saber do meu chamado"));

		String resposta = cliente.responder(PromptDaLily.TEXTO, conversa);

		assertTrue(resposta.contains("Quero saber do meu chamado"));
	}

	@Test
	void usaAUltimaMensagemQuandoHaHistorico() {
		List<MensagemLlm> conversa = List.of(
				new MensagemLlm(Papel.USUARIO, "Oi"),
				new MensagemLlm(Papel.ASSISTENTE, "Olá, como posso ajudar?"),
				new MensagemLlm(Papel.USUARIO, "Preciso de ajuda com a nota fiscal"));

		String resposta = cliente.responder(PromptDaLily.TEXTO, conversa);

		assertTrue(resposta.contains("Preciso de ajuda com a nota fiscal"));
		assertFalse(resposta.contains("como posso ajudar"));
	}

	@Test
	void conversaVaziaLancaIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> cliente.responder(PromptDaLily.TEXTO, List.of()));
	}
}