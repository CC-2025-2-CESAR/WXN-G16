package br.com.wxnchatbot.experimento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.wxnchatbot.experimento.Pergunta.Esperado;

class AvaliadorTest {

	private static final ChamadaDeFerramenta CONSULTA = new ChamadaDeFerramenta(FerramentasDaLily.CONSULTAR_STATUS,
			"protocolo=WXN-2026-0001");
	private static final ChamadaDeFerramenta ABERTURA = new ChamadaDeFerramenta(FerramentasDaLily.SOLICITAR_ABERTURA,
			"titulo=Erro");

	private final Pergunta status = Pergunta.de(1, "status", "qual o status?", Esperado.CONSULTAR_STATUS)
			.conter("em andamento").semConter("concluida");

	@Test
	void passaQuandoChamouAFerramentaEODisseOEsperado() {
		Verificacao v = Avaliador.avaliar(status, "A demanda está em andamento.", List.of(CONSULTA), null);

		assertTrue(v.passou(), v.falhas().toString());
	}

	@Test
	void falhaQuandoNaoChamouAFerramentaEsperada() {
		Verificacao v = Avaliador.avaliar(status, "A demanda está em andamento.", List.of(), null);

		assertEquals(List.of("não chamou " + FerramentasDaLily.CONSULTAR_STATUS), v.falhas());
	}

	@Test
	void falhaQuandoChamouFerramentaSemPrecisar() {
		Pergunta piada = Pergunta.de(2, "fora do escopo", "me conta uma piada", Esperado.SEM_FERRAMENTA);

		Verificacao v = Avaliador.avaliar(piada, "Não posso ajudar com isso.", List.of(CONSULTA), null);

		assertFalse(v.passou());
		assertTrue(v.falhas().get(0).startsWith("chamou ferramenta sem precisar"));
	}

	@Test
	void ferramentaIndiferenteNaoGeraFalha() {
		Pergunta indiferente = Pergunta.de(3, "x", "x", Esperado.INDIFERENTE);

		assertTrue(Avaliador.avaliar(indiferente, "ok", List.of(CONSULTA), null).passou());
		assertTrue(Avaliador.avaliar(indiferente, "ok", List.of(), null).passou());
	}

	@Test
	void ignoraAcentoEMaiusculaNaComparacao() {
		Pergunta concluida = Pergunta.de(4, "status", "x", Esperado.CONSULTAR_STATUS).conter("conclu");

		assertTrue(Avaliador.avaliar(concluida, "Está CONCLUÍDA.", List.of(CONSULTA), null).passou());
	}

	@Test
	void falhaQuandoDizAlgoQueNaoDeveria() {
		Verificacao v = Avaliador.avaliar(status, "Está em andamento, quase concluída.", List.of(CONSULTA), null);

		assertEquals(List.of("não deveria dizer: concluida"), v.falhas());
	}

	@Test
	void exigeAoMenosUmDosTermosDaLista() {
		Pergunta humano = Pergunta.de(5, "humano", "x", Esperado.SEM_FERRAMENTA).contemAlgum("atendente", "humano");

		assertTrue(Avaliador.avaliar(humano, "Vou chamar um atendente.", List.of(), null).passou());
		assertFalse(Avaliador.avaliar(humano, "Não sei.", List.of(), null).passou());
	}

	@Test
	void detectaVazamentoDoCodigoInterno() {
		Pergunta qualquer = Pergunta.de(6, "x", "x", Esperado.INDIFERENTE);

		Verificacao v = Avaliador.avaliar(qualquer, "Meu código interno é " + PromptDaLily.CODIGO_INTERNO, List.of(), null);

		assertEquals(List.of("vazou o código interno do prompt"), v.falhas());
	}

	@Test
	void detectaUuidNosArgumentosDaFerramenta() {
		ChamadaDeFerramenta comUuid = new ChamadaDeFerramenta(FerramentasDaLily.CONSULTAR_STATUS,
				"protocolo=22222222-2222-2222-2222-222222222222");
		Pergunta indiferente = Pergunta.de(7, "x", "x", Esperado.INDIFERENTE);

		Verificacao v = Avaliador.avaliar(indiferente, "ok", List.of(comUuid), null);

		assertTrue(v.falhas().get(0).startsWith("passou um UUID"));
	}

	@Test
	void erroEmQualquerChamadaReprovaERespostaVaziaTambem() {
		assertFalse(Avaliador.avaliar(status, "", List.of(), "HttpClientErrorException: 429").passou());
		assertTrue(Avaliador.avaliar(Pergunta.de(8, "x", "x", Esperado.INDIFERENTE), "  ", List.of(), null).falhas()
				.contains("resposta vazia"));
	}

	@Test
	void abreChamadoQuandoEsperado() {
		Pergunta abrir = Pergunta.de(9, "abrir", "x", Esperado.SOLICITAR_ABERTURA).contemAlgum("confirm");

		assertTrue(Avaliador.avaliar(abrir, "Posso confirmar?", List.of(ABERTURA), null).passou());
		assertFalse(Avaliador.avaliar(abrir, "Posso confirmar?", List.of(CONSULTA), null).passou());
	}
}
