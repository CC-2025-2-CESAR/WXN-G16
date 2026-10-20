package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import br.com.wxnchatbot.backend.model.NivelUrgencia;
import br.com.wxnchatbot.backend.model.ResultadoUrgencia;

class ClassificadorUrgenciaTest {

    private ClassificadorUrgencia classificador = new ClassificadorUrgencia();

    // ---------- Casos realmente urgentes (ALTA) ----------

    @Test
    void palavraUrgenteDeveSerAlta() {
        ResultadoUrgencia r = classificador.classificar("Preciso de ajuda URGENTE com o faturamento");
        assertEquals(NivelUrgencia.ALTA, r.getNivel());
    }

    @Test
    void sistemaForaDoArDeveSerAlta() {
        ResultadoUrgencia r = classificador.classificar("O sistema está fora do ar desde cedo!");
        assertEquals(NivelUrgencia.ALTA, r.getNivel());
    }

    @Test
    void naoConseguirEmitirNotaDeveSerAlta() {
        ResultadoUrgencia r = classificador.classificar("Não consigo emitir nota fiscal e estou perdendo vendas");
        assertEquals(NivelUrgencia.ALTA, r.getNivel());
    }

    @Test
    void cobrancaDuplicadaDeveSerAlta() {
        ResultadoUrgencia r = classificador.classificar("Fui cobrado duas vezes na fatura deste mês");
        assertEquals(NivelUrgencia.ALTA, r.getNivel());
    }

    // ---------- Problemas comuns (MEDIA) ----------

    @Test
    void erroNoRelatorioDeveSerMedia() {
        ResultadoUrgencia r = classificador.classificar("Está dando erro ao gerar o relatório");
        assertEquals(NivelUrgencia.MEDIA, r.getNivel());
    }

    @Test
    void telaLentaDeveSerMedia() {
        ResultadoUrgencia r = classificador.classificar("A tela de pedidos está muito lenta hoje");
        assertEquals(NivelUrgencia.MEDIA, r.getNivel());
    }

    // ---------- Dúvidas simples (BAIXA) ----------

    @Test
    void duvidaSimplesDeveSerBaixa() {
        ResultadoUrgencia r = classificador.classificar("Bom dia, como faço para consultar a segunda via do boleto?");
        assertEquals(NivelUrgencia.BAIXA, r.getNivel());
        assertNull(r.getTermo());
    }

    // ---------- Falsos positivos: parece urgente, mas não é ----------

    @Test
    void naoEUrgenteNaoDeveSerAlta() {
        // "Não é urgente, só queria tirar uma dúvida sobre relatórios"
        ResultadoUrgencia r = classificador.classificar(
                "N\u00e3o \u00e9 urgente, s\u00f3 queria tirar uma d\u00favida sobre relat\u00f3rios");
        assertEquals(NivelUrgencia.BAIXA, r.getNivel());
    }

    @Test
    void semUrgenciaNaoDeveSerAlta() {
        ResultadoUrgencia r = classificador.classificar("Sem urgência nenhuma, quando puder me responda");
        assertEquals(NivelUrgencia.BAIXA, r.getNivel());
    }

    @Test
    void problemaQueJaPassouNaoDeveSerAlta() {
        ResultadoUrgencia r = classificador.classificar("Ontem o sistema caiu, mas já voltou ao normal");
        assertEquals(NivelUrgencia.BAIXA, r.getNivel());
    }

    @Test
    void sistemaQueJaNormalizouNaoDeveSerAlta() {
        ResultadoUrgencia r = classificador.classificar("O sistema parou ontem à noite, mas já normalizou, só aviso");
        assertEquals(NivelUrgencia.BAIXA, r.getNivel());
    }

    @Test
    void erroJaResolvidoFicaMediaMasNuncaAlta() {
        ResultadoUrgencia r = classificador.classificar("Tivemos um erro na nota, mas já foi resolvido, obrigado");
        assertEquals(NivelUrgencia.MEDIA, r.getNivel());
    }

    @Test
    void semProblemaNaoDeveSerMedia() {
        ResultadoUrgencia r = classificador.classificar("Sem problema nenhum por aqui, só quero saber o preço do plano");
        assertEquals(NivelUrgencia.BAIXA, r.getNivel());
    }

    // ---------- Outros comportamentos ----------

    @Test
    void deveInformarOTermoQueAcionouARegra() {
        ResultadoUrgencia r = classificador.classificar("O sistema está FORA DO AR!!!");
        assertEquals("fora do ar", r.getTermo());
    }

    @Test
    void acentoEMaiusculaNaoAtrapalham() {
        // "URGÊNCIA!!! preciso de ajuda"
        ResultadoUrgencia r = classificador.classificar("URG\u00caNCIA!!! preciso de ajuda");
        assertEquals(NivelUrgencia.ALTA, r.getNivel());
    }

    @Test
    void altaTemPrioridadeSobreMedia() {
        ResultadoUrgencia r = classificador.classificar("Deu erro e o sistema está fora do ar");
        assertEquals(NivelUrgencia.ALTA, r.getNivel());
    }

    @Test
    void mensagemNulaOuVaziaDeveSerBaixa() {
        assertEquals(NivelUrgencia.BAIXA, classificador.classificar(null).getNivel());
        assertEquals(NivelUrgencia.BAIXA, classificador.classificar("   ").getNivel());
    }
}