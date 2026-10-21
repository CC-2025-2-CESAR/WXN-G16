package br.com.wxnchatbot.backend.service;

import java.util.UUID;

public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String recurso, UUID id) {
		super(recurso + " não encontrado(a): " + id);
	}
}