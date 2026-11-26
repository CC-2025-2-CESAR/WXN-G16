package br.com.wxnchatbot.backend.service;

import java.util.List;

public record PaginaHistorico(List<ItemHistorico> itens, long total, int pagina, int tamanho) {
}
