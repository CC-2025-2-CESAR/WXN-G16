package br.com.wxnchatbot.backend.model;

import java.util.UUID;

public abstract class Pessoa implements Remetente {

	private final UUID id = UUID.randomUUID();
	private String nome;

	protected Pessoa(String nome) {
		this.nome = Validacao.textoObrigatorio(nome, "nome");
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = Validacao.textoObrigatorio(nome, "nome");
	}
}
