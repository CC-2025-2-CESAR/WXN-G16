package br.com.wxnchatbot.backend.controller;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.wxnchatbot.backend.service.RecursoNaoEncontradoException;

@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);
	private static final String PACOTE_DO_PROJETO = "br.com.wxnchatbot.backend.";

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException e) {
		return problema(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler({ IllegalStateException.class, IllegalArgumentException.class })
	public ProblemDetail regraDeNegocio(RuntimeException e) {
		if (!lancadaPeloProjeto(e)) {
			return erroInesperado(e);
		}
		HttpStatus status = e instanceof IllegalStateException ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
		return problema(status, e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail erroInesperado(Exception e) {
		log.error("Erro inesperado ao processar a requisição", e);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR,
				"Não foi possível concluir a operação. Tente novamente em instantes.");
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object corpo, HttpHeaders cabecalhos,
			HttpStatusCode status, WebRequest requisicao) {
		return super.handleExceptionInternal(ex, problema(status, detalhePadrao(ex, status)), cabecalhos, status,
				requisicao);
	}

	private static boolean lancadaPeloProjeto(Exception e) {
		StackTraceElement[] pilha = e.getStackTrace();
		return pilha.length > 0 && pilha[0].getClassName().startsWith(PACOTE_DO_PROJETO);
	}

	private static ProblemDetail problema(HttpStatusCode status, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo(status));
		return problema;
	}

	private static String titulo(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> "Requisição inválida";
			case 404 -> "Não encontrado";
			case 405 -> "Método não permitido";
			case 409 -> "Conflito";
			case 415 -> "Tipo de conteúdo não suportado";
			default -> status.is4xxClientError() ? "Requisição inválida" : "Erro interno";
		};
	}

	private static String detalhePadrao(Exception ex, HttpStatusCode status) {
		if (ex instanceof MethodArgumentNotValidException invalido) {
			return "Dados inválidos: " + invalido.getBindingResult().getFieldErrors().stream()
					.map(erro -> erro.getField() + " " + erro.getDefaultMessage()).collect(Collectors.joining("; "));
		}
		if (ex instanceof MethodArgumentTypeMismatchException divergente) {
			return "O valor informado para '" + divergente.getName() + "' está em formato inválido";
		}
		if (ex instanceof HttpMessageNotReadableException) {
			return "O corpo da requisição está ausente ou malformado";
		}
		return switch (status.value()) {
			case 404 -> "O recurso solicitado não existe";
			case 405 -> "Método não permitido para este recurso";
			case 415 -> "Tipo de conteúdo não suportado";
			default -> status.is4xxClientError() ? "Requisição inválida" : "Erro interno do servidor";
		};
	}
}
