package br.com.wxnchatbot.backend.service;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Mensagem;
import br.com.wxnchatbot.backend.model.NivelUrgencia;
import br.com.wxnchatbot.backend.model.Ticket;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServico {
    private static final int MENSAGENS_NO_RESUMO = 3;
    private final ConversaRepositorio conversaRepositorio;
    private final ClassificadorUrgencia classificador;
    public TicketServico(ConversaRepositorio conversaRepositorio, ClassificadorUrgencia classificador) {
        this.conversaRepositorio = conversaRepositorio;
        this.classificador = classificador;
    }

    @Transactional
    public Ticket abrir(UUID conversaId, String motivo) {
        Conversa conversa = conversaRepositorio.findById(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));

        List<String> textosDoCliente = conversa.getMensagens().stream()
                .filter(m -> m.getTipoRemetente() == TipoRemetente.CLIENTE)
                .map(Mensagem::getConteudo).toList();

        NivelUrgencia urgencia = maiorUrgencia(motivo, textosDoCliente);
        String resumo = resumir(textosDoCliente);

        Ticket ticket = conversa.solicitarAtendente(motivo, urgencia, resumo);
        conversaRepositorio.save(conversa);
        return ticket;
    }

    /** Classifica o motivo e cada mensagem do cliente e fica com o nível mais alto. */
    private NivelUrgencia maiorUrgencia(String motivo, List<String> textos) {
        List<String> todos = new ArrayList<>(textos);
        todos.add(motivo);

        NivelUrgencia maior = NivelUrgencia.BAIXA;
        for (String texto : todos) {
            NivelUrgencia nivel = classificador.classificar(texto).getNivel();
            if (nivel.compareTo(maior) > 0) {
                maior = nivel;
            }
        }
        return maior;
    }

    /** As últimas mensagens do cliente, para o atendente entender o caso sem ler tudo. */
    private String resumir(List<String> textos) {
        if (textos.isEmpty()){
            return null;
        }
        int inicio = Math.max(0, textos.size() - MENSAGENS_NO_RESUMO);
        List<String> ultimas = textos.subList(inicio, textos.size());
        return String.join(" | ", ultimas);
    }
}

