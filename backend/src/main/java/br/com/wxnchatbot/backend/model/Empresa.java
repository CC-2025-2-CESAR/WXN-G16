package br.com.wxnchatbot.backend.model;

import java.util.Objects;
import java.util.Optional;
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

	/** Só os 14 dígitos. Opcional: nem toda empresa tem CNPJ cadastrado, e não pode haver duas com o mesmo. */
	@Column(name = "cnpj", length = 14, unique = true)
	private String cnpj;

	protected Empresa() {
	}

	public Empresa(String nome) {
		this.nome = Validacao.textoObrigatorio(nome, "nome");
	}

	/** O CNPJ pode vir com pontos, barra e traço; é guardado só com os dígitos. */
	public Empresa(String nome, String cnpj) {
		this(nome);
		this.cnpj = normalizarCnpj(cnpj);
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

	public Optional<String> getCnpj() {
		return Optional.ofNullable(cnpj);
	}

	/**
	 * Tira a formatação do CNPJ e confere que sobram 14 dígitos. Use o resultado também para buscar uma empresa
	 * ({@code EmpresaRepositorio.findByCnpj}). Não confere os dígitos verificadores: o CNPJ só identifica, nunca prova
	 * quem está falando.
	 */
	public static String normalizarCnpj(String texto) {
		String digitos = Validacao.textoObrigatorio(texto, "cnpj").replaceAll("\\D", "");
		if (digitos.length() != 14) {
			throw new IllegalArgumentException("cnpj deve ter 14 dígitos");
		}
		return digitos;
	}
}
