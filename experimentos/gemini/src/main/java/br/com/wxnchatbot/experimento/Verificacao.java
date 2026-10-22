package br.com.wxnchatbot.experimento;

import java.util.List;

public record Verificacao(List<String> falhas) {

	public boolean passou() {
		return falhas.isEmpty();
	}
}
