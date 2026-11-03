package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.NivelUrgencia;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.model.Ticket;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@ExtendWith(MockitoExtension.class)
class TicketServicoTest {

    private static final Empresa EMPRESA = new Empresa("Empresa Demo");

    @Mock
    private ConversaRepositorio conversas;

    private TicketServico servico;

    @BeforeEach
    void preparar() {
        servico = new TicketServico(conversas, new ClassificadorUrgencia());
    }

    private Conversa conversaComMensagens(String... textosDoCliente) {
        Conversa conversa = new Conversa(new Cliente(EMPRESA, "Maria Souza", "81999990001"),
                new Chatbot("Lily", "Oi, sou a Lily!"));
        for (String texto : textosDoCliente) {
            conversa.adicionarMensagem(conversa.getCliente(), texto);
        }
        when(conversas.findById(conversa.getId())).thenReturn(Optional.of(conversa));
        return conversa;
    }

    @Test
    void mensagemAnteriorUrgenteDeixaOTicketComUrgenciaAlta() {
        Conversa conversa = conversaComMensagens("O sistema caiu e estou perdendo vendas");

        Ticket ticket = servico.abrir(conversa.getId(), "Pediu atendimento humano");

        assertEquals(NivelUrgencia.ALTA, ticket.getUrgencia());
        assertEquals(StatusConversa.AGUARDANDO_ATENDENTE, conversa.getStatus());
        verify(conversas).save(conversa);
    }

    @Test
    void conversaSemNadaUrgenteGeraTicketBaixa() {
        Conversa conversa = conversaComMensagens("Bom dia", "Queria tirar uma duvida");

        Ticket ticket = servico.abrir(conversa.getId(), "Pediu atendimento humano");

        assertEquals(NivelUrgencia.BAIXA, ticket.getUrgencia());
    }

    @Test
    void prevaleceONivelMaisAltoEntreAsMensagens() {
        Conversa conversa = conversaComMensagens("O sistema caiu", "Tem um erro na tela");

        Ticket ticket = servico.abrir(conversa.getId(), "Pediu atendimento humano");

        assertEquals(NivelUrgencia.ALTA, ticket.getUrgencia());
    }

    @Test
    void resumoTrazSoAsTresUltimasMensagensDoCliente() {
        Conversa conversa = conversaComMensagens("Bom dia", "A nota nao sai");
        conversa.adicionarMensagem(conversa.getChatbot(), "Vou verificar");
        conversa.adicionarMensagem(conversa.getCliente(), "Ja tentei de novo");
        conversa.adicionarMensagem(conversa.getCliente(), "Preciso de ajuda");

        Ticket ticket = servico.abrir(conversa.getId(), "Pediu atendimento humano");

        assertEquals("A nota nao sai | Ja tentei de novo | Preciso de ajuda", ticket.getResumo());
    }

    @Test
    void conversaSemMensagensGeraResumoNulo() {
        Conversa conversa = conversaComMensagens();

        Ticket ticket = servico.abrir(conversa.getId(), "Pediu atendimento humano");

        assertNull(ticket.getResumo());
    }

    @Test
    void conversaInexistenteDaNaoEncontradoENaoSalva() {
        UUID id = UUID.randomUUID();
        when(conversas.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> servico.abrir(id, "Pediu atendimento humano"));

        verify(conversas, never()).save(any());
    }
}
