package br.com.wxnchatbot.experimento;

public record ChamadaDeFerramenta(String nome, String argumentos) {

	@Override
	public String toString() {
		return nome + "(" + argumentos + ")";
	}
}
