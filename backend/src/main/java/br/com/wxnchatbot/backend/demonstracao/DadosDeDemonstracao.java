package br.com.wxnchatbot.backend.demonstracao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.wxnchatbot.backend.model.Atendente;
import br.com.wxnchatbot.backend.model.Chatbot;
import br.com.wxnchatbot.backend.model.Cliente;
import br.com.wxnchatbot.backend.model.Conversa;
import br.com.wxnchatbot.backend.model.Empresa;
import br.com.wxnchatbot.backend.model.Mensagem;
import br.com.wxnchatbot.backend.model.Remetente;
import br.com.wxnchatbot.backend.model.StatusConversa;
import br.com.wxnchatbot.backend.service.FilaConversasServico;
import jakarta.persistence.EntityManager;

@Component
@Profile("demo")
public class DadosDeDemonstracao implements CommandLineRunner {

    public static final String NOME_EMPRESA = "Empresa Demonstração";
    public static final String DOMINIO_EMAIL = "@demo.wxn.example";

    private static final Logger log = LoggerFactory.getLogger(DadosDeDemonstracao.class);

    private record Fala(boolean doCliente, String texto) {
    }

    private record Cenario(String nome, String telefone, int minutosAtras, String motivo, List<Fala> falas) {
    }

    private static final List<Cenario> NA_FILA = List.of(
            new Cenario("Marcos Albuquerque", "81 99999-0101", 118,
                    "Relatório de vendas com valores divergentes, urgente para a reunião de hoje",
                    List.of(doCliente("Boa tarde. O relatório de vendas do ERP está com valores diferentes do fechamento do mês."),
                            daLily("Boa tarde! Sinto muito pelo transtorno. Você já tem algum protocolo aberto para isso?"),
                            doCliente("Não tenho. Preciso do relatório correto para a reunião de hoje às 16h."),
                            daLily("Entendi que é urgente. Vou chamar um atendente para te ajudar, tudo bem?"),
                            doCliente("Sim, por favor."))),
            new Cenario("Juliana Teixeira", "81 99999-0102", 86,
                    "Quer saber o andamento da integração de notas fiscais e não tem o protocolo",
                    List.of(doCliente("Oi, queria saber como está a integração de emissão de notas fiscais."),
                            daLily("Claro! Qual é o protocolo da demanda?"),
                            doCliente("Não lembro. Pedi isso por telefone na semana passada."),
                            daLily("Sem o protocolo eu não consigo localizar a demanda com segurança. Posso chamar um atendente que busque pelo seu cadastro?"),
                            doCliente("Pode sim."))),
            new Cenario("Roberto Carvalho", "81 99999-0103", 64,
                    "Emissão de nota fiscal trava na etapa de assinatura desde ontem",
                    List.of(doCliente("A emissão de nota fiscal trava na assinatura desde ontem."),
                            daLily("Sinto muito por isso. Aparece alguma mensagem de erro na tela?"),
                            doCliente("Aparece \"tempo esgotado\" e a nota fica parada. Já tentei umas dez vezes."),
                            daLily("Obrigada pelos detalhes. Como isso está parando a emissão das suas notas, vou chamar um atendente agora."))),
            new Cenario("Fernanda Lima", "81 99999-0104", 41,
                    "Quer tratar da renovação do contrato com uma pessoa",
                    List.of(doCliente("Quero falar sobre a renovação do meu contrato."),
                            daLily("Eu consigo ajudar com o status de demandas e a abertura de chamados. Para renovação de contrato, o melhor é falar com um atendente. Posso encaminhar?"),
                            doCliente("Prefiro falar com uma pessoa mesmo."))),
            new Cenario("Diego Barros", "81 99999-0105", 27,
                    "Módulo financeiro bloqueado depois da troca de senha",
                    List.of(doCliente("Troquei a senha hoje e agora o módulo financeiro não abre."),
                            daLily("Entendi. Os outros módulos abrem normalmente para você?"),
                            doCliente("Abrem, só o financeiro que não."),
                            daLily("Parece um problema de permissão de acesso, e isso eu não consigo resolver por aqui. Vou chamar um atendente."))),
            new Cenario("Patrícia Santana", "81 99999-0106", 13,
                    "Diz já ter enviado a planilha da demanda WXN-2026-0003, que consta como aguardando retorno",
                    List.of(doCliente("Qual é o status da demanda WXN-2026-0003?"),
                            daLily("A demanda WXN-2026-0003, de cadastro de novos centros de custo, está aguardando retorno: falta o envio da planilha de centros de custo."),
                            doCliente("Mas eu já mandei a planilha por e-mail na segunda-feira!"),
                            daLily("Entendo a confusão. Não consigo confirmar o recebimento do e-mail por aqui, então vou chamar um atendente para verificar com você."))),
            new Cenario("Luís Henrique Duarte", "81 99999-0107", 4,
                    "Quer cancelar um chamado aberto por engano",
                    List.of(doCliente("Abri um chamado por engano agora há pouco e preciso cancelar."),
                            daLily("Eu não consigo cancelar chamados por aqui. Posso encaminhar você para um atendente?"),
                            doCliente("Pode, é rápido."))));

    private static final List<Cenario> COM_CHATBOT = List.of(
            new Cenario("Camila Ribeiro", "81 99999-0108", 3, null,
                    List.of(doCliente("Oi, tudo bem? Como consulto o status de uma demanda?"),
                            daLily("Oi! É só me passar o protocolo da demanda, por exemplo WXN-2026-0001, que eu consulto para você."))),
            new Cenario("Thiago Menezes", "81 99999-0109", 1, null,
                    List.of(doCliente("Preciso abrir um chamado para o sistema de estoque."),
                            daLily("Posso abrir para você. Me conta o que está acontecendo, com o máximo de detalhes que puder."))));

    private static final Cenario ENCERRADA = new Cenario("Renata Figueiredo", "81 99999-0110", 190,
            "Segunda via de boleto não é gerada pelo sistema",
            List.of(doCliente("O sistema não gera a segunda via do boleto que venceu semana passada."),
                    daLily("Sinto muito. Isso eu não consigo resolver por aqui. Vou chamar um atendente para te ajudar.")));

    private final EntityManager em;
    private final FilaConversasServico fila;

    public DadosDeDemonstracao(EntityManager em, FilaConversasServico fila) {
        this.em = em;
        this.fila = fila;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (jaExistem()) {
            log.info("Dados de demonstração já existem, nada a criar");
            return;
        }
        Instant agora = Instant.now();
        Empresa empresa = persistir(new Empresa(NOME_EMPRESA, "00.000.000/0001-00"));
        Chatbot lily = persistir(new Chatbot("Lily", "Oi, eu sou a Lily, assistente virtual da WXN. Como posso ajudar?"));
        Atendente ana = persistir(new Atendente("Ana Martins", "ana.martins" + DOMINIO_EMAIL));
        persistir(new Atendente("Paulo Mendes", "paulo.mendes" + DOMINIO_EMAIL));
        persistir(new Atendente("Carla Nogueira", "carla.nogueira" + DOMINIO_EMAIL)).marcarIndisponivel();

        for (Cenario cenario : NA_FILA) {
            Conversa conversa = iniciar(empresa, lily, cenario);
            int mensagensAntes = conversa.getMensagens().size();
            fila.solicitarAtendente(conversa.getId(), cenario.motivo());
            ajustarHorarios(conversa, mensagensAntes, agora.minus(cenario.minutosAtras(), ChronoUnit.MINUTES));
        }
        for (Cenario cenario : COM_CHATBOT) {
            Conversa conversa = iniciar(empresa, lily, cenario);
            ajustarHorarios(conversa, conversa.getMensagens().size(),
                    agora.minus(cenario.minutosAtras(), ChronoUnit.MINUTES));
        }

        Conversa encerrada = iniciar(empresa, lily, ENCERRADA);
        int mensagensAntes = encerrada.getMensagens().size();
        fila.solicitarAtendente(encerrada.getId(), ENCERRADA.motivo());
        fila.atribuirAtendente(encerrada.getId(), ana.getId());
        encerrada.adicionarMensagem(ana, "Oi, Renata, aqui é a Ana, da WXN. Vi o problema da segunda via e já ajustei o vencimento no cadastro.");
        encerrada.adicionarMensagem(encerrada.getCliente().orElseThrow(), "Consegui gerar agora, muito obrigada!");
        encerrada.adicionarMensagem(ana, "Que bom! Vou encerrar o atendimento. Se precisar, é só chamar.");
        fila.encerrar(encerrada.getId());
        ajustarHorarios(encerrada, mensagensAntes, agora.minus(ENCERRADA.minutosAtras(), ChronoUnit.MINUTES));

        log.info("Dados de demonstração criados: {} conversas na fila", NA_FILA.size());
    }

    private boolean jaExistem() {
        return em.createQuery("select count(e) from Empresa e where e.nome = :nome", Long.class)
                .setParameter("nome", NOME_EMPRESA).getSingleResult() > 0;
    }

    private <T> T persistir(T entidade) {
        em.persist(entidade);
        return entidade;
    }

    private Conversa iniciar(Empresa empresa, Chatbot lily, Cenario cenario) {
        Cliente cliente = persistir(new Cliente(empresa, cenario.nome(), cenario.telefone()));
        Conversa conversa = new Conversa(lily);
        conversa.vincularCliente(cliente);
        persistir(conversa);
        lily.saudar(conversa);
        for (Fala fala : cenario.falas()) {
            Remetente remetente = fala.doCliente() ? cliente : lily;
            conversa.adicionarMensagem(remetente, fala.texto());
        }
        return conversa;
    }

    private void ajustarHorarios(Conversa conversa, int mensagensAntes, Instant referencia) {
        em.flush();
        List<Mensagem> mensagens = conversa.getMensagens();
        for (int i = 0; i < mensagens.size(); i++) {
            Instant enviadaEm = i < mensagensAntes
                    ? referencia.minusSeconds(40L * (mensagensAntes - i))
                    : referencia.plusSeconds(90L * (i - mensagensAntes + 1));
            atualizar("update Mensagem m set m.enviadaEm = :quando where m.id = :id", enviadaEm,
                    mensagens.get(i).getId());
        }
        atualizar("update Conversa c set c.iniciadaEm = :quando where c.id = :id",
                referencia.minusSeconds(40L * mensagensAntes + 10), conversa.getId());
        conversa.getTicket().ifPresent(ticket -> atualizar("update Ticket t set t.abertoEm = :quando where t.id = :id",
                referencia, ticket.getId()));
        if (conversa.getStatus() == StatusConversa.ENCERRADA) {
            Instant fim = referencia.plusSeconds(90L * (mensagens.size() - mensagensAntes + 1));
            atualizar("update Ticket t set t.resolvidoEm = :quando where t.id = :id", fim,
                    conversa.getTicket().orElseThrow().getId());
            atualizar("update Conversa c set c.encerradaEm = :quando where c.id = :id", fim, conversa.getId());
        }
    }

    private void atualizar(String jpql, Instant quando, UUID id) {
        em.createQuery(jpql).setParameter("quando", quando).setParameter("id", id).executeUpdate();
    }

    private static Fala doCliente(String texto) {
        return new Fala(true, texto);
    }

    private static Fala daLily(String texto) {
        return new Fala(false, texto);
    }
}