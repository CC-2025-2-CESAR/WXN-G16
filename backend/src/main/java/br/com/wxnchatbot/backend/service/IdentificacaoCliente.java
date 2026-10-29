package br.com.wxnchatbot.backend.service;

import br.com.wxnchatbot.backend.DadosReconhecimentoCliente;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.repository.ClienteRepository;
import br.com.wxnchatbot.backend.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentificacaoCliente {

    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;

    public IdentificacaoCliente(ClienteRepository clienteRepository, EmpresaRepository empresaRepository) {
        this.clienteRepository = clienteRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public Cliente processarReconhecimento(DadosReconhecimentoCliente dados) {
        
        Empresa empresa = empresaRepository.findByCnpj(dados.cnpj())
            .orElseGet(() -> {
                Empresa novaEmpresa = new Empresa("Empresa a definir", dados.cnpj());
                return empresaRepository.save(novaEmpresa);
            });

        return clienteRepository.findByTelefone(dados.telefone())
            .orElseGet(() -> {
                Cliente novoCliente = new Cliente(empresa, dados.nome(), dados.telefone());
                return clienteRepository.save(novoCliente);
            });
    }
}
