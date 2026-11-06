package br.com.wxnchatbot.backend.llm;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "chatbot.llm", havingValue = "fake", matchIfMissing = true)
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

	/** O fake não decide nada: com ele o backend usa o classificador por regras, que nunca chega aqui. */
	@Override
	public String responderEmJson(String promptDoSistema, List<MensagemLlm> conversa, Map<String, Object> esquema) {
		throw new UnsupportedOperationException("O LlmClientFake não gera JSON; use chatbot.llm=gemini");
	}
}
