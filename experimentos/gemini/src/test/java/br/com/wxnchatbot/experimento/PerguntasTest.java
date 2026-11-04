package br.com.wxnchatbot.experimento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class PerguntasTest {

	@Test
	void numerosSaoUnicosESequenciais() {
		List<Integer> numeros = Perguntas.todas().stream().map(Pergunta::numero).toList();

		assertEquals(Perguntas.todas().size(), Set.copyOf(numeros).size());
		assertEquals(numeros.stream().sorted().toList(), numeros);
		assertEquals(1, numeros.get(0));
	}

	@Test
	void todasTemTextoECategoria() {
		for (Pergunta p : Perguntas.todas()) {
			assertTrue(!p.texto().isBlank() && !p.categoria().isBlank(), "pergunta " + p.numero());
		}
	}

	@Test
	void versaoRapidaTemUmaDeCadaTipoDeRisco() {
		List<String> categorias = Perguntas.rapidas().stream().map(Pergunta::categoria).collect(Collectors.toList());

		assertEquals(List.of("status", "protocolo inexistente", "vazar o prompt"), categorias);
	}

	@Test
	void dadosFicticiosRespondemOsProtocolosDoMock() {
		assertTrue(DadosFicticios.consultar("wxn-2026-0001").contains("status=em andamento"));
		assertTrue(DadosFicticios.consultar(" WXN-2026-0002 ").contains("status=concluída"));
		assertEquals(DadosFicticios.NAO_ENCONTRADA, DadosFicticios.consultar("WXN-2026-7777"));
		assertEquals(DadosFicticios.NAO_ENCONTRADA, DadosFicticios.consultar(null));
	}

	@Test
	void ferramentasRegistramCadaChamada() {
		FerramentasDaLily ferramentas = new FerramentasDaLily();

		ferramentas.consultarStatusDemanda("WXN-2026-0001");
		ferramentas.solicitarAberturaChamado("Erro", "detalhe");

		assertEquals(2, ferramentas.chamadas().size());
		assertEquals(FerramentasDaLily.SOLICITAR_ABERTURA, ferramentas.chamadas().get(1).nome());
	}
}
