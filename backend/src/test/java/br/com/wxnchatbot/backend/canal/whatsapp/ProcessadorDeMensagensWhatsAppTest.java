package br.com.wxnchatbot.backend.canal.whatsapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.canal.whatsapp.EntregaDoWhatsApp.MensagemRecebida;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;
import br.com.wxnchatbot.backend.service.RespostaChat;

@ExtendWith(MockitoExtension.class)
class ProcessadorDeMensagensWhatsAppTest {

	private static final String NUMERO = "5581999990101";

	@Mock
	private ChatServico chat;

	@Mock
	private WhatsAppClient whatsApp;

	private ProcessadorDeMensagensWhatsApp processador;

	@BeforeEach
	void preparar() {
		processador = new ProcessadorDeMensagensWhatsApp(chat, whatsApp, Runnable::run);
	}

	private static MensagemRecebida texto(String id, String conteudo) {
		return new MensagemRecebida(id, NUMERO, "Maria Souza", "text", conteudo);
	}

	private static RespostaChat resposta(String texto) {
		return new RespostaChat(UUID.randomUUID(), texto, StatusConversa.COM_CHATBOT);
	}

	@Test
	void textoVaiParaOChatEARespostaVoltaPeloWhatsApp() {
		when(chat.receber(NUMERO, "Maria Souza", "Oi")).thenReturn(resposta("Olá!"));

		processador.aceitar(List.of(texto("wamid.1", "Oi")));

		verify(whatsApp).enviarTexto(NUMERO, "Olá!");
	}

	@Test
	void conversaComUmAtendenteNaoGeraRespostaDaLily() {
		when(chat.receber(any(), any(), any())).thenReturn(new RespostaChat(UUID.randomUUID(), null, StatusConversa.COM_ATENDENTE));

		processador.aceitar(List.of(texto("wamid.1", "Obrigado")));

		verifyNoInteractions(whatsApp);
	}

	@Test
	void mensagemQueNaoETextoRecebeUmAvisoESemChamarOChat() {
		processador.aceitar(List.of(new MensagemRecebida("wamid.2", NUMERO, null, "audio", null)));

		verify(whatsApp).enviarTexto(NUMERO, ProcessadorDeMensagensWhatsApp.SO_TEXTO);
		verifyNoInteractions(chat);
	}

	@Test
	void aMesmaMensagemEntregueDuasVezesSoEAtendidaUma() {
		when(chat.receber(any(), any(), any())).thenReturn(resposta("Olá!"));

		processador.aceitar(List.of(texto("wamid.3", "Oi")));
		processador.aceitar(List.of(texto("wamid.3", "Oi")));

		verify(chat, times(1)).receber(any(), any(), any());
		verify(whatsApp, times(1)).enviarTexto(anyString(), anyString());
	}

	@Test
	void mensagensDeUmaMesmaEntregaSaoAtendidasEmOrdem() {
		when(chat.receber(eq(NUMERO), any(), anyString())).thenReturn(resposta("ok"));

		processador.aceitar(List.of(texto("wamid.4", "primeira"), texto("wamid.5", "segunda")));

		InOrder ordem = inOrder(chat);
		ordem.verify(chat).receber(NUMERO, "Maria Souza", "primeira");
		ordem.verify(chat).receber(NUMERO, "Maria Souza", "segunda");
	}

	@Test
	void numeroSemCadastroRecebeAvisoEspecifico() {
		when(chat.receber(any(), any(), any())).thenThrow(new RecursoNaoEncontradoException("Não há cadastro para este telefone"));

		processador.aceitar(List.of(texto("wamid.6", "Oi")));

		verify(whatsApp).enviarTexto(NUMERO, ProcessadorDeMensagensWhatsApp.SEM_CADASTRO);
	}

	@Test
	void falhaInesperadaRecebeDesculpasENaoDerrubaAsProximas() {
		when(chat.receber(eq(NUMERO), any(), eq("quebra"))).thenThrow(new IllegalStateException("bug"));
		when(chat.receber(eq(NUMERO), any(), eq("normal"))).thenReturn(resposta("Olá!"));

		processador.aceitar(List.of(texto("wamid.7", "quebra"), texto("wamid.8", "normal")));

		verify(whatsApp).enviarTexto(NUMERO, ProcessadorDeMensagensWhatsApp.FALHA);
		verify(whatsApp).enviarTexto(NUMERO, "Olá!");
	}

	@Test
	void falhaAoAvisarOClienteNaoPropagaEProximaMensagemSegue() {
		when(chat.receber(eq(NUMERO), any(), eq("quebra"))).thenThrow(new IllegalStateException("bug"));
		when(chat.receber(eq(NUMERO), any(), eq("normal"))).thenReturn(resposta("Olá!"));
		doThrow(new EnvioNoWhatsAppFalhouException("Meta fora do ar", null)).when(whatsApp)
				.enviarTexto(NUMERO, ProcessadorDeMensagensWhatsApp.FALHA);

		processador.aceitar(List.of(texto("wamid.9", "quebra"), texto("wamid.10", "normal")));

		verify(whatsApp).enviarTexto(NUMERO, "Olá!");
	}

	@Test
	void mensagemSemRemetenteEIgnorada() {
		processador.aceitar(List.of(new MensagemRecebida("wamid.11", null, null, "text", "Oi")));

		verifyNoInteractions(chat, whatsApp);
	}

	@Test
	void entregaSemMensagensNaoFazNada() {
		processador.aceitar(List.of());

		verifyNoMoreInteractions(chat, whatsApp);
		verify(whatsApp, never()).enviarTexto(any(), any());
	}
}
