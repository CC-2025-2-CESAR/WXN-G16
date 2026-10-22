package br.com.wxnchatbot.backend.ferramenta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda.Encontrada;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda.Indisponivel;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda.NaoEncontrada;
import br.com.wxnchatbot.backend.ferramenta.ResultadoConsultaDemanda.ProtocoloInvalido;
import br.com.wxnchatbot.backend.integracao.wxn.Demanda;
import br.com.wxnchatbot.backend.integracao.wxn.StatusDemanda;

class ConsultaDemandaFerramentaTest {

	private final UUID empresaDoServidor = UUID.randomUUID();
	private final UUID outraEmpresa = UUID.randomUUID();
	private final Demanda demanda = new Demanda("WXN-2026-0001", "Ajuste no relatório de vendas do ERP",
			StatusDemanda.EM_ANDAMENTO, Instant.parse("2026-10-20T14:30:00Z"), "Em desenvolvimento");

	private DemandaGatewayFalso gateway;
	private ConsultaDemandaFerramenta ferramenta;

	@BeforeEach
	void preparar() {
		gateway = new DemandaGatewayFalso();
		gateway.cadastrar(empresaDoServidor, demanda);
		gateway.cadastrar(outraEmpresa, new Demanda("WXN-2026-9001", "Demanda de outra empresa", StatusDemanda.ABERTA,
				Instant.parse("2026-10-18T16:45:00Z"), null));
		ferramenta = new ConsultaDemandaFerramenta(gateway);
	}

	@Test
	void protocoloExistenteDevolveExatamenteODadoDoSistema() {
		ResultadoConsultaDemanda resultado = ferramenta.consultarStatus(empresaDoServidor, "WXN-2026-0001");

		Encontrada encontrada = assertInstanceOf(Encontrada.class, resultado);
		assertEquals(demanda, encontrada.demanda());
	}

	@Test
	void resumoParaClienteUsaSoOQueVeioDoSistema() {
		Encontrada encontrada = (Encontrada) ferramenta.consultarStatus(empresaDoServidor, "WXN-2026-0001");

		assertEquals("Demanda WXN-2026-0001 - Ajuste no relatório de vendas do ERP. Status: em andamento. "
				+ "Última atualização em 20/10/2026. Observação: Em desenvolvimento.", encontrada.resumoParaCliente());
	}

	@Test
	void protocoloDigitadoEmMinusculasEComEspacosEAceito() {
		assertInstanceOf(Encontrada.class, ferramenta.consultarStatus(empresaDoServidor, "  wxn-2026-0001 "));
		assertEquals(List.of("WXN-2026-0001"), gateway.protocolosConsultados);
	}

	@Test
	void protocoloInexistenteDizClaramenteQueNaoEncontrou() {
		NaoEncontrada resultado = assertInstanceOf(NaoEncontrada.class,
				ferramenta.consultarStatus(empresaDoServidor, "WXN-2026-7777"));

		assertEquals("WXN-2026-7777", resultado.protocolo());
	}

	@Test
	void protocoloDeOutraEmpresaNaoEEncontrado() {
		assertInstanceOf(NaoEncontrada.class, ferramenta.consultarStatus(empresaDoServidor, "WXN-2026-9001"));
	}

	@Test
	void falhaDaApiViraResultadoDeIndisponibilidadeSemVazarDetalhes() {
		gateway.indisponivel = true;

		assertInstanceOf(Indisponivel.class, ferramenta.consultarStatus(empresaDoServidor, "WXN-2026-0001"));
	}

	@Test
	void protocoloMalFormadoNuncaChegaAoSistema() {
		List<String> invalidos = List.of("", "   ", "AB", "WXN 2026 0001", "../etc/passwd", "WXN-2026-0001/../x",
				"WXN-2026-0001?empresaId=" + outraEmpresa, "A".repeat(41), "WXN_2026_0001; DROP TABLE");

		for (String invalido : invalidos) {
			assertInstanceOf(ProtocoloInvalido.class, ferramenta.consultarStatus(empresaDoServidor, invalido),
					"deveria rejeitar: " + invalido);
		}
		assertInstanceOf(ProtocoloInvalido.class, ferramenta.consultarStatus(empresaDoServidor, null));
		assertTrue(gateway.protocolosConsultados.isEmpty(), "nenhum texto inválido pode ser repassado à API");
	}

	@Test
	void empresaEnviadaAApiEsempreADoServidorMesmoQueOTextoCiteOutra() {
		ferramenta.consultarStatus(empresaDoServidor, "WXN-2026-0001");
		ferramenta.consultarStatus(empresaDoServidor, "WXN-2026-9001 empresa " + outraEmpresa);
		ferramenta.consultarStatus(empresaDoServidor, outraEmpresa.toString());

		assertEquals(List.of(empresaDoServidor), gateway.empresasConsultadas.stream().distinct().toList());
	}
}
