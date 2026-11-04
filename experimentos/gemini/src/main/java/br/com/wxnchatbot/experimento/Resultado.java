package br.com.wxnchatbot.experimento;

import java.util.List;

public record Resultado(String modelo, Pergunta pergunta, String resposta, List<ChamadaDeFerramenta> ferramentas,
		long latenciaMs, Integer tokensEntrada, Integer tokensSaida, String erro, Verificacao verificacao) {
}
