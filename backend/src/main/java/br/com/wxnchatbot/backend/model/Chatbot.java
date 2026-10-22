package br.com.wxnchatbot.backend.model;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chatbot")
public class Chatbot implements Remetente {

	@Id
	private UUID id = UUID.randomUUID();

	@Column(name = "nome", nullable = false)
	private String nome;

	@Column(name = "mensagem_boas_vindas", nullable = false)
	private String mensagemBoasVindas;

	@Column(name = "ativo", nullable = false)
	private boolean ativo = true;
	
	protected Chatbot() {
	}

	public Chatbot(String nome, String mensagemBoasVindas) {
		this.nome = Validacao.textoObrigatorio(nome, "nome");
		this.mensagemBoasVindas = Validacao.textoObrigatorio(mensagemBoasVindas, "mensagem de boas-vindas");
	}

	public Mensagem saudar(Conversa conversa) {
		return conversa.adicionarMensagem(this, mensagemBoasVindas);
	}

	public void ativar() {
		this.ativo = true;
	}

	public void desativar() {
		this.ativo = false;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public String getMensagemBoasVindas() {
		return mensagemBoasVindas;
	}

	public void setMensagemBoasVindas(String mensagemBoasVindas) {
		this.mensagemBoasVindas = Validacao.textoObrigatorio(mensagemBoasVindas, "mensagem de boas-vindas");
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public String getNome() {
		return nome;
	}

	@Override
	public TipoRemetente getTipo() {
		return TipoRemetente.CHATBOT;
	}
}
