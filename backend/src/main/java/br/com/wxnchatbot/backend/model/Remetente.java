package br.com.wxnchatbot.backend.model;

import java.util.UUID;

public interface Remetente {

	UUID getId();

	String getNome();

	TipoRemetente getTipo();
}
