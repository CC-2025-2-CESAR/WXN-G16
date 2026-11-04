package br.com.wxnchatbot.backend.llm;

import java.util.List;
import java.util.Optional;

public interface LlmClient {

	/** Devolve a resposta da Lily; a última mensagem de "conversa" é a pergunta atual do usuário. */
	String responder(String promptDoSistema, List<MensagemLlm> conversa);

	/** 
	 * Analisa a conversa para identificar os dados do cliente.
	 * Retorna Optional.empty() se os dados obrigatórios ainda não foram fornecidos pelo usuário.
	 */
	Optional<DadosReconhecimentoCliente> extrairDadosCliente(List<MensagemLlm> conversa);
}
