package br.com.wxnchatbot.backend.chatbot;

/**
 * Em que ponto da conversa a Lily está quando precisa falar. O código decide a situação e traz os fatos num rascunho;
 * o {@link Redator} só escolhe as palavras. A descrição vai no prompt da LLM.
 */
public enum Situacao {

	PEDIR_PROTOCOLO("O cliente quer saber de uma demanda, mas ainda não informou o protocolo. Peça o número do protocolo."),
	PEDIR_DETALHES("O cliente quer abrir um chamado, mas ainda não explicou o problema. Peça que conte o que está acontecendo, com o máximo de detalhes."),
	PEDIR_SIM_OU_NAO("Há um chamado esperando confirmação e o cliente não respondeu sim nem não. Peça que responda sim para abrir ou não para cancelar."),
	PEDIR_CONFIRMACAO("Um chamado está pronto para ser aberto, mas só abre depois que o cliente confirmar. Mostre o que vai ser aberto e pergunte se pode confirmar."),
	CHAMADO_ABERTO("O chamado foi aberto. Avise o protocolo dele e que o cliente pode usá-lo para acompanhar o andamento com a Lily."),
	CHAMADO_CANCELADO("O cliente desistiu e nenhum chamado foi aberto. Confirme isso e pergunte se pode ajudar com mais alguma coisa."),
	CHAMADO_INDISPONIVEL("Não foi possível abrir o chamado agora. Peça que responda sim para tentar de novo em instantes, ou que peça para falar com um atendente."),
	PEDIDO_EXPIROU("Não há pedido de chamado esperando confirmação. Peça que o cliente conte de novo o que está acontecendo."),
	DEMANDA_ENCONTRADA("Informe ao cliente o andamento da demanda que ele consultou."),
	DEMANDA_NAO_ENCONTRADA("A demanda informada não existe. Diga que não encontrou e peça para conferir o número, ou ofereça chamar um atendente."),
	CONSULTA_INDISPONIVEL("O sistema da WXN não respondeu à consulta. Avise que não conseguiu consultar agora e sugira tentar de novo em instantes ou falar com um atendente."),
	FORA_DE_ESCOPO("O assunto não é da WXN. Explique com gentileza que só ajuda com o status de demandas, a abertura de chamados e dúvidas sobre o atendimento, e ofereça chamar um atendente."),
	ENCAMINHANDO("O cliente pediu uma pessoa e a conversa já entrou na fila de atendimento. Avise que logo um atendente assume."),
	ENCAMINHANDO_SEM_PROTOCOLO("O cliente não tem o protocolo, então não dá para localizar a demanda com segurança. Avise que vai chamar um atendente para buscar pelo cadastro dele."),
	ENCAMINHANDO_POR_DUVIDA("A Lily não entendeu o pedido. Diga que não tem certeza de que consegue ajudar com isso e que vai chamar um atendente.");

	private final String descricao;

	Situacao(String descricao) {
		this.descricao = descricao;
	}

	public String descricao() {
		return descricao;
	}
}
