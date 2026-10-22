package br.com.wxnchatbot.backend.integracao.wxn.mock;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.wxnchatbot.backend.integracao.wxn.CabecalhosWxn;
import br.com.wxnchatbot.backend.integracao.wxn.Chamado;
import br.com.wxnchatbot.backend.integracao.wxn.ErroApi;
import br.com.wxnchatbot.backend.integracao.wxn.NovoChamado;

@RestController
@Profile("mock")
@RequestMapping("/mock/wxn")
public class MockWxnController {

	public static final String PROTOCOLO_ERRO = "WXN-ERRO-500";
	public static final String PROTOCOLO_LENTO = "WXN-LENTO";

	private static final Pattern PADRAO_PROTOCOLO = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{2,39}$");

	private final MockWxnDados dados;
	private final long atrasoLentoMs;

	public MockWxnController(MockWxnDados dados, @Value("${wxn.mock.atraso-lento-ms:6000}") long atrasoLentoMs) {
		this.dados = dados;
		this.atrasoLentoMs = atrasoLentoMs;
	}

	@GetMapping("/demandas/{protocolo}")
	public ResponseEntity<Object> consultar(@RequestHeader(CabecalhosWxn.EMPRESA_ID) UUID empresaId,
			@PathVariable String protocolo) throws InterruptedException {
		if (!PADRAO_PROTOCOLO.matcher(protocolo).matches()) {
			return erro(HttpStatus.BAD_REQUEST, ErroApi.PROTOCOLO_INVALIDO, "Protocolo em formato inválido");
		}
		if (PROTOCOLO_ERRO.equals(protocolo)) {
			return erro(HttpStatus.INTERNAL_SERVER_ERROR, ErroApi.ERRO_INTERNO, "Falha simulada pelo mock");
		}
		if (PROTOCOLO_LENTO.equals(protocolo)) {
			Thread.sleep(atrasoLentoMs);
		}
		return dados.buscar(empresaId, protocolo)
				.<ResponseEntity<Object>>map(ResponseEntity::ok)
				.orElseGet(() -> erro(HttpStatus.NOT_FOUND, ErroApi.DEMANDA_NAO_ENCONTRADA,
						"Nenhuma demanda com esse protocolo"));
	}

	@PostMapping("/chamados")
	public ResponseEntity<Chamado> abrir(@RequestHeader(CabecalhosWxn.EMPRESA_ID) UUID empresaId,
			@RequestHeader(value = CabecalhosWxn.CHAVE_IDEMPOTENCIA, required = false) String chaveIdempotencia,
			@RequestBody NovoChamado novo) {
		return ResponseEntity.status(HttpStatus.CREATED).body(dados.abrir(empresaId, novo, chaveIdempotencia));
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
			MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ErroApi> requisicaoInvalida() {
		return ResponseEntity.badRequest()
				.body(new ErroApi(ErroApi.REQUISICAO_INVALIDA, "Cabeçalho ou corpo da requisição inválido"));
	}

	private static ResponseEntity<Object> erro(HttpStatus status, String codigo, String mensagem) {
		return ResponseEntity.status(status).body(new ErroApi(codigo, mensagem));
	}
}
