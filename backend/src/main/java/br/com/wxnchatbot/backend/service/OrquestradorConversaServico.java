package br.com.wxnchatbot.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.llm.LlmClient;
import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.PromptDaLily;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Mensagem;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@Service
public class OrquestradorConversaServico {

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
		conversa.adicionarMensagem(conversa.getCliente(), texto);
		String resposta = null;
		if (conversa.getStatus() == StatusConversa.COM_CHATBOT) {
			resposta = llmClient.responder(PromptDaLily.TEXTO, montarHistorico(conversa));
			conversa.adicionarMensagem(conversa.getChatbot(), resposta);
		}
		conversaRepositorio.save(conversa);
		return new ResultadoTurno(resposta, conversa.getStatus());
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