package br.com.wxnchatbot.backend.integracao.wxn;

public record ErroApi(String codigo, String mensagem) {

	public static final String DEMANDA_NAO_ENCONTRADA = "DEMANDA_NAO_ENCONTRADA";
	public static final String PROTOCOLO_INVALIDO = "PROTOCOLO_INVALIDO";
	public static final String REQUISICAO_INVALIDA = "REQUISICAO_INVALIDA";
	public static final String ERRO_INTERNO = "ERRO_INTERNO";
}
