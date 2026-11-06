package br.com.wxnchatbot.backend.chatbot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DetalhesDoChamadoTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"Quero abrir um chamado",
			"Preciso de ajuda",
			"Preciso de ajuda com aquele problema do relatório",
			"Quero abrir um chamado para aquele problema do relatório",
			"O sistema não funciona",
			"Não consigo acessar o sistema",
			"Tenho um problema, pode me ajudar por favor?",
			"Está dando erro",
			"Oi, boa tarde, tudo bem? Preciso de um chamado",
			"WXN-2026-0001 0001 2026",
			"",
			"   " })
	void mensagemVagaNaoTemDetalhesSuficientes(String texto) {
		assertFalse(DetalhesDoChamado.suficientes(texto), texto);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Meu sistema caiu e a empresa está parada!",
			"O financeiro não abre depois que troquei a senha. Os outros módulos abrem normal.",
			"Preciso de um chamado: a nota fiscal trava na assinatura desde ontem",
			"A nota fiscal trava na assinatura desde ontem e já tentei dez vezes",
			"Está dando erro no relatório de vendas",
			"O relatório de vendas não carrega desde a atualização de ontem" })
	void mensagemQueDizOQueAcontecePassa(String texto) {
		assertTrue(DetalhesDoChamado.suficientes(texto), texto);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Caiu tudo", "Relatório quebrado" })
	void duasPalavrasQueDizemAlgoAindaSaoPoucas(String texto) {
		assertFalse(DetalhesDoChamado.suficientes(texto), texto);
	}
}
