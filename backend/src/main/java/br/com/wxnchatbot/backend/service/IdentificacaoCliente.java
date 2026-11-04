package br.com.wxnchatbot.backend.service;

import br.com.wxnchatbot.backend.llm.DadosReconhecimentoCliente;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.repository.ClienteRepositorio;
import br.com.wxnchatbot.backend.repository.EmpresaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentificacaoCliente {

    private final ClienteRepositorio clienteRepositorio;
    private final EmpresaRepositorio empresaRepositorio;

    public IdentificacaoCliente(ClienteRepositorio clienteRepositorio, EmpresaRepositorio empresaRepositorio) {
        this.clienteRepositorio = clienteRepositorio;
        this.empresaRepositorio = empresaRepositorio;
    }

    @Transactional
    public Cliente processarReconhecimento(DadosReconhecimentoCliente dados) {
        Empresa empresa = empresaRepositorio.findByCnpj(dados.cnpj())
            .orElseGet(() -> empresaRepositorio.save(new Empresa("Empresa a definir", dados.cnpj())));

        return clienteRepositorio.findByTelefone(dados.telefone())
            .orElseGet(() -> clienteRepositorio.save(new Cliente(empresa, dados.nome(), dados.telefone())));
    }
}