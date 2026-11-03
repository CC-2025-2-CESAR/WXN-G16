package br.com.wxnchatbot.backend.llm;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class LlmClientFake implements LlmClient {

	/** Resposta fixa para desenvolvimento e testes, sem chamar nenhuma LLM. */
	@Override
	public String responder(String promptDoSistema, List<MensagemLlm> conversa) {
		if (conversa.isEmpty()) {
			throw new IllegalArgumentException("conversa não pode ser vazia");
		}
		MensagemLlm ultima = conversa.get(conversa.size() - 1);
		return "[resposta simulada da Lily] Recebi: " + ultima.conteudo();
	}
}