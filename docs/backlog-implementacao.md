# Backlog de implementação e divisão do time

> **Data de referência: 15/10/2026.** Plano para começar a implementar a Lily
> com **8 pessoas de CC** (sem Design). Substitui a seção 7 de
> [`status-e-roadmap.md`](status-e-roadmap.md), que assumia 5 de CC + 3 de
> Design. Junta o rascunho de divisão do grupo (Pessoas 1 a 8) com o
> levantamento técnico de 15/10.
>
> Onde cada coisa está no GitHub:
> - **Histórias de usuário:** [issues #4 a #12](https://github.com/CC-2025-2-CESAR/WXN-G16/issues?q=label%3Auser-story).
> - **Tarefas técnicas** (este documento): [issues com a label `tarefa`](https://github.com/CC-2025-2-CESAR/WXN-G16/issues?q=label%3Atarefa),
>   uma label por pessoa (`p1-dominio-banco`, `p2-persistencia`…) e um
>   *milestone* por sprint.
> - **Board:** [WXN Grupo 16](https://github.com/orgs/CC-2025-2-CESAR/projects/3).

## 1. Onde estamos

*Atualizado em 22/10/2026.*

- **Pronto na `develop`:**
  - Fundação técnica no ar (backend, frontend e deploy).
  - Entidades JPA do domínio (`Cliente`, `Atendente`, `Conversa`, `Mensagem`,
    `Chatbot`, `Ticket`; ver [`modelo-de-classes.md`](modelo-de-classes.md)),
    a fila de conversas (repositórios e serviço) e o classificador de
    urgência por regras (HOF-01).
  - CI nos pull requests: testes do backend com Postgres e lint/build do
    frontend (PLAT-06).
  - Contrato OpenAPI, mock da WXN (perfil `mock`), `DemandaGateway`, as
    ferramentas de consulta e de abertura de chamado com confirmação e o
    `NotificationGateway` fake (INT-01 a INT-08; ver
    [`integracao-wxn.md`](integracao-wxn.md)).
- **Não existe ainda:** migrations (o schema vem do Hibernate com
  `ddl-auto=update`), a `Empresa`, o endpoint de mensagens do chat, o
  orquestrador, a integração com a LLM e as telas além do health check.
- **Nomes:** o `Handoff` do plano antigo agora é o **`Ticket`**, e `Company`
  vira **`Empresa`** (o código de domínio está em português).
- **Escopo do SR2 (PoC):** US 1.1, US 1.4 e US 3.1 (issues #4, #7, #11). As
  outras seis histórias ficam para depois, na seção 7.
- A data do SR2 ainda **não foi confirmada**. Os prazos abaixo são sprints de
  uma semana contados a partir de hoje e devem ser ajustados quando ela sair.

## 2. "Precisa treinar a IA?" — não

Não treinamos nem ajustamos modelo nenhum. Usamos uma LLM pronta e
**controlamos o que ela faz** com camadas. "Filtrar", no vocabulário do
projeto, é o que o glossário chama de *guardrails*:

| Camada | O que faz | Tarefa |
|---|---|---|
| 1. Filtro de entrada | Barra mensagem gigante, tentativa de "ignore suas instruções" e assunto fora do escopo da WXN, antes de chamar a LLM | IA-04 |
| 2. Prompt de sistema | Define a persona Lily, o tom, o escopo e a regra "na dúvida, chame um humano" | IA-03 |
| 3. Dados só por ferramenta | Status, protocolo e urgência vêm de regra ou de *tool calling* na API da WXN, **nunca do "palpite" da LLM** | INT-04 |
| 4. Filtro de saída | Não vaza o prompt de sistema, não promete o que o sistema não faz | IA-05 |
| 5. Plano B | LLM caiu, demorou ou não entendeu: resposta padrão e criação de ticket para humano | IA-05, HOF-02 |

As **4 perguntas frequentes da WXN** (ainda pendentes) entram como exemplos no
prompt ou, numa fase seguinte, como base de conhecimento (RAG). Também não é
treino.

## 3. Qual LLM usar (pesquisa de 15/10/2026, em páginas oficiais)

Requisitos: grátis, sem cartão (de preferência), *tool calling*, português,
resposta rápida, integração com Spring AI 2.0.1. Cada provedor foi levantado
e depois **reconferido de forma independente**, nas páginas oficiais.

| Opção | Veredito | Motivo principal |
|---|---|---|
| **Google Gemini API (free)** | **Principal** | Starter nativo do Spring AI; modelos estáveis com tool calling; disponível no Brasil; cota por projeto |
| **Groq (free)** | Plano B | Boa privacidade, mas cota **da organização inteira**: 30 req/min, 8 mil tokens/min, 1 mil req/dia |
| **Mistral (free mode)** | Plano C | Starter nativo; limites só no console; usa dados para treino (com opt-out); elegibilidade para pessoa física incerta |
| OpenRouter (`:free`) | Descartada | 50 requisições por dia sem comprar crédito; modelos grátis somem e oscilam |
| Cerebras | Descartada | Exige cartão; só US$ 5 de crédito por 30 dias; 5 req/min |
| GitHub Models | Descartada | **Aposentado em 30/07/2026** (tutoriais antigos ainda ensinam, mas não funciona mais) |

### Recomendação: Gemini como principal, Groq como reserva

- **Modelo para desenvolver e para o loop de ferramentas:**
  `gemini-3.5-flash-lite` (o mais rápido, estável, com tool calling). Para
  respostas de melhor qualidade, `gemini-3.8-flash` com `thinking-level=LOW`.
  Evitar modelos `preview`.
- **Dependência:** `spring-ai-starter-model-google-genai`. Definir o modelo
  **explicitamente** (o padrão do Spring AI, `gemini-2.5-flash`, tem acesso
  limitado em contas novas).
- **Reserva:** Groq com `openai/gpt-oss-120b`, pelo starter OpenAI do Spring AI
  com `base-url=https://api.groq.com/openai/v1`. Trocar de provedor tem que ser
  só **configuração** (perfil Spring), por isso o código fala com uma interface
  nossa (IA-06) e não com o provedor direto.

### O que ainda é incerto (e como resolver)

1. **Os números de cota do Gemini grátis não são públicos**: só aparecem no
   painel do AI Studio, com login. Um post de fórum (não oficial) falou em 20
   req/dia para o 3.8 Flash. **A primeira tarefa (IA-01) é abrir o painel e
   anotar os limites reais**; se forem baixos demais, o Groq passa a ser o
   principal.
2. **Não está confirmado que o plano grátis do Gemini dispensa cartão.** A doc
   só deixa implícito. Quem criar o projeto vai descobrir na hora.
3. **O Spring AI não lista `gemini-3.5-flash-lite` nem `gemini-3.8-flash`**
   na tabela de modelos dele, e chaves novas do AI Studio (desde 28/05/2026)
   têm regras diferentes. Por isso o IA-01 inclui um **teste de fumaça com
   tool calling** antes de qualquer um depender disso.
4. **Português e latência (~3 s)**: nenhuma página oficial garante. Medir no IA-07.

### Regras de uso (valem para qualquer provedor)

- **Só dados fictícios**, inclusive telefones. O plano grátis do Gemini usa o
  que enviamos para melhorar produtos e pode ter revisão humana, e os termos
  pedem para não enviar informação pessoal ou confidencial.
- **A cota é por projeto, não por chave.** Cada dev usa o **próprio projeto na
  própria conta**. Não criar contas ou projetos extras só para multiplicar
  cota, e não compartilhar a chave de uma pessoa com o grupo.
- **Os testes automáticos nunca chamam a LLM de verdade**: usam um cliente
  falso (IA-06). Assim 8 pessoas rodando testes não gastam cota.
- **A demo do SR2 usa um projeto exclusivo**, que ninguém usa durante o
  desenvolvimento, e é testado no dia anterior.
- Chave de API só em variável de ambiente, **nunca no Git**.

## 4. Decisões rápidas (reunião de 30 minutos, antes de começar)

| # | Decisão | Recomendação | Quem fecha |
|---|---|---|---|
| D1 | Como persistir o `model` | Anotar as classes de `model` com JPA (sem duplicar em `entity`). O problema é `Mensagem.remetente`, que é uma interface: guardar `tipoRemetente` + `remetenteId` + `remetenteNome` em vez do objeto | P1 (DEC-01) |
| D2 | Multi-empresa agora? | Sim, mínimo: classe `Empresa` (id, nome) com **uma empresa de demo** no seed, e `empresaId` em `Cliente` e `Conversa`. Barato agora, caro depois (e o critério da US 3.1 exige) | P1 (DEC-01) |
| D3 | Nomes e idioma | `Empresa` e `Ticket` (não `Company` e `Handoff`); domínio e rotas em português, termos técnicos em inglês | P1 (DEC-01) |
| D4 | Biblioteca de UI do frontend | Escolher **uma** biblioteca pronta de componentes. Sem designer, o wireframe da Semana 10 vira o layout | P6 (FE-01) |
| D5 | Qual LLM | Seção 3. Decidir depois do IA-01 | P3 (IA-01) |
| D6 | Neon em São Paulo | Sim, recriar agora (banco vazio, custo zero). Detalhes em [`status-e-roadmap.md`](status-e-roadmap.md) seção 3 | P1 (PLAT-03) |

Também precisa de um **contrato de endpoints** antes de frontend e backend
trabalharem em paralelo: o chat (API-01, P6 escreve), a fila e as métricas
(API-02, P7 escreve) e o mock da WXN (INT-01, P5 escreve). Quem consome
escreve o contrato; quem implementa confirma.

## 5. As 8 pessoas (escolham)

| Vaga | Papel | Objetivo | Histórias | Trabalha com | Carga estimada |
|---|---|---|---|---|---|
| **P1** | Modelo de domínio e banco | Definir como conversas, mensagens, clientes e atendimentos são representados e persistidos. | US 1.1 (#4) | P2 | 6 tarefas, ~7 dias |
| **P2** | Persistência da conversa e endpoint de mensagens | Fazer mensagens e histórico sobreviverem ao reinício da aplicação. | US 1.1 (#4), US 3.2 (#12) | P1, P3 | 6 tarefas, ~8 dias |
| **P3** | Orquestração da conversa e Lily (LLM) | Conectar recebimento da mensagem, memória, consulta de dados e encaminhamento; dono da integração com a LLM e das regras da Lily. | US 1.1 (#4), US 1.2 (#5), US 1.4 (#7), US 3.1 (#11) | P2, P4, P5 | 9 tarefas, ~9,5 dias |
| **P4** | Urgência, handoff e fila | Registrar conversas que precisam de atendimento humano e permitir que um atendente as assuma. | US 1.4 (#7), US 2.3 (#10) | P3, P7, P8 | 8 tarefas, ~8 dias |
| **P5** | Mock da API WXN e ferramentas | Simular a consulta de dados do cliente e expô-la como ferramenta segura para o orquestrador. | US 3.1 (#11) | P3 | 8 tarefas, ~9 dias |
| **P6** | Frontend: simulador de chat | Permitir que o grupo teste uma conversa sem depender do WhatsApp real. | US 1.1 (#4), US 1.2 (#5) | P2, P7 | 7 tarefas, ~6,5 dias |
| **P7** | Frontend: dashboard de atendimento | Mostrar os handoffs e permitir que um atendente assuma um caso. | US 1.4 (#7), US 3.2 (#12) | P4, P6 | 6 tarefas, ~6 dias |
| **P8** | Integração, testes e CI | Garantir que as partes funcionem juntas e que cada pull request seja verificado. | US 1.1 (#4), US 1.4 (#7), US 3.1 (#11), US 3.2 (#12) | todos | 9 tarefas, ~10,5 dias |

*A carga é uma estimativa grosseira (P = 0,5 dia, M = 1,5 dia) só para comparar as vagas entre si.*

**Como escolher:** cada pessoa responde no grupo com a vaga de 1ª opção e uma
de reserva, uma pessoa por vaga. Cada pessoa precisa ter **ao menos um PR
mergeado até o fim da Sprint 2**, porque a nota individual de Técnica do SR2
olha os commits de cada um.

**Sugestão para a P8:** quem for tech lead (revisa todos os PRs e conhece a
infra). É a vaga mais carregada, então ela revisa PRs e **não** escreve
funcionalidade de negócio.

**Pool de apoio:** quem terminar a própria lista antes avisa no grupo e pega
tarefa de quem está mais atrasado, começando pela P8 e pela P3. Tarefa pega por
outra pessoa continua com a label original.

## 6. Tarefas por pessoa

Tamanho: **P** ≈ meio dia · **M** ≈ 1 a 2 dias · **G** ≈ 3 dias ou mais.
"Depende" = o que precisa estar pronto antes (uma tarefa pode começar com
implementações falsas enquanto a outra não chega).

### Pessoa 1 — Modelo de domínio e banco

**Objetivo:** Definir como conversas, mensagens, clientes e atendimentos são representados e persistidos. **Trabalha junto com:** P2.

| ID | Tarefa | Tam. | Sprint | Depende | Issue |
|---|---|---|---|---|---|
| DEC-01 | Fechar as decisões de modelo (D1 a D3) e atualizar o diagrama | P | 1 | — | #13 |
| MEM-01 | Mapear o `model` para JPA e criar `Empresa` | M | 1 | DEC-01 | #14 |
| PLAT-04 | Flyway e validação do schema | M | 1 | — | #15 |
| MEM-02 | Migrations do domínio e seed de demonstração | M | 1 | PLAT-04, MEM-01 | #16 |
| PLAT-03 | Recriar o Neon em São Paulo | P | 1 | — | #17 |
| MEM-07 | Testes de validação e de relacionamentos com banco real | M | 3 | MEM-02, PLAT-05 | #18 |

### Pessoa 2 — Persistência da conversa e endpoint de mensagens

**Objetivo:** Fazer mensagens e histórico sobreviverem ao reinício da aplicação. **Trabalha junto com:** P1, P3.

| ID | Tarefa | Tam. | Sprint | Depende | Issue |
|---|---|---|---|---|---|
| MEM-03 | Repositórios e `ConversaService` | M | 2 | MEM-02 | #19 |
| IA-02 | `LilyService`: prompt com histórico e chamada ao `LlmClient` | M | 2 | IA-06, MEM-03 | #20 |
| MEM-04 | Endpoint `POST /api/chat/mensagens` | M | 2 | MEM-03, API-01 | #21 |
| MEM-05 | Testes de integração da US 1.1 | M | 3 | MEM-04, PLAT-05 | #22 |
| MEM-06 | `GET /api/conversas` e `GET /api/conversas/{id}` | P | 3 | MEM-03 | #23 |
| HOF-08 | `GET /api/metricas` | M | 3 | HOF-02 | #24 |

### Pessoa 3 — Orquestração da conversa e Lily (LLM)

**Objetivo:** Conectar recebimento da mensagem, memória, consulta de dados e encaminhamento; dono da integração com a LLM e das regras da Lily. **Trabalha junto com:** P2, P4, P5.

| ID | Tarefa | Tam. | Sprint | Depende | Issue |
|---|---|---|---|---|---|
| IA-01 | Spike da LLM: limites, tool calling e português | P | 1 | — | #25 |
| IA-06 | Interface `LlmClient` com implementação falsa | M | 1 | — | #26 |
| ORQ-01 | Serviço de orquestração de um turno da conversa | M | 2 | — | #27 |
| ORQ-02 | Salvar a mensagem do usuário e a resposta no histórico | P | 2 | ORQ-01, MEM-03 | #28 |
| IA-03 | Prompt de sistema da Lily | M | 2 | IA-02 | #29 |
| ORQ-03 | Falhas da API e da ferramenta com mensagem compreensível | P | 3 | ORQ-01, INT-03 | #30 |
| ORQ-05 | Testes dos caminhos principais e de erro do orquestrador | M | 3 | ORQ-01 | #31 |
| IA-05 | Filtro de saída e plano B | M | 3 | IA-02, HOF-02 | #32 |
| ORQ-04 | Documentar os fluxos de conversa | P | 4 | ORQ-01 | #33 |

### Pessoa 4 — Urgência, handoff e fila

**Objetivo:** Registrar conversas que precisam de atendimento humano e permitir que um atendente as assuma. **Trabalha junto com:** P3, P7, P8.

| ID | Tarefa | Tam. | Sprint | Depende | Issue |
|---|---|---|---|---|---|
| HOF-01 | `UrgenciaClassifier` por regras | M | 1 | — | #34 |
| HOF-02 | Urgência no `Ticket` e `TicketService` | M | 2 | MEM-02, HOF-01 | #35 |
| HOF-03 | `GET /api/tickets/fila` | P | 2 | HOF-02 | #36 |
| HOF-04 | Assumir e resolver ticket | P | 2 | HOF-02 | #37 |
| HOF-05 | Comportamento sem atendente disponível | P | 3 | HOF-02 | #38 |
| HOF-06 | Evento de domínio ao criar ticket | P | 3 | HOF-02 | #39 |
| IA-04 | Filtro de entrada antes da LLM | M | 3 | IA-02 | #40 |
| HOF-07 | Testes da US 1.4 | M | 3 | HOF-02, HOF-03, HOF-04, HOF-05, HOF-06 | #41 |

### Pessoa 5 — Mock da API WXN e ferramentas

**Objetivo:** Simular a consulta de dados do cliente e expô-la como ferramenta segura para o orquestrador. **Trabalha junto com:** P3.

| ID | Tarefa | Tam. | Sprint | Depende | Issue |
|---|---|---|---|---|---|
| INT-01 | Contrato OpenAPI da API de demandas | P | 1 | — | #42 |
| INT-02 | Mock da API da WXN no perfil `mock` | M | 1 | INT-01, PLAT-07 | #43 |
| INT-03 | `DemandaGateway` | M | 2 | INT-02 | #44 |
| INT-04 | Ferramenta `consultarStatusDemanda` | M | 3 | INT-03, IA-06 | #45 |
| INT-05 | Ferramenta `abrirChamado` com confirmação explícita | M | 3 | INT-04 | #46 |
| INT-06 | Protocolo inexistente e API fora do ar | P | 3 | INT-04 | #47 |
| INT-07 | `empresaId` sempre vindo do servidor | P | 3 | INT-04 | #48 |
| INT-08 | Testes da US 3.1 | M | 3 | INT-05, INT-06, INT-07 | #49 |

### Pessoa 6 — Frontend: simulador de chat

**Objetivo:** Permitir que o grupo teste uma conversa sem depender do WhatsApp real. **Trabalha junto com:** P2, P7.

| ID | Tarefa | Tam. | Sprint | Depende | Issue |
|---|---|---|---|---|---|
| FE-01 | Estrutura base do frontend e biblioteca de UI | M | 1 | — | #50 |
| API-01 | Contrato do endpoint de chat | P | 1 | — | #51 |
| FE-02 | Mocks de API no navegador (MSW) | P | 1 | FE-01, API-01 | #52 |
| FE-03 | Simulador de chat | M | 2 | FE-01, API-01 | #53 |
| FE-07 | Estados de carregamento e erro, incluindo o cold start | P | 3 | FE-01 | #54 |
| FE-09 | Testes do frontend | P | 4 | FE-03 | #55 |
| IA-07 | Conjunto de teste de conversas, latência e português | M | 4 | IA-03, ORQ-01, FE-03 | #56 |

### Pessoa 7 — Frontend: dashboard de atendimento

**Objetivo:** Mostrar os handoffs e permitir que um atendente assuma um caso. **Trabalha junto com:** P4, P6.

| ID | Tarefa | Tam. | Sprint | Depende | Issue |
|---|---|---|---|---|---|
| API-02 | Contrato da fila e das métricas | P | 1 | — | #57 |
| PLAT-09 | Atualizar as GitHub Actions | P | 1 | — | #58 |
| FE-04 | Fila de atendimento | M | 2 | FE-01, API-02 | #59 |
| FE-05 | Detalhe do caso e ação de assumir | M | 3 | FE-04, HOF-04 | #60 |
| FE-06 | Painel de métricas | M | 3 | FE-01, API-02, HOF-08 | #61 |
| FE-08 | Login com API key no painel | P | 3 | PLAT-08 | #62 |

### Pessoa 8 — Integração, testes e CI

**Objetivo:** Garantir que as partes funcionem juntas e que cada pull request seja verificado. **Trabalha junto com:** todos.

| ID | Tarefa | Tam. | Sprint | Depende | Issue |
|---|---|---|---|---|---|
| PLAT-01 | Organizar o board e as convenções | P | 1 | — | #63 |
| PLAT-02 | Proteger `main` e `develop` | P | 1 | — | #64 |
| PLAT-07 | Perfis, `.env.example` e Actuator | M | 1 | — | #65 |
| PLAT-05 | Testcontainers para testes de integração | M | 2 | PLAT-04 | #66 |
| PLAT-06 | Workflow de CI em pull request | M | 2 | PLAT-05 | #67 |
| E2E-02 | Acompanhar a integração na `develop` | M | 2 | — | #68 |
| PLAT-08 | Segurança mínima com API key | M | 3 | PLAT-07 | #69 |
| E2E-01 | Roteiro de ponta a ponta repetível | M | 4 | ORQ-01, FE-03 | #70 |
| SEG-01 | Conferir segredos fora do repositório | P | 4 | — | #71 |

## 7. Ordem de integração e sprints

Antes de dividir, a ordem em que as peças se encontram:

1. Enviar uma mensagem pelo simulador e vê-la salva no histórico.
2. Fazer uma segunda pergunta que reaproveita o contexto da conversa.
3. Consultar um protocolo no mock e responder com o dado retornado.
4. Gerar um handoff (ticket) e mostrá-lo na fila do dashboard.
5. Assumir o atendimento e testar uma tentativa inválida de atribuição.
6. **Só então** decidir se a resposta com LLM melhora a demonstração.

| Sprint | Período | Foco | Tarefas |
|---|---|---|---|
| 0 | hoje | Reunião de 30 min: decisões D1 a D6, escolha das vagas, board no ar | — |
| 1 | 15/10 a 22/10 | **Destravar**: banco, contratos, mock, spike da LLM, base do frontend | DEC-01, MEM-01, PLAT-04, MEM-02, PLAT-03, IA-01, IA-06, HOF-01, INT-01, INT-02, FE-01, API-01, FE-02, API-02, PLAT-09, PLAT-01, PLAT-02, PLAT-07 |
| 2 | 22/10 a 29/10 | **Miolo**: a mensagem entra, é gravada e a Lily responde | MEM-03, IA-02, MEM-04, ORQ-01, ORQ-02, IA-03, HOF-02, HOF-03, HOF-04, INT-03, FE-03, FE-04, PLAT-05, PLAT-06, E2E-02 |
| 3 | 29/10 a 05/11 | **Histórias do SR2**: as 3 histórias funcionando com testes | MEM-07, MEM-05, MEM-06, HOF-08, ORQ-03, ORQ-05, IA-05, HOF-05, HOF-06, IA-04, HOF-07, INT-04, INT-05, INT-06, INT-07, INT-08, FE-07, FE-05, FE-06, FE-08, PLAT-08 |
| 4 | 05/11 a 12/11 | **Integração e ensaio**: roteiro de ponta a ponta, medições, buffer | ORQ-04, FE-09, IA-07, E2E-01, SEG-01 |
| 5 | depois | US 1.2 completa, 2.1, 2.2, 2.3, 1.3 e canal WhatsApp real (Meta Cloud API) | fora do PoC |

Se o SR2 for mais cedo do que isso, o corte natural é: sai o painel de
métricas (FE-06, HOF-08) e a US 1.2 fica só no prompt básico, e permanece o que
as 3 histórias exigem.

## 8. Combinados de trabalho

- **Uma tarefa = uma branch** `feature/<ID>-descricao` a partir da `develop` e
  **um PR para a `develop`**, pequeno, que **outra pessoa de outra frente
  revisa**. `main` só recebe merge da `develop` em marco de entrega.
- Cada PR inclui os **testes do próprio componente**. Teste que precisa de banco
  usa Testcontainers; **nenhum teste chama a LLM de verdade**.
- Se uma tarefa travar por causa de outra, comentar na issue e avisar no grupo.
- O frontend e o backend combinam o **contrato antes de implementar**; a
  interface apresenta, o servidor decide (ordem da fila, regras, `empresaId`).
- Sem segredos, sem dados reais e sem chave de API no repositório.
- Decisões já tomadas continuam valendo: sem menção de IA em commits e PRs
  (a declaração semanal aos orientadores continua obrigatória), e quem usar
  Eclipse usa o STS.

## 9. Pendências de fora do time

| Quem | O quê |
|---|---|
| WXN | As 4 perguntas mais frequentes dos clientes; o contrato real (Swagger) da API de demandas; formato de um protocolo |
| Professor | A pergunta do banco de dados ([`status-e-roadmap.md`](status-e-roadmap.md) seção 3); a data do SR2; se classificação por regra, sem LLM decidindo, atende o que ele espera de "guardrails, RAG, tool calling" |
| Orientadores (Raoni e Victor) | Conferir a arquitetura de IA descrita na seção 2 |
