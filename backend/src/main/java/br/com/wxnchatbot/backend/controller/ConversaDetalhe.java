package br.com.wxnchatbot.backend.controller;

import java.util.List;
import java.util.UUID;

import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.TipoRemetente;

public record ConversaDetalhe(
        UUID id,
        String clienteNome,
        StatusConversa status,
        List<MensagemDetalhe> mensagens
) {
    public ConversaDetalhe(Conversa conversa) {
        this(
                conversa.getId(),
                conversa.getCliente().isPresent() ? conversa.getCliente().get().getNome() : "Anónimo",
                conversa.getStatus(),
                conversa.getMensagens().stream().map(MensagemDetalhe::new).toList()
        );
    }

    public record MensagemDetalhe(
            TipoRemetente tipoRemetente,
            String texto
    ) {
        public MensagemDetalhe(br.com.wxnchatbot.backend.model.Mensagem mensagem) {
            this(mensagem.getTipoRemetente(), mensagem.getTexto());
        }
    }
}