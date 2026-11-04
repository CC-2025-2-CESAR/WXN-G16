package br.com.wxnchatbot.backend.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.LlmIndisponivelException;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.PromptDaLily;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Mensagem;
import br.com.wxnchatbot.backend.model.Remetente;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@Service
public class OrquestradorConversaServico {

	private static final Logger log = LoggerFactory.getLogger(OrquestradorConversaServico.class);
	private static final String MENSAGEM_LLM_INDISPONIVEL = "Desculpe, não consegui processar sua mensagem agora. Tente de novo em instantes ou peça para falar com um atendente.";
	private static final int LIMITE_HISTORICO = 20;

	private final ConversaRepositorio conversaRepositorio;
	private final LlmClient llmClient;

	public OrquestradorConversaServico(ConversaRepositorio conversaRepositorio, LlmClient llmClient) {
		this.conversaRepositorio = conversaRepositorio;
		this.llmClient = llmClient;
	}

	/** Salva a mensagem do cliente e, se a conversa está com a Lily, gera e salva a resposta. */
	@Transactional
	public ResultadoTurno processarTurno(UUID conversaId, String texto) {
		Conversa conversa = conversaRepositorio.findById(conversaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));

		// Desempacota o cliente caso exista; se nulo, passa null (o método podeEnviar da Conversa aceita cliente nulo no início)
		Remetente remetenteCliente = conversa.getCliente().orElse(null);
		conversa.adicionarMensagem(remetenteCliente, texto);

		String resposta = null;
		if (conversa.getStatus() == StatusConversa.COM_CHATBOT) {
			resposta = gerarResposta(conversa);
			conversa.adicionarMensagem(conversa.getChatbot(), resposta);
		}
		conversaRepositorio.save(conversa);
		return new ResultadoTurno(resposta, conversa.getStatus());
	}

	/** Pede a resposta à LLM; se ela estiver fora do ar, devolve a mensagem padrão em vez de falhar o turno. */
	private String gerarResposta(Conversa conversa) {
		try {
			return llmClient.responder(PromptDaLily.TEXTO, montarHistorico(conversa));
		} catch (LlmIndisponivelException e) {
			log.warn("LLM indisponível na conversa {}: {}", conversa.getId(), e.getMessage());
			return MENSAGEM_LLM_INDISPONIVEL;
		}
	}

	/** Converte as últimas mensagens da conversa para o formato da LLM. */
	private List<MensagemLlm> montarHistorico(Conversa conversa) {
		List<Mensagem> mensagens = conversa.getMensagens();
		int inicio = Math.max(0, mensagens.size() - LIMITE_HISTORICO);
		return mensagens.subList(inicio, mensagens.size()).stream()
				.map(m -> new MensagemLlm(papelDe(m.getTipoRemetente()), m.getConteudo()))
				.toList();
	}

	private MensagemLlm.Papel papelDe(TipoRemetente tipo) {
		return tipo == TipoRemetente.CLIENTE ? MensagemLlm.Papel.USUARIO : MensagemLlm.Papel.ASSISTENTE;
	}
}