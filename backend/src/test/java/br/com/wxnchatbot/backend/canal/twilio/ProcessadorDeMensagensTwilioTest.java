package br.com.wxnchatbot.backend.canal.twilio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;
import br.com.wxnchatbot.backend.service.RespostaChat;

@ExtendWith(MockitoExtension.class)
class ProcessadorDeMensagensTwilioTest {

	private static final String DE = "whatsapp:+5581999990001";

	@Mock
	private ChatServico chat;

	@Mock
	private TwilioClient twilio;

	private ProcessadorDeMensagensTwilio processador;

	@BeforeEach
	void preparar() {
		processador = new ProcessadorDeMensagensTwilio(chat, twilio, Runnable::run);
	}

	private static MensagemDoTwilio texto(String id, String conteudo) {
		return new MensagemDoTwilio(id, DE, "Maria Souza", conteudo);
	}

	private static RespostaChat resposta(String texto) {
		return new RespostaChat(UUID.randomUUID(), texto, StatusConversa.COM_CHATBOT);
	}

	@Test
	void textoVaiParaOChatComOTelefoneEmDigitosEARespostaVoltaPeloTwilio() {
		when(chat.receber(OrigemConversa.WHATSAPP, "5581999990001", "Maria Souza", "Oi")).thenReturn(resposta("Olá!"));

		processador.aceitar(texto("SM1", "Oi"));

		verify(twilio).enviarTexto(DE, "Olá!");
	}

	@Test
	void conversaComUmAtendenteNaoGeraRespostaDaLily() {
		when(chat.receber(eq(OrigemConversa.WHATSAPP), any(), any(), any()))
				.thenReturn(new RespostaChat(UUID.randomUUID(), null, StatusConversa.COM_ATENDENTE));

		processador.aceitar(texto("SM1", "Alguém aí?"));

		verifyNoInteractions(twilio);
	}

	@Test
	void mensagemSemTextoRecebeAvisoSemChamarALily() {
		processador.aceitar(texto("SM1", ""));
		processador.aceitar(texto("SM2", null));
		processador.aceitar(texto("SM3", "   "));

		verify(twilio, times(3)).enviarTexto(DE, ProcessadorDeMensagensTwilio.SO_TEXTO);
		verifyNoInteractions(chat);
	}

	@Test
	void remetenteQueNaoEhWhatsAppEhIgnorado() {
		processador.aceitar(new MensagemDoTwilio("SM1", "+5581999990001", null, "Oi"));
		processador.aceitar(new MensagemDoTwilio("SM2", null, null, "Oi"));
		processador.aceitar(new MensagemDoTwilio("SM3", "whatsapp:", null, "Oi"));

		verifyNoInteractions(chat, twilio);
	}

	@Test
	void mesmaMensagemRepetidaPeloTwilioSoGeraUmaResposta() {
		when(chat.receber(eq(OrigemConversa.WHATSAPP), any(), any(), any())).thenReturn(resposta("Olá!"));

		processador.aceitar(texto("SM7", "Oi"));
		processador.aceitar(texto("SM7", "Oi"));

		verify(chat, times(1)).receber(any(), any(), any(), any());
		verify(twilio, times(1)).enviarTexto(anyString(), anyString());
	}

	@Test
	void semCadastroOClienteRecebeUmAviso() {
		when(chat.receber(eq(OrigemConversa.WHATSAPP), any(), any(), any()))
				.thenThrow(new RecursoNaoEncontradoException("Não há cadastro para este telefone"));

		processador.aceitar(texto("SM1", "Oi"));

		verify(twilio).enviarTexto(DE, ProcessadorDeMensagensTwilio.SEM_CADASTRO);
	}

	@Test
	void falhaInesperadaAvisaOClienteENaoDerrubaAsProximas() {
		when(chat.receber(eq(OrigemConversa.WHATSAPP), any(), any(), eq("quebra"))).thenThrow(new IllegalStateException("bug"));
		when(chat.receber(eq(OrigemConversa.WHATSAPP), any(), any(), eq("normal"))).thenReturn(resposta("Olá!"));

		processador.aceitar(texto("SM1", "quebra"));
		processador.aceitar(texto("SM2", "normal"));

		verify(twilio).enviarTexto(DE, ProcessadorDeMensagensTwilio.FALHA);
		verify(twilio).enviarTexto(DE, "Olá!");
	}

	@Test
	void seOAvisoDeFalhaTambemFalharNadaPropaga() {
		when(chat.receber(eq(OrigemConversa.WHATSAPP), any(), any(), any())).thenThrow(new IllegalStateException("bug"));
		doThrow(new FalhaNoTwilioException("fora da janela", 400, 63016)).when(twilio).enviarTexto(anyString(), anyString());

		processador.aceitar(texto("SM1", "Oi"));

		verify(twilio).enviarTexto(DE, ProcessadorDeMensagensTwilio.FALHA);
	}

	@Test
	void osCamposDoFormularioViramAMensagem() {
		java.util.Map<String, java.util.List<String>> formulario = new java.util.LinkedHashMap<>();
		formulario.put("MessageSid", java.util.List.of("SM42"));
		formulario.put("From", java.util.List.of("whatsapp:+5581999990001"));
		formulario.put("ProfileName", java.util.List.of("Maria"));
		formulario.put("Body", java.util.List.of("Oi"));

		MensagemDoTwilio mensagem = MensagemDoTwilio.de(formulario);

		org.junit.jupiter.api.Assertions.assertEquals(new MensagemDoTwilio("SM42", "whatsapp:+5581999990001", "Maria", "Oi"),
				mensagem);
		org.junit.jupiter.api.Assertions.assertEquals("5581999990001", mensagem.telefone());
		org.junit.jupiter.api.Assertions.assertTrue(mensagem.ehWhatsApp());
	}
}
