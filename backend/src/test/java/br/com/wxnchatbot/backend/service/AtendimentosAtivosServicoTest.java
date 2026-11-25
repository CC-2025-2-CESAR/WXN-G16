package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.TipoRemetente;
import br.com.wxnchatbot.backend.repository.ConversaRepositorio;

@ExtendWith(MockitoExtension.class)
class AtendimentosAtivosServicoTest {

	@Mock
	private ConversaRepositorio conversas;

	@Test
	void retomaConversaComResponsavelMotivoHorarioEHistorico() {
		Empresa empresa = new Empresa("Empresa Demo");
		Cliente cliente = new Cliente(empresa, "Maria Souza", "81999990001");
		Chatbot lily = new Chatbot("Lily", "Oi, sou a Lily!");
		Atendente atendente = new Atendente("Ana Martins", "ana@wxn.com.br");
		Conversa conversa = new Conversa(cliente, lily);
		lily.saudar(conversa);
		conversa.adicionarMensagem(cliente, "Preciso de ajuda");
		conversa.solicitarAtendente("Problema com a nota fiscal");
		conversa.atribuirAtendente(atendente);
		when(conversas.buscarAtendimentosAtivos(atendente.getId())).thenReturn(List.of(conversa));

		List<AtendimentoAtivo> resultado = new AtendimentosAtivosServico(conversas).listar(atendente.getId());

		assertEquals(1, resultado.size());
		AtendimentoAtivo ativo = resultado.getFirst();
		assertEquals(conversa.getId(), ativo.detalhe().conversaId());
		assertEquals("Maria Souza", ativo.detalhe().clienteNome());
		assertEquals("Problema com a nota fiscal", ativo.detalhe().motivo());
		assertEquals(List.of(TipoRemetente.CHATBOT, TipoRemetente.CLIENTE),
				ativo.detalhe().mensagens().stream().map(DetalheConversaFila.MensagemResumo::tipoRemetente).toList());
		assertEquals(atendente.getId(), ativo.atendente().id());
		assertNotNull(ativo.assumidaEm());
		verify(conversas).buscarAtendimentosAtivos(atendente.getId());
	}
}
