package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.wxnchatbot.backend.llm.MensagemLlm;
import br.com.wxnchatbot.backend.llm.MensagemLlm.Papel;

class ClassificadorPorRegrasTest {

	private final ClassificadorPorRegras classificador = new ClassificadorPorRegras(new ExtratorDeProtocolo());

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"Quero falar com um atendente|FALAR_COM_HUMANO",
			"ME PASSA PRA UMA PESSOA DE VERDADE|FALAR_COM_HUMANO",
			"Preciso de atendimento humano, é urgente!|FALAR_COM_HUMANO",
			"Posso falar com alguém?|FALAR_COM_HUMANO",
			"Quero abrir um chamado|ABRIR_CHAMADO",
			"preciso criar um chamado para o financeiro|ABRIR_CHAMADO",
			"Pode registrar um novo chamado?|ABRIR_CHAMADO",
			"Qual é o status da WXN-2026-0001?|CONSULTAR_DEMANDA",
			"wxn-2026-0002|CONSULTAR_DEMANDA",
			"Como anda a minha demanda?|CONSULTAR_DEMANDA",
			"Queria acompanhar o andamento da integração|CONSULTAR_DEMANDA",
			"Não tenho o protocolo aqui|DUVIDA",
			"Não quero falar com atendente, só uma dúvida|FALAR_COM_HUMANO",
			"Oi, bom dia|DUVIDA",
			"Obrigada!|DUVIDA" })
	void classificaPelasPalavrasDaMensagem(String mensagem, Intencao esperada) {
		ResultadoIntencao resultado = classificador.classificar(List.of(new MensagemLlm(Papel.USUARIO, mensagem)));

		assertEquals(esperada, resultado.intencao());
		assertEquals(1.0, resultado.confianca());
	}

	@Test
	void olhaSoParaAUltimaMensagemDoCliente() {
		List<MensagemLlm> conversa = List.of(
				new MensagemLlm(Papel.USUARIO, "Quero falar com um atendente"),
				new MensagemLlm(Papel.ASSISTENTE, "Claro!"),
				new MensagemLlm(Papel.USUARIO, "Esquece, qual o status da WXN-2026-0001?"));

		assertEquals(Intencao.CONSULTAR_DEMANDA, classificador.classificar(conversa).intencao());
	}

	@Test
	void decidirDevolveVazioQuandoNenhumaRegraServe() {
		assertEquals(Optional.empty(), classificador.decidir("Boa tarde, tudo bem?"));
		assertTrue(classificador.decidir("Quero um atendente").isPresent());
	}

	@Test
	void numeroDeTelefoneNaoViraProtocolo() {
		assertEquals(Optional.empty(), classificador.decidir("Meu telefone é 81-99999-0101"));
	}

	@Test
	void conversaSemMensagemDoClienteNaoSeClassifica() {
		assertThrows(IllegalArgumentException.class,
				() -> classificador.classificar(List.of(new MensagemLlm(Papel.ASSISTENTE, "Oi"))));
	}
}
