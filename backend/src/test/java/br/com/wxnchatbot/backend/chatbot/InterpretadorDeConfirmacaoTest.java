package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.wxnchatbot.backend.chatbot.InterpretadorDeConfirmacao.Resposta;

class InterpretadorDeConfirmacaoTest {

	private final InterpretadorDeConfirmacao interpretador = new InterpretadorDeConfirmacao();

	@ParameterizedTest
	@ValueSource(strings = { "sim", "Sim!", "S", "pode", "Pode sim", "pode abrir", "sim, por favor", "isso mesmo", "ok", "Confirmo",
			"claro, pode abrir sim" })
	void concordanciaClaraConfirma(String mensagem) {
		assertEquals(Resposta.SIM, interpretador.interpretar(mensagem));
	}

	@ParameterizedTest
	@ValueSource(strings = { "não", "Não, obrigado", "nao quero", "cancela", "deixa pra lá", "melhor não", "sim, mas não",
			"não pode abrir" })
	void qualquerNegacaoCancela(String mensagem) {
		assertEquals(Resposta.NAO, interpretador.interpretar(mensagem));
	}

	@ParameterizedTest
	@ValueSource(strings = { "ok mas qual o prazo?", "sim e também preciso de outra coisa", "talvez", "o relatório continua errado",
			"quanto tempo demora?", "" })
	void maisQueUmSimOuUmNaoNaoConfirmaNada(String mensagem) {
		assertEquals(Resposta.INDEFINIDA, interpretador.interpretar(mensagem));
	}

	@Test
	void mensagemNulaNaoConfirma() {
		assertEquals(Resposta.INDEFINIDA, interpretador.interpretar(null));
	}
}
