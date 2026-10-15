package br.com.wxnchatbot.backend.model;

import java.util.UUID;

public class Chatbot implements Remetente {

	private final UUID id = UUID.randomUUID();
	private final String nome;
	private String mensagemBoasVindas;
	private boolean ativo = true;

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
