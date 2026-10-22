package br.com.wxnchatbot.backend.integracao.wxn;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

public class DemandaGatewayHttp implements DemandaGateway {

	private final RestClient cliente;

	public DemandaGatewayHttp(WxnApiProperties propriedades) {
		HttpClient http = HttpClient.newBuilder().connectTimeout(propriedades.timeoutConexao()).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
		fabrica.setReadTimeout(propriedades.timeoutLeitura());
		this.cliente = RestClient.builder().baseUrl(propriedades.baseUrl()).requestFactory(fabrica).build();
	}

	@Override
	public Optional<Demanda> consultarPorProtocolo(UUID empresaId, String protocolo) {
		try {
			return cliente.get()
					.uri("/demandas/{protocolo}", protocolo)
					.header(CabecalhosWxn.EMPRESA_ID, empresaId.toString())
					.accept(MediaType.APPLICATION_JSON)
					.exchange((requisicao, resposta) -> interpretarConsulta(resposta));
		} catch (RestClientException e) {
			throw new DemandaIndisponivelException("Falha ao consultar a API da WXN: " + e.getMessage(), e);
		}
	}

	@Override
	public Chamado abrirChamado(UUID empresaId, NovoChamado chamado, UUID chaveIdempotencia) {
		try {
			return cliente.post()
					.uri("/chamados")
					.header(CabecalhosWxn.EMPRESA_ID, empresaId.toString())
					.header(CabecalhosWxn.CHAVE_IDEMPOTENCIA, chaveIdempotencia.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(chamado)
					.exchange((requisicao, resposta) -> interpretarAbertura(resposta));
		} catch (RestClientException e) {
			throw new DemandaIndisponivelException("Falha ao abrir chamado na API da WXN: " + e.getMessage(), e);
		}
	}

	private Optional<Demanda> interpretarConsulta(ConvertibleClientHttpResponse resposta) throws IOException {
		int status = resposta.getStatusCode().value();
		if (status == 200) {
			Demanda demanda = resposta.bodyTo(Demanda.class);
			if (demanda == null) {
				throw new DemandaIndisponivelException("A API da WXN respondeu 200 sem corpo");
			}
			return Optional.of(demanda);
		}
		if (status == 404 && corpoIndicaDemandaInexistente(resposta)) {
			return Optional.empty();
		}
		throw new DemandaIndisponivelException("A API da WXN respondeu " + status + " ao consultar a demanda");
	}

	private Chamado interpretarAbertura(ConvertibleClientHttpResponse resposta) throws IOException {
		int status = resposta.getStatusCode().value();
		if (status == 200 || status == 201) {
			Chamado chamado = resposta.bodyTo(Chamado.class);
			if (chamado == null) {
				throw new DemandaIndisponivelException("A API da WXN respondeu " + status + " sem corpo");
			}
			return chamado;
		}
		throw new DemandaIndisponivelException("A API da WXN respondeu " + status + " ao abrir o chamado");
	}

	private boolean corpoIndicaDemandaInexistente(ConvertibleClientHttpResponse resposta) {
		try {
			ErroApi erro = resposta.bodyTo(ErroApi.class);
			return erro != null && ErroApi.DEMANDA_NAO_ENCONTRADA.equals(erro.codigo());
		} catch (RestClientException e) {
			return false;
		}
	}
}
