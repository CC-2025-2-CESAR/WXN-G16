package br.com.wxnchatbot.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "atendente")
public class Atendente extends Pessoa {
	
	@Column(name = "email", nullable = false)
	private String email;
	
	@Column(name = "disponivel", nullable = false)
	private boolean disponivel = true;
	
	protected Atendente() {
	}

	public Atendente(String nome, String email) {
		super(nome);
		this.email = validarEmail(email);
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = validarEmail(email);
	}

	public boolean isDisponivel() {
		return disponivel;
	}

	public void marcarDisponivel() {
		this.disponivel = true;
	}

	public void marcarIndisponivel() {
		this.disponivel = false;
	}

	@Override
	public TipoRemetente getTipo() {
		return TipoRemetente.ATENDENTE;
	}

	private static String validarEmail(String email) {
		String valor = Validacao.textoObrigatorio(email, "email");
		if (!valor.contains("@")) {
			throw new IllegalArgumentException("email inválido");
		}
		return valor;
	}
}
