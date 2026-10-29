package br.com.wxnchatbot.experimento;

import java.util.Locale;
import java.util.Map;

public final class DadosFicticios {

	public static final String NAO_ENCONTRADA = "NAO_ENCONTRADA: nenhuma demanda com esse protocolo";

	private static final Map<String, String> DEMANDAS = Map.of(
			"WXN-2026-0001", "protocolo=WXN-2026-0001; titulo=Ajuste no relatório de vendas do ERP; status=em andamento; "
					+ "atualizada_em=20/10/2026; observacao=Em desenvolvimento pelo time de integrações",
			"WXN-2026-0002", "protocolo=WXN-2026-0002; titulo=Integração de emissão de notas fiscais; status=concluída; "
					+ "atualizada_em=14/10/2026; observacao=Entregue e validada pelo cliente",
			"WXN-2026-0003", "protocolo=WXN-2026-0003; titulo=Cadastro de novos centros de custo; "
					+ "status=aguardando retorno do cliente; atualizada_em=21/10/2026; "
					+ "observacao=Aguardando o cliente enviar a planilha de centros de custo");

	private DadosFicticios() {
	}

	public static String consultar(String protocolo) {
		if (protocolo == null) {
			return NAO_ENCONTRADA;
		}
		return DEMANDAS.getOrDefault(protocolo.strip().toUpperCase(Locale.ROOT), NAO_ENCONTRADA);
	}
}
