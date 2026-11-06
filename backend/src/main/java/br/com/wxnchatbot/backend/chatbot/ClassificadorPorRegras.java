package br.com.wxnchatbot.backend.chatbot;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;

/**
 * Decide por palavras-chave. Serve de atalho (pedir atendente e citar protocolo não precisam de LLM), de reserva
 * quando a LLM cai e de classificador único enquanto não há LLM configurada.
 */
@Component
public class ClassificadorPorRegras implements ClassificadorDeIntencao {

	private static final List<String> PEDIDOS_DE_HUMANO = List.of("atendente", "atendimento humano", "humano",
			"falar com alguem", "falar com uma pessoa", "falar com pessoa", "pessoa de verdade", "alguem da equipe",
			"um responsavel", "suporte humano");

	private static final List<String> VERBOS_DE_ABERTURA = List.of("abrir", "abra", "abre", "criar", "crie", "registrar",
			"registre", "novo", "nova");

	private static final List<String> PEDIDOS_DE_STATUS = List.of("status", "andamento", "como esta", "como anda",
			"ja foi resolvido", "situacao", "consultar", "acompanhar", "protocolo");

	private final ExtratorDeProtocolo extrator;

	public ClassificadorPorRegras(ExtratorDeProtocolo extrator) {
		this.extrator = extrator;
	}

	@Override
	public ResultadoIntencao classificar(List<MensagemLlm> conversa) {
		return decidir(ultimaDoCliente(conversa)).orElse(ResultadoIntencao.certa(Intencao.DUVIDA));
	}

	/** Só responde quando uma regra decide com segurança; nos demais casos devolve vazio. */
	public Optional<ResultadoIntencao> decidir(String mensagem) {
		String texto = Texto.normalizar(mensagem);
		// Sem tratar negação de propósito: na dúvida entre ignorar um pedido de atendente e chamar um atendente sem
		// precisar, é melhor chamar.
		if (PEDIDOS_DE_HUMANO.stream().anyMatch(frase -> Texto.contemFrase(texto, frase))) {
			return Optional.of(ResultadoIntencao.certa(Intencao.FALAR_COM_HUMANO));
		}
		if (Texto.contemPalavra(texto, "chamado") && VERBOS_DE_ABERTURA.stream().anyMatch(v -> Texto.contemPalavra(texto, v))) {
			return Optional.of(ResultadoIntencao.certa(Intencao.ABRIR_CHAMADO));
		}
		if (extrator.extrair(mensagem).isPresent() || Texto.contemAlguma(texto, PEDIDOS_DE_STATUS)) {
			return Optional.of(ResultadoIntencao.certa(Intencao.CONSULTAR_DEMANDA));
		}
		return Optional.empty();
	}

	static String ultimaDoCliente(List<MensagemLlm> conversa) {
		for (int i = conversa.size() - 1; i >= 0; i--) {
			if (conversa.get(i).papel() == Papel.USUARIO) {
				return conversa.get(i).conteudo();
			}
		}
		throw new IllegalArgumentException("conversa sem mensagem do cliente");
	}
}
