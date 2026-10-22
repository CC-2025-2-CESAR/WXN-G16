package br.com.wxnchatbot.backend.integracao.wxn;

public record NovoChamado(String titulo, String descricao) {

	public static final int TITULO_MINIMO = 3;
	public static final int TITULO_MAXIMO = 120;
	public static final int DESCRICAO_MAXIMA = 1000;

	public NovoChamado {
		titulo = titulo == null ? "" : titulo.strip();
		descricao = descricao == null ? "" : descricao.strip();
		if (titulo.length() < TITULO_MINIMO || titulo.length() > TITULO_MAXIMO) {
			throw new IllegalArgumentException(
					"título deve ter entre " + TITULO_MINIMO + " e " + TITULO_MAXIMO + " caracteres");
		}
		if (descricao.length() > DESCRICAO_MAXIMA) {
			throw new IllegalArgumentException("descrição deve ter no máximo " + DESCRICAO_MAXIMA + " caracteres");
		}
	}
}
