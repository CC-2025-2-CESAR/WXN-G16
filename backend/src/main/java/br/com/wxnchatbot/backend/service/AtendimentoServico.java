package br.com.wxnchatbot.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Mensagem;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import br.com.wxnchatbot.backend.service.DetalheConversaFila.MensagemResumo;

/**
 * A conversa do atendente com o cliente: lê o histórico (que continua crescendo com as mensagens do cliente) e envia as
 * respostas do atendente. A resposta é gravada primeiro; só depois, fora da transação, vai para o canal do cliente, para
 * não segurar o banco esperando a rede. Se a entrega falhar a mensagem continua no histórico e o atendente é avisado.
 */
@Service
public class AtendimentoServico {

	private static final Logger log = LoggerFactory.getLogger(AtendimentoServico.class);

	private final ConversaRepositorio conversas;
	private final TransactionTemplate transacao;
	private final List<CanalDeSaida> canais;

	@Autowired
	public AtendimentoServico(ConversaRepositorio conversas, PlatformTransactionManager transacoes,
			ObjectProvider<CanalDeSaida> canais) {
		this(conversas, transacoes, canais.orderedStream().toList());
	}

	AtendimentoServico(ConversaRepositorio conversas, PlatformTransactionManager transacoes, List<CanalDeSaida> canais) {
		this.conversas = conversas;
		this.transacao = new TransactionTemplate(transacoes);
		this.canais = List.copyOf(canais);
	}

	/** O histórico inteiro da conversa, da mais antiga para a mais nova, em qualquer estado dela. */
	@Transactional(readOnly = true)
	public List<MensagemResumo> listarMensagens(UUID conversaId) {
		Conversa conversa = conversas.findById(conversaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));
		return conversa.getMensagens().stream().map(AtendimentoServico::resumir).toList();
	}

	/**
	 * Grava a resposta do atendente que assumiu a conversa e a entrega ao cliente pelo canal dele.
	 *
	 * @throws RecursoNaoEncontradoException se a conversa não existe
	 * @throws IllegalStateException se a conversa não está em atendimento (ainda na fila, com a Lily ou encerrada)
	 */
	public ResultadoDoEnvio enviarMensagem(UUID conversaId, String texto) {
		Gravada gravada = transacao.execute(status -> gravar(conversaId, texto));
		return entregar(gravada);
	}

	private Gravada gravar(UUID conversaId, String texto) {
		Conversa conversa = conversas.buscarPorIdParaAtualizar(conversaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));
		if (conversa.getStatus() != StatusConversa.COM_ATENDENTE) {
			throw new IllegalStateException(porQueNaoAceita(conversa.getStatus()));
		}
		Atendente atendente = conversa.getAtendente()
				.orElseThrow(() -> new IllegalStateException("A conversa está em atendimento, mas sem atendente"));
		Mensagem mensagem = conversa.adicionarMensagem(atendente, texto);
		conversas.save(conversa);
		return new Gravada(resumir(mensagem), conversa.getOrigem(), conversa.getCliente().getTelefone());
	}

	private ResultadoDoEnvio entregar(Gravada gravada) {
		Optional<CanalDeSaida> canal = canais.stream().filter(c -> c.origem() == gravada.origem()).findFirst();
		if (canal.isEmpty()) {
			return new ResultadoDoEnvio(gravada.mensagem(), false, avisoSemCanal(gravada.origem()));
		}
		try {
			canal.get().entregar(gravada.identificador(), gravada.mensagem().conteudo());
			return new ResultadoDoEnvio(gravada.mensagem(), true, null);
		} catch (FalhaNaEntregaException e) {
			log.warn("Mensagem do atendente salva, mas não entregue ({}): {}", gravada.origem(), e.getMessage());
			return new ResultadoDoEnvio(gravada.mensagem(), false, e.getMessage());
		} catch (RuntimeException e) {
			log.error("Falha inesperada ao entregar a mensagem do atendente ({})", gravada.origem(), e);
			return new ResultadoDoEnvio(gravada.mensagem(), false,
					"A mensagem foi salva no histórico, mas não foi possível entregá-la ao cliente agora.");
		}
	}

	private static String porQueNaoAceita(StatusConversa status) {
		return switch (status) {
			case COM_CHATBOT -> "A conversa ainda está com a Lily: o atendente só responde depois de assumir a conversa";
			case AGUARDANDO_ATENDENTE -> "A conversa ainda não foi assumida por um atendente";
			case ENCERRADA -> "A conversa já foi encerrada";
			case COM_ATENDENTE -> "A conversa está em atendimento";
		};
	}

	private static String avisoSemCanal(OrigemConversa origem) {
		return switch (origem) {
			case SIMULADOR -> "Conversa do simulador: a mensagem foi salva no histórico, mas o simulador não a recebe.";
			case DEMONSTRACAO -> "Conversa de demonstração: a mensagem foi salva no histórico, mas não sai para nenhum aplicativo.";
			case WHATSAPP -> "O envio pelo WhatsApp ainda não está disponível: a mensagem foi salva no histórico, mas o cliente não a recebeu.";
			case TELEGRAM -> "O Telegram está desligado neste servidor: a mensagem foi salva no histórico, mas o cliente não a recebeu.";
		};
	}

	private static MensagemResumo resumir(Mensagem mensagem) {
		return new MensagemResumo(mensagem.getTipoRemetente(), mensagem.getConteudo(), mensagem.getEnviadaEm());
	}

	/** O que sobra de uma gravação para a entrega: a mensagem, a origem da conversa e quem recebe (fora da transação). */
	private record Gravada(MensagemResumo mensagem, OrigemConversa origem, String identificador) {
	}
}
