package br.com.wxnchatbot.backend.model;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "empresa")
public class Empresa {
	@Id
	private UUID id = UUID.randomUUID();

	@Column(name = "nome", nullable = false)
	private String nome;

	protected Empresa() {
	}

	public Empresa(String nome) {
		this.nome = Validacao.textoObrigatorio(nome, "nome");
	}

	/** Para empresas cujo id já é conhecido por outro sistema, como a empresa de demonstração da API simulada da WXN. */
	public Empresa(UUID id, String nome) {
		this(nome);
		this.id = Objects.requireNonNull(id, "id é obrigatório");
	}

	public UUID getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}
}
