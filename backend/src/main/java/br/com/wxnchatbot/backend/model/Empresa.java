package br.com.wxnchatbot.backend.model;

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

	@Column(name = "cnpj", unique = true)
	private String cnpj;

	protected Empresa() {
	}

	public Empresa(String nome, String cnpj) {
		this.nome = Validacao.textoObrigatorio(nome, "nome");
		this.cnpj = Validacao.textoObrigatorio(cnpj, "cnpj");
	}

	public UUID getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getCnpj() {
		return cnpj;
	}
}