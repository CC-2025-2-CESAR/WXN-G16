package br.com.wxnchatbot.experimento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExecutorDoExperimentoTest {

	@Test
	void descreveASimplesMensagemQuandoNaoHaCausa() {
		assertEquals("IllegalStateException: falhou", ExecutorDoExperimento.descrever(new IllegalStateException("falhou")));
	}

	@Test
	void incluiACausaRaizQuandoExiste() {
		Exception raiz = new IllegalArgumentException("API key not valid");
		Exception meio = new IllegalStateException("meio", raiz);
		Exception topo = new RuntimeException("Failed to generate content", meio);

		assertEquals("RuntimeException: Failed to generate content (causa: IllegalArgumentException: API key not valid)",
				ExecutorDoExperimento.descrever(topo));
	}

	@Test
	void abreviaMensagensMuitoLongasEQuebrasDeLinha() {
		String descricao = ExecutorDoExperimento.descrever(new RuntimeException("a\nb ".repeat(200)));

		assertTrue(descricao.length() < 330, "tamanho " + descricao.length());
		assertTrue(!descricao.contains("\n"));
	}
}
