package br.com.wxnchatbot.backend.llm;

public record DadosReconhecimentoCliente(
    String nome,
    String telefone,
    String cnpj
) {}
