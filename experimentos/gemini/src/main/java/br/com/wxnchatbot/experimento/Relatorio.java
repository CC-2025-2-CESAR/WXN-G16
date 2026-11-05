package br.com.wxnchatbot.experimento;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Relatorio {

	private static final int TAMANHO_NA_TABELA = 220;

	private Relatorio() {
	}

	public static String montar(LocalDateTime data, String nivelDePensamento, List<Resultado> resultados) {
		Map<String, List<Resultado>> porModelo = new LinkedHashMap<>();
		for (Resultado r : resultados) {
			porModelo.computeIfAbsent(r.modelo(), m -> new ArrayList<>()).add(r);
		}
		StringBuilder md = new StringBuilder();
		md.append("# Experimento com a Gemini\n\n");
		md.append("- Data: ").append(data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
		md.append("- Nível de pensamento: ").append(nivelDePensamento).append("\n");
		md.append("- Perguntas por modelo: ").append(porModelo.values().stream().findFirst().map(List::size).orElse(0))
				.append("\n");
		md.append("- Dados fictícios. As verificações automáticas são heurísticas (palavras esperadas, ferramenta ")
				.append("chamada, vazamento do código interno); a avaliação da qualidade do português e do tom é humana.\n\n");

		md.append("## Resumo por modelo\n\n");
		md.append("| Modelo | Erros | Passou nas verificações | Ferramenta certa | Latência p50 | p95 | máxima | Tokens médios (entrada/saída) |\n");
		md.append("|---|---|---|---|---|---|---|---|\n");
		porModelo.forEach((modelo, lista) -> md.append(linhaDeResumo(modelo, lista)));

		md.append("\n## Falhas nas verificações automáticas\n\n");
		List<Resultado> falhas = resultados.stream().filter(r -> !r.verificacao().passou()).toList();
		if (falhas.isEmpty()) {
			md.append("Nenhuma.\n");
		} else {
			md.append("| Modelo | # | Pergunta | O que falhou |\n|---|---|---|---|\n");
			for (Resultado r : falhas) {
				md.append("| ").append(r.modelo()).append(" | ").append(r.pergunta().numero()).append(" | ")
						.append(celula(r.pergunta().texto())).append(" | ")
						.append(celula(String.join("; ", r.verificacao().falhas()))).append(" |\n");
			}
		}

		md.append("\n## Resultado por pergunta\n\n");
		md.append("| # | Categoria | Modelo | Ferramentas | Latência | Verificação | Resposta |\n|---|---|---|---|---|---|---|\n");
		for (Resultado r : resultados) {
			md.append("| ").append(r.pergunta().numero()).append(" | ").append(r.pergunta().categoria()).append(" | ")
					.append(r.modelo()).append(" | ").append(celula(ferramentas(r))).append(" | ")
					.append(r.latenciaMs()).append(" ms | ").append(r.verificacao().passou() ? "ok" : "falhou")
					.append(" | ").append(celula(r.erro() != null ? "ERRO: " + r.erro() : r.resposta())).append(" |\n");
		}

		md.append("\n## Respostas completas\n");
		for (Resultado r : resultados) {
			md.append("\n### ").append(r.modelo()).append(" - #").append(r.pergunta().numero()).append(" ")
					.append(r.pergunta().categoria()).append("\n\n");
			md.append("Pergunta: ").append(r.pergunta().texto()).append("\n\n");
			md.append("Ferramentas: ").append(ferramentas(r)).append("\n\n");
			md.append(r.erro() != null ? "Erro: " + r.erro() : "Resposta: " + r.resposta()).append("\n");
		}
		return md.toString();
	}

	public static Path gravar(Path pasta, LocalDateTime data, String conteudo) throws IOException {
		Files.createDirectories(pasta);
		Path arquivo = pasta.resolve("gemini-" + data.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".md");
		Files.writeString(arquivo, conteudo);
		return arquivo;
	}

	static long percentil(List<Long> valores, int p) {
		if (valores.isEmpty()) {
			return 0;
		}
		List<Long> ordenados = valores.stream().sorted().toList();
		int indice = (int) Math.ceil(p / 100.0 * ordenados.size()) - 1;
		return ordenados.get(Math.max(0, Math.min(indice, ordenados.size() - 1)));
	}

	private static String linhaDeResumo(String modelo, List<Resultado> lista) {
		long erros = lista.stream().filter(r -> r.erro() != null).count();
		long passaram = lista.stream().filter(r -> r.verificacao().passou()).count();
		List<Resultado> comEsperado = lista.stream()
				.filter(r -> r.pergunta().esperado() != Pergunta.Esperado.INDIFERENTE).toList();
		long ferramentaCerta = comEsperado.stream().filter(r -> r.erro() == null && ferramentaOk(r)).count();
		List<Long> latencias = lista.stream().filter(r -> r.erro() == null).map(Resultado::latenciaMs).toList();
		return "| " + modelo + " | " + erros + "/" + lista.size() + " | " + passaram + "/" + lista.size() + " | "
				+ ferramentaCerta + "/" + comEsperado.size() + " | " + percentil(latencias, 50) + " ms | "
				+ percentil(latencias, 95) + " ms | " + percentil(latencias, 100) + " ms | "
				+ media(lista, true) + " / " + media(lista, false) + " |\n";
	}

	private static boolean ferramentaOk(Resultado r) {
		return r.verificacao().falhas().stream()
				.noneMatch(f -> f.startsWith("não chamou") || f.startsWith("chamou ferramenta"));
	}

	private static String media(List<Resultado> lista, boolean entrada) {
		List<Integer> valores = lista.stream().map(r -> entrada ? r.tokensEntrada() : r.tokensSaida())
				.filter(v -> v != null).toList();
		return valores.isEmpty() ? "n/d" : String.valueOf(Math.round(valores.stream().mapToInt(Integer::intValue).average().orElse(0)));
	}

	private static String ferramentas(Resultado r) {
		return r.ferramentas().isEmpty() ? "nenhuma"
				: r.ferramentas().stream().map(ChamadaDeFerramenta::toString).collect(Collectors.joining(", "));
	}

	private static String celula(String texto) {
		String limpo = (texto == null ? "" : texto).replace("|", "\\|").replaceAll("\\s+", " ").strip();
		return limpo.length() > TAMANHO_NA_TABELA ? limpo.substring(0, TAMANHO_NA_TABELA) + "..." : limpo;
	}
}
