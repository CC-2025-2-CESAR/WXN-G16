package br.com.wxnchatbot.backend.chatbot;

/** O que o cliente quer com a mensagem; define o caminho que a Lily segue no turno. */
public enum Intencao {
	CONSULTAR_DEMANDA("quer saber o andamento ou o status de uma demanda já existente (geralmente cita um protocolo)"),
	ABRIR_CHAMADO("relata um problema novo ou pede um serviço novo e quer que um chamado seja aberto"),
	DUVIDA("faz uma pergunta geral sobre o atendimento ou os serviços, cumprimenta ou agradece"),
	FALAR_COM_HUMANO("pede para falar com um atendente, uma pessoa ou alguém da equipe"),
	FORA_DE_ESCOPO("fala de um assunto que nada tem a ver com a WXN, como receita, piada ou política"),
	INCERTO("a mensagem é ambígua, confusa ou não dá para saber o que o cliente quer");

	private final String descricao;

	Intencao(String descricao) {
		this.descricao = descricao;
	}

	public String descricao() {
		return descricao;
	}
}
