package br.com.wxnchatbot.backend.ferramenta;

import java.time.Instant;
import java.util.UUID;

public record PedidoDeConfirmacao(UUID conversaId, String titulo, String descricao, Instant expiraEm) {

	public String resumoParaCliente() {
		StringBuilder texto = new StringBuilder("Vou abrir um chamado com o título \"").append(titulo).append("\"");
		if (!descricao.isBlank()) {
			texto.append(" e a descrição \"").append(descricao).append("\"");
		}
		return texto.append(". Posso confirmar?").toString();
	}
}
