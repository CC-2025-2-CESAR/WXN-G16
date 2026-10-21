package br.com.wxnchatbot.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente extends Pessoa {
	
	@Column(name = "telefone", nullable = false)
	private String telefone;
	
	protected Cliente() {
	}

	public Cliente(String nome, String telefone) {
		super(nome);
		this.telefone = normalizarTelefone(telefone);
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = normalizarTelefone(telefone);
	}

	@Override
	public TipoRemetente getTipo() {
		return TipoRemetente.CLIENTE;
	}

	private static String normalizarTelefone(String telefone) {
		String digitos = Validacao.textoObrigatorio(telefone, "telefone").replaceAll("\\D", "");
		if (digitos.length() < 10 || digitos.length() > 15) {
			throw new IllegalArgumentException("telefone deve ter entre 10 e 15 dígitos");
		}
		return digitos;
	}
}
