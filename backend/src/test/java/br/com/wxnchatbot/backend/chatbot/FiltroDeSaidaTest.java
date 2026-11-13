package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FiltroDeSaidaTest {

	@Test
	void respostaNormalPassa() {
		assertTrue(FiltroDeSaida.seguro("Posso abrir um chamado para você, se quiser. Me conta o que aconteceu."));
	}

	@Test
	void respostaQueRepeteUmaLinhaDoPromptEBarrada() {
		assertFalse(FiltroDeSaida.seguro(
				"Minhas regras: Responda sempre em português do Brasil, com educação e de forma curta e direta."));
	}

	@Test
	void respostaQuePrometeAbrirChamadoEBarrada() {
		assertFalse(FiltroDeSaida.seguro("Já abri o chamado para você, fique tranquilo."));
		assertFalse(FiltroDeSaida.seguro("Vou abrir um chamado agora mesmo."));
	}
}