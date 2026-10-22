package br.com.wxnchatbot.experimento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.wxnchatbot.experimento.Pergunta.Esperado;

class RelatorioTest {

	private static final LocalDateTime DATA = LocalDateTime.of(2026, 10, 22, 15, 30);

	private final Pergunta pergunta = Pergunta.de(1, "status", "Como está o WXN-2026-0001?", Esperado.CONSULTAR_STATUS);
	private final ChamadaDeFerramenta consulta = new ChamadaDeFerramenta(FerramentasDaLily.CONSULTAR_STATUS,
			"protocolo=WXN-2026-0001");

	private Resultado ok(String modelo, long ms, String resposta) {
		return new Resultado(modelo, pergunta, resposta, List.of(consulta), ms, 100, 20, null,
				new Verificacao(List.of()));
	}

	private Resultado comErro(String modelo) {
		return new Resultado(modelo, pergunta, "", List.of(), 50, null, null, "HttpClientErrorException: 429",
				new Verificacao(List.of("erro na chamada: HttpClientErrorException: 429")));
	}

	@Test
	void percentilUsaPosicaoMaisProxima() {
		List<Long> valores = List.of(100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L);

		assertEquals(500, Relatorio.percentil(valores, 50));
		assertEquals(1000, Relatorio.percentil(valores, 95));
		assertEquals(1000, Relatorio.percentil(valores, 100));
		assertEquals(0, Relatorio.percentil(List.of(), 50));
	}

	@Test
	void resumoContaErrosAcertosELatencia() {
		String md = Relatorio.montar(DATA, "LOW",
				List.of(ok("modelo-a", 1000, "Em andamento."), comErro("modelo-a"), ok("modelo-b", 300, "Em andamento.")));

		assertTrue(md.contains("| modelo-a | 1/2 | 1/2 | 1/2 | 1000 ms |"), md);
		assertTrue(md.contains("| modelo-b | 0/1 | 1/1 | 1/1 | 300 ms |"), md);
		assertTrue(md.contains("Nível de pensamento: LOW"));
		assertTrue(md.contains("erro na chamada: HttpClientErrorException: 429"));
	}

	@Test
	void semFalhasDizNenhuma() {
		String md = Relatorio.montar(DATA, "LOW", List.of(ok("modelo-a", 10, "ok")));

		assertTrue(md.contains("## Falhas nas verificações automáticas\n\nNenhuma."));
	}

	@Test
	void respostaComBarraEQuebraDeLinhaNaoQuebraATabela() {
		String md = Relatorio.montar(DATA, "LOW", List.of(ok("modelo-a", 10, "linha 1\nlinha 2 | com barra")));

		assertTrue(md.contains("| linha 1 linha 2 \\| com barra |"), md);
	}

	@Test
	void gravaOArquivoComODataNoNome(@TempDir Path pasta) throws IOException {
		Path arquivo = Relatorio.gravar(pasta.resolve("resultados"), DATA, "conteudo");

		assertEquals("gemini-20261022-1530.md", arquivo.getFileName().toString());
		assertEquals("conteudo", Files.readString(arquivo));
	}
}
