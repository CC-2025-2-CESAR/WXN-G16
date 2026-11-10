package br.com.wxnchatbot.backend.chatbot;

import java.util.Arrays;
import java.util.Set;

/**
 * Decide, só pelas palavras, se o cliente já contou o que está acontecendo ou só pediu ajuda de forma vaga. Conta
 * palavras que dizem alguma coisa: artigos, pedidos de ajuda, "aquele problema" e afins não contam.
 */
final class DetalhesDoChamado {

	private static final int PALAVRAS_MINIMAS = 3;
	private static final int TAMANHO_MINIMO_DA_PALAVRA = 3;

	private static final Set<String> SEM_INFORMACAO = Set.of(
			// artigos, preposições e conjunções
			"uma", "uns", "umas", "dos", "das", "nos", "nas", "por", "para", "pra", "pro", "com", "sem", "sob", "sobre",
			"que", "mas", "como", "entre", "ate", "desde", "ainda", "quando", "onde",
			// pronomes e demonstrativos
			"meu", "minha", "meus", "minhas", "nosso", "nossa", "nossos", "nossas", "voce", "voces", "vcs", "ele", "ela",
			"eles", "elas", "isso", "isto", "aquilo", "aquele", "aquela", "aqueles", "aquelas", "esse", "essa", "esses",
			"essas", "este", "esta", "estes", "estas", "ali", "aqui", "qual", "quais",
			// verbos de uso geral e de pedido
			"sou", "foi", "era", "estao", "estou", "estamos", "estava", "tao", "tem", "tenho", "temos", "fazer", "faz",
			"preciso", "precisamos", "precisar", "queria", "quero", "queremos", "gostaria", "gostariamos", "pedir",
			"pedido", "ajuda", "ajudar", "ajudem", "abrir", "abra", "abrimos", "abrindo", "chamado", "ticket",
			"solicitacao", "suporte", "atendimento", "reportar", "relatar", "informar", "avisar",
			// palavras genéricas demais para descrever um problema
			"problema", "problemas", "coisa", "coisas", "negocio", "negocios", "assunto", "questao", "situacao", "sistema",
			// cumprimentos e cortesias
			"ola", "bom", "boa", "dia", "tarde", "noite", "tudo", "bem", "obrigado", "obrigada", "favor", "pfv",
			// negação e advérbios sem informação
			"nao", "sim", "mais", "muito", "muita", "tambem", "agora");

	private DetalhesDoChamado() {
	}

	static boolean suficientes(String texto) {
		String normalizado = Texto.normalizar(texto).strip();
		if (normalizado.isEmpty()) {
			return false;
		}
		return Arrays.stream(normalizado.split(" "))
				.filter(palavra -> palavra.length() >= TAMANHO_MINIMO_DA_PALAVRA)
				.filter(palavra -> !palavra.chars().allMatch(Character::isDigit))
				.filter(palavra -> !SEM_INFORMACAO.contains(palavra))
				.distinct()
				.count() >= PALAVRAS_MINIMAS;
	}
}
