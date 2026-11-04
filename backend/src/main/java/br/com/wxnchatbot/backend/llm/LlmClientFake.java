package br.com.wxnchatbot.backend.llm;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LlmClientFake implements LlmClient {

	@Override
	public String responder(String promptDoSistema, List<MensagemLlm> conversa) {
		if (conversa.isEmpty()) {
			throw new IllegalArgumentException("conversa não pode ser vazia");
		}
		MensagemLlm ultima = conversa.get(conversa.size() - 1);
		return "[resposta simulada da Lily] Recebi: " + ultima.conteudo();
	}

	@Override
	public Optional<DadosReconhecimentoCliente> extrairDadosCliente(List<MensagemLlm> conversa) {
		if (conversa.isEmpty()) {
			return Optional.empty();
		}
		
		MensagemLlm ultima = conversa.get(conversa.size() - 1);
		
		// Simulação: se o usuário digitar "meus dados", o fake devolve um cliente extraído.
		// Isso permite testar a gravação no banco sem precisar da IA real.
		if (ultima.conteudo().toLowerCase().contains("meus dados")) {
			return Optional.of(new DadosReconhecimentoCliente(
				"João Silva", 
				"11999999999", 
				"12345678000199"
			));
		}
		
		return Optional.empty();
	}
}
