package br.com.wxnchatbot.backend.canal.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.wxnchatbot.backend.canal.whatsapp.EntregaDoWhatsApp.MensagemRecebida;
import tools.jackson.databind.json.JsonMapper;

class EntregaDoWhatsAppTest {

	private final JsonMapper json = JsonMapper.builder().build();

	private static final String TEXTO = """
			{"object":"whatsapp_business_account","entry":[{"id":"102290129340398","changes":[{"field":"messages","value":{
			"messaging_product":"whatsapp",
			"metadata":{"display_phone_number":"15550783881","phone_number_id":"106540352242922"},
			"contacts":[{"profile":{"name":"Maria Souza"},"wa_id":"5581999990101"}],
			"messages":[{"from":"5581999990101","id":"wamid.HBgM1","timestamp":"1504902988","type":"text","text":{"body":"Oi, tudo bem?"}}]}}]}]}""";

	private static final String STATUS = """
			{"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{
			"messaging_product":"whatsapp","metadata":{"phone_number_id":"1"},
			"statuses":[{"id":"wamid.X","status":"delivered","timestamp":"1","recipient_id":"5581999990101"}]}}]}]}""";

	private List<MensagemRecebida> ler(String corpo) {
		return json.readValue(corpo, EntregaDoWhatsApp.class).mensagens();
	}

	@Test
	void leMensagemDeTextoComONomeDoPerfil() {
		List<MensagemRecebida> mensagens = ler(TEXTO);

		assertEquals(List.of(new MensagemRecebida("wamid.HBgM1", "5581999990101", "Maria Souza", "text", "Oi, tudo bem?")),
				mensagens);
		assertTrue(mensagens.get(0).ehTexto());
	}

	@Test
	void avisoDeStatusNaoTemMensagens() {
		assertEquals(List.of(), ler(STATUS));
	}

	@Test
	void mensagemQueNaoETextoVemMarcadaComoTal() {
		String imagem = TEXTO.replace("\"type\":\"text\",\"text\":{\"body\":\"Oi, tudo bem?\"}",
				"\"type\":\"image\",\"image\":{\"id\":\"999\",\"mime_type\":\"image/jpeg\"}");

		MensagemRecebida mensagem = ler(imagem).get(0);

		assertEquals("image", mensagem.tipo());
		assertNull(mensagem.texto());
		assertFalse(mensagem.ehTexto());
	}

	@Test
	void textoEmBrancoNaoContaComoTexto() {
		MensagemRecebida mensagem = new MensagemRecebida("1", "5581999990101", null, "text", "   ");

		assertFalse(mensagem.ehTexto());
	}

	@Test
	void semContatoOUmContatoDeOutroNumeroOMeNomeFicaNulo() {
		String semContato = TEXTO.replace("\"contacts\":[{\"profile\":{\"name\":\"Maria Souza\"},\"wa_id\":\"5581999990101\"}],", "");

		assertNull(ler(semContato).get(0).nome());
	}

	@Test
	void guardaAOrdemDasMensagensDeUmaMesmaEntrega() {
		String duas = TEXTO.replace("\"text\":{\"body\":\"Oi, tudo bem?\"}}]",
				"\"text\":{\"body\":\"primeira\"}},{\"from\":\"5581999990101\",\"id\":\"wamid.HBgM2\",\"type\":\"text\",\"text\":{\"body\":\"segunda\"}}]");

		assertEquals(List.of("primeira", "segunda"), ler(duas).stream().map(MensagemRecebida::texto).toList());
	}

	@Test
	void camposQueNaoConhecemosSaoIgnoradosEFaltasNaoQuebram() {
		assertEquals(List.of(), ler("{}"));
		assertEquals(List.of(), ler("{\"entry\":[]}"));
		assertEquals(List.of(), ler("{\"entry\":[{\"changes\":[{\"field\":\"messages\"}]}],\"novo\":1}"));
	}
}
