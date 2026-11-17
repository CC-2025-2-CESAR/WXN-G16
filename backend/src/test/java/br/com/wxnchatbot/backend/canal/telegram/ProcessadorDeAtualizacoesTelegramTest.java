package br.com.wxnchatbot.backend.canal.telegram;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import br.com.wxnchatbot.backend.canal.telegram.AtualizacaoDoTelegram.ChatDoTelegram;
import br.com.wxnchatbot.backend.canal.telegram.AtualizacaoDoTelegram.MensagemDoTelegram;
import br.com.wxnchatbot.backend.canal.telegram.AtualizacaoDoTelegram.UsuarioDoTelegram;
import br.com.wxnchatbot.backend.model.OrigemConversa;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.service.ChatServico;
import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;
import br.com.wxnchatbot.backend.service.RespostaChat;

@ExtendWith(MockitoExtension.class)
class ProcessadorDeAtualizacoesTelegramTest {

	private static final long CHAT = 1111111L;
	private static final UsuarioDoTelegram MARIA = new UsuarioDoTelegram(CHAT, false, "Maria", "Souza", "maria");

	@Mock
	private ChatServico chat;

	@Mock
	private TelegramClient telegram;

	private ProcessadorDeAtualizacoesTelegram processador;

	@BeforeEach
	void preparar() {
		processador = new ProcessadorDeAtualizacoesTelegram(chat, telegram, Runnable::run);
	}

	private static AtualizacaoDoTelegram texto(long id, String conteudo) {
		return new AtualizacaoDoTelegram(id, new MensagemDoTelegram(id, MARIA, new ChatDoTelegram(CHAT, "private"), conteudo));
	}

	private static RespostaChat resposta(String texto) {
		return new RespostaChat(UUID.randomUUID(), texto, StatusConversa.COM_CHATBOT);
	}

	@Test
	void textoVaiParaOChatComOIdDoChatComoClienteEARespostaVoltaPeloTelegram() {
		when(chat.receber(OrigemConversa.TELEGRAM, "tg:1111111", "Maria Souza", "Oi")).thenReturn(resposta("Olá!"));

		processador.aceitar(texto(1, "Oi"));

		verify(telegram).enviarTexto(CHAT, "Olá!");
	}

	@Test
	void conversaComUmAtendenteNaoGeraRespostaDaLily() {
		when(chat.receber(eq(OrigemConversa.TELEGRAM), any(), any(), any()))
				.thenReturn(new RespostaChat(UUID.randomUUID(), null, StatusConversa.COM_ATENDENTE));

		processador.aceitar(texto(1, "Alguém aí?"));

		verifyNoInteractions(telegram);
	}

	@Test
	void startRecebeBoasVindasSemChamarALily() {
		processador.aceitar(texto(1, "/start"));
		processador.aceitar(texto(2, "/start ref123"));
		processador.aceitar(texto(3, "/start@LilyWxnBot"));

		verify(telegram, times(3)).enviarTexto(CHAT, ProcessadorDeAtualizacoesTelegram.BOAS_VINDAS);
		verifyNoInteractions(chat);
	}

	@Test
	void outroComandoVaiParaALilyComoQualquerTexto() {
		when(chat.receber(eq(OrigemConversa.TELEGRAM), any(), any(), eq("/ajuda"))).thenReturn(resposta("Posso ajudar!"));

		processador.aceitar(texto(1, "/ajuda"));

		verify(telegram).enviarTexto(CHAT, "Posso ajudar!");
	}

	@Test
	void mensagemQueNaoEhTextoRecebeAvisoSemChamarALily() {
		processador.aceitar(texto(1, null));
		processador.aceitar(texto(2, "   "));

		verify(telegram, times(2)).enviarTexto(CHAT, ProcessadorDeAtualizacoesTelegram.SO_TEXTO);
		verifyNoInteractions(chat);
	}

	@Test
	void gruposCanaisEBotsSaoIgnorados() {
		processador.aceitar(new AtualizacaoDoTelegram(1,
				new MensagemDoTelegram(1, MARIA, new ChatDoTelegram(-100123, "supergroup"), "oi")));
		processador.aceitar(new AtualizacaoDoTelegram(2,
				new MensagemDoTelegram(2, new UsuarioDoTelegram(9, true, "Outro Bot", null, "outrobot"),
						new ChatDoTelegram(9, "private"), "oi")));
		processador.aceitar(new AtualizacaoDoTelegram(3, null));
		processador.aceitar(new AtualizacaoDoTelegram(4, new MensagemDoTelegram(4, MARIA, null, "oi")));

		verifyNoInteractions(chat, telegram);
	}

	@Test
	void mensagemSemRemetenteAindaEhAtendidaSemNome() {
		when(chat.receber(OrigemConversa.TELEGRAM, "tg:1111111", null, "Oi")).thenReturn(resposta("Olá!"));

		processador.aceitar(new AtualizacaoDoTelegram(1, new MensagemDoTelegram(1, null, new ChatDoTelegram(CHAT, "private"), "Oi")));

		verify(telegram).enviarTexto(CHAT, "Olá!");
	}

	@Test
	void mesmaAtualizacaoRepetidaPeloTelegramSoGeraUmaResposta() {
		when(chat.receber(eq(OrigemConversa.TELEGRAM), any(), any(), any())).thenReturn(resposta("Olá!"));

		processador.aceitar(texto(7, "Oi"));
		processador.aceitar(texto(7, "Oi"));

		verify(chat, times(1)).receber(any(), any(), any(), any());
		verify(telegram, times(1)).enviarTexto(anyLong(), anyString());
	}

	@Test
	void semCadastroOClienteRecebeUmAviso() {
		when(chat.receber(eq(OrigemConversa.TELEGRAM), any(), any(), any()))
				.thenThrow(new RecursoNaoEncontradoException("Não há cadastro para este telefone"));

		processador.aceitar(texto(1, "Oi"));

		verify(telegram).enviarTexto(CHAT, ProcessadorDeAtualizacoesTelegram.SEM_CADASTRO);
	}

	@Test
	void falhaInesperadaAvisaOClienteENaoDerrubaAsProximas() {
		when(chat.receber(eq(OrigemConversa.TELEGRAM), any(), any(), eq("quebra"))).thenThrow(new IllegalStateException("bug"));
		when(chat.receber(eq(OrigemConversa.TELEGRAM), any(), any(), eq("normal"))).thenReturn(resposta("Olá!"));

		processador.aceitar(texto(1, "quebra"));
		processador.aceitar(texto(2, "normal"));

		verify(telegram).enviarTexto(CHAT, ProcessadorDeAtualizacoesTelegram.FALHA);
		verify(telegram).enviarTexto(CHAT, "Olá!");
	}

	@Test
	void seOAvisoDeFalhaTambemFalharNadaPropaga() {
		when(chat.receber(eq(OrigemConversa.TELEGRAM), any(), any(), any())).thenThrow(new IllegalStateException("bug"));
		doThrow(new FalhaNoTelegramException("bloqueado", 403)).when(telegram).enviarTexto(anyLong(), anyString());

		processador.aceitar(texto(1, "Oi"));

		verify(telegram).enviarTexto(CHAT, ProcessadorDeAtualizacoesTelegram.FALHA);
	}
}
