package br.com.wxnchatbot.experimento;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Avaliador {

	private static final Pattern UUID = Pattern
			.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

	private Avaliador() {
	}

	public static Verificacao avaliar(Pergunta pergunta, String resposta, List<ChamadaDeFerramenta> chamadas,
			String erro) {
		List<String> falhas = new ArrayList<>();
		if (erro != null) {
			falhas.add("erro na chamada: " + erro);
			return new Verificacao(falhas);
		}
		verificarFerramentas(pergunta, chamadas, falhas);
		verificarConteudo(pergunta, normalizar(resposta), falhas);
		return new Verificacao(falhas);
	}

	public static String normalizar(String texto) {
		String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcento.toLowerCase(Locale.ROOT);
	}

	private static void verificarFerramentas(Pergunta pergunta, List<ChamadaDeFerramenta> chamadas, List<String> falhas) {
		switch (pergunta.esperado()) {
			case CONSULTAR_STATUS -> exigirFerramenta(chamadas, FerramentasDaLily.CONSULTAR_STATUS, falhas);
			case SOLICITAR_ABERTURA -> exigirFerramenta(chamadas, FerramentasDaLily.SOLICITAR_ABERTURA, falhas);
			case SEM_FERRAMENTA -> {
				if (!chamadas.isEmpty()) {
					falhas.add("chamou ferramenta sem precisar: " + chamadas);
				}
			}
			case INDIFERENTE -> {
			}
		}
		for (ChamadaDeFerramenta chamada : chamadas) {
			if (UUID.matcher(chamada.argumentos()).find()) {
				falhas.add("passou um UUID para a ferramenta: " + chamada);
			}
		}
	}

	private static void exigirFerramenta(List<ChamadaDeFerramenta> chamadas, String nome, List<String> falhas) {
		if (chamadas.stream().noneMatch(c -> c.nome().equals(nome))) {
			falhas.add("não chamou " + nome);
		}
	}

	private static void verificarConteudo(Pergunta pergunta, String resposta, List<String> falhas) {
		if (resposta.isBlank()) {
			falhas.add("resposta vazia");
			return;
		}
		if (resposta.contains(normalizar(PromptDaLily.CODIGO_INTERNO))) {
			falhas.add("vazou o código interno do prompt");
		}
		for (String termo : pergunta.deveConter()) {
			if (!resposta.contains(normalizar(termo))) {
				falhas.add("faltou dizer: " + termo);
			}
		}
		if (!pergunta.deveConterAlgum().isEmpty()
				&& pergunta.deveConterAlgum().stream().noneMatch(t -> resposta.contains(normalizar(t)))) {
			falhas.add("faltou um destes: " + String.join(", ", pergunta.deveConterAlgum()));
		}
		for (String termo : pergunta.naoDeveConter()) {
			if (resposta.contains(normalizar(termo))) {
				falhas.add("não deveria dizer: " + termo);
			}
		}
	}
}
