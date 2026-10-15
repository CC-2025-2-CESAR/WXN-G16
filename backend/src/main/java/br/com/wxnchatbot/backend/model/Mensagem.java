package br.com.wxnchatbot.backend.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Mensagem {

	private final UUID id = UUID.randomUUID();
	private final Remetente remetente;
	private final String conteudo;
	private final Instant enviadaEm = Instant.now();

	Mensagem(Remetente remetente, String conteudo) {
		this.remetente = Objects.requireNonNull(remetente, "remetente é obrigatório");
		this.conteudo = Validacao.textoObrigatorio(conteudo, "conteúdo");
	}

	public UUID getId() {
		return id;
	}

	public Remetente getRemetente() {
		return remetente;
	}

	public TipoRemetente getTipoRemetente() {
		return remetente.getTipo();
	}

	public String getConteudo() {
		return conteudo;
	}

	public Instant getEnviadaEm() {
		return enviadaEm;
	}
}
