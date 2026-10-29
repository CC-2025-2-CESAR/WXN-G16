package br.com.wxnchatbot.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.dto.DadosReconhecimentoCliente;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.repository.ClienteRepository;
import br.com.wxnchatbot.backend.repository.EmpresaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class IdentificacaoClienteIntegracaoTest {

    @Autowired
    private IdentificacaoCliente servico;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    void limparContexto() {
        em.flush();
        em.clear();
    }

    @Test
    void criarNovoClienteENovaEmpresaSeNenhumExistir() {
        DadosReconhecimentoCliente dados = new DadosReconhecimentoCliente("Carlos Silva", "11988887777", "12345678000199");

        Cliente clienteSalvo = servico.processarReconhecimento(dados);

        assertNotNull(clienteSalvo.getId());
        assertEquals("Carlos Silva", clienteSalvo.getNome());
        assertEquals("11988887777", clienteSalvo.getTelefone());
        assertEquals("12345678000199", clienteSalvo.getEmpresa().getCnpj());
        
        assertTrue(empresaRepository.findByCnpj("12345678000199").isPresent());
        assertTrue(clienteRepository.findByTelefone("11988887777").isPresent());
    }

    @Test
    void reutilizarEmpresaEClienteSeElesJaExistirem() {
        Empresa empresaExistente = new Empresa("WXN Tech", "99888777000166");
        em.persist(empresaExistente);
        Cliente clienteExistente = new Cliente(empresaExistente, "Ana Paula", "21955554444");
        em.persist(clienteExistente);
        em.flush();
        
        long totalEmpresasAntes = empresaRepository.count();
        long totalClientesAntes = clienteRepository.count();

        DadosReconhecimentoCliente dados = new DadosReconhecimentoCliente("Ana Paula", "21955554444", "99888777000166");
        Cliente clienteRetornado = servico.processarReconhecimento(dados);

        assertEquals(clienteExistente.getId(), clienteRetornado.getId());
        assertEquals(empresaExistente.getId(), clienteRetornado.getEmpresa().getId());
        assertEquals(totalEmpresasAntes, empresaRepository.count());
        assertEquals(totalClientesAntes, clienteRepository.count());
    }

    @Test
    void reutilizarEmpresaECriarNovoClienteSeEleAindaNaoExistir() {
        Empresa empresaExistente = new Empresa("WXN Tech", "11222333000144");
        em.persist(empresaExistente);
        em.flush();

        long totalEmpresasAntes = empresaRepository.count();

        DadosReconhecimentoCliente dados = new DadosReconhecimentoCliente("Roberto Carlos", "31933332222", "11222333000144");
        Cliente clienteRetornado = servico.processarReconhecimento(dados);

        assertNotNull(clienteRetornado.getId());
        assertEquals("Roberto Carlos", clienteRetornado.getNome());
        assertEquals(empresaExistente.getId(), clienteRetornado.getEmpresa().getId());
        assertEquals(totalEmpresasAntes, empresaRepository.count());
    }
}
