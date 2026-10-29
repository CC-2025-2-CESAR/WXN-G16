package br.com.wxnchatbot.experimento;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class FerramentasDaLily {

	public static final String CONSULTAR_STATUS = "consultarStatusDemanda";
	public static final String SOLICITAR_ABERTURA = "solicitarAberturaChamado";

	private final List<ChamadaDeFerramenta> chamadas = new ArrayList<>();

	@Tool(name = CONSULTAR_STATUS, description = "Consulta o status de uma demanda pelo número de protocolo, como WXN-2026-0001")
	public String consultarStatusDemanda(
			@ToolParam(description = "Número do protocolo informado pelo cliente") String protocolo) {
		chamadas.add(new ChamadaDeFerramenta(CONSULTAR_STATUS, "protocolo=" + protocolo));
		return DadosFicticios.consultar(protocolo);
	}

	@Tool(name = SOLICITAR_ABERTURA, description = "Registra o pedido de abertura de um chamado. Não abre nada: o chamado só é aberto depois que o cliente confirmar.")
	public String solicitarAberturaChamado(@ToolParam(description = "Título curto do problema") String titulo,
			@ToolParam(description = "Descrição do problema com os detalhes que o cliente informou") String descricao) {
		chamadas.add(new ChamadaDeFerramenta(SOLICITAR_ABERTURA, "titulo=" + titulo));
		return "PEDIDO_REGISTRADO: aguardando a confirmação explícita do cliente antes de abrir o chamado";
	}

	public List<ChamadaDeFerramenta> chamadas() {
		return List.copyOf(chamadas);
	}
}
