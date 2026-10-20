package br.com.wxnchatbot.backend.model;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Pessoa implements Remetente {

	@Id
	private UUID id = UUID.randomUUID();
	@Column(name = "nome", nullable = false)
	private String nome;

	protected Pessoa() {
   	}

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
