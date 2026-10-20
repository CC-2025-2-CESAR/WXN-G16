# WXN-G16 — Diagnóstico do projeto e plano de ação

> **Data de referência: 23/09/2026.** Documento só de pesquisa/análise — nenhum
> código foi alterado, nenhum recurso de nuvem foi criado além do que já
> existia. Gerado a partir de uma auditoria do repositório, pesquisa técnica
> verificada (Spring AI, latência de banco, WhatsApp Business, mocks de API)
> e três análises independentes de escopo que convergiram para o mesmo
> resultado (seção 4).

## ⚠️ Mais urgente que qualquer coisa neste documento

**A Semana 08 vence amanhã, 24/09/2026, e ela É o SR1** — o Classroom
confirma: "as entregas da semana 08 se referem ao Status Report 1, e devem
ser postadas na Atividade SR01". Não é uma entrega separada a descobrir; é
a apresentação de 5-7 min (Design + CC) descrita no guia oficial do SR1 —
ver o que falta preparar na seção 8. A regra da disciplina é explícita:
entrega fora do prazo vale **zero**, mesmo se enviada depois.

Segundo maior risco estrutural, que também não pode esperar: a disciplina
diz que **só projetos aprovados pelos orientadores seguem para a próxima
fase** — isto é separado de "perder uma entrega vale zero". Um SR1/SR2 mal
avaliado pode significar o projeto não continuar no semestre seguinte, não
só uma nota baixa.

---

## 1. O que temos (inventário real)

### Processo/disciplina
- **Semana 05** (montar ambiente) — entregue.
- **Semana 06** (Design: ideação; CC: lista de funcionalidades + backlog) —
  entregue em 11/09 como "Entrega 6 G16": MoSCoW com 4 MUST e 6 SHOULD,
  backlog de 3 épicos e 9 user stories.
- **Semana 07** (validação, sem template) — feita de forma informal em
  17/09: escopo alinhado com professores e WXN (canal WhatsApp via webhook,
  dados via API REST ou manual/RAG, "catálogo MCP", pedir 4 perguntas
  frequentes à WXN) e feedback do professor de POO sobre o banco de dados
  (ver seção 3).
- **Semana 08 = SR1** — prazo **24/09/2026 (amanhã)**. É a apresentação de
  Descoberta (5-7 min, Design + CC), não uma entrega separada. Ver seção 8
  para o que falta preparar.
- **Fórmulas de nota que definem prioridade** (2º/3º período): nota do
  grupo no SR1 = Nota de Processo; no SR2 = (Processo×4 + CC×3 + Design×3)/10.
  Nota individual no SR1 = (Processo×7 + FaCT×3)/10; no SR2 =
  (Técnica×4 + Processo×3 + FaCT×3)/10. **O FaCT (avaliação individual)
  pesa 3 em ambos os SRs e vale zero se não for entregue no dia exato do
  SR** — é fácil de esquecer por não aparecer em nenhum board técnico.

### Repositório (github.com/CC-2025-2-CESAR/WXN-G16, público)
- Monorepo com `backend/`, `frontend/`, `docs/`, `.github/workflows/`.
- **Backend**: Spring Boot 4.1.1, Java 21, Maven Wrapper → Maven 3.9.16. Só
  existem 3 classes: `BackendApplication`, `HealthController`
  (`GET /api/health` → `{"status":"ok"}`) e `CorsConfig`. **Nenhuma entidade
  JPA, nenhum repositório, nenhum service.** `application.properties` lê
  `DB_URL/DB_USER/DB_PASSWORD/SERVER_PORT/CORS_ALLOWED_ORIGINS` com defaults
  locais; `spring.jpa.hibernate.ddl-auto=update`; `show-sql=true`.
- **Testes**: existe apenas `BackendApplicationTests.contextLoads`, que
  precisa de um Postgres acessível e **nunca roda no CI** (o workflow de
  deploy usa `-DskipTests`).
- **Frontend**: React 19.2 + Vite 8.2 + TypeScript 6 + oxlint. Uma única
  página (Dashboard) que chama `/api/health` e mostra "Backend
  conectado/indisponível".
- **CI/CD**: `deploy-backend.yml` e `deploy-frontend.yml` funcionando
  (últimas execuções com sucesso em 04/09), mas com actions desatualizadas
  (`actions/checkout`, `setup-java`, `setup-node`) e um input inválido
  (`skip_deploy_on_missing_secrets`) no workflow de frontend.
- **Docs**: `modelagem-inicial.md` (ER conceitual: Company, User,
  Conversation, Message, Agent, Handoff — ainda não implementado),
  `deploy.md`, `escopo-chatbot.md` (só na `develop`), `entrega-semana-05.md`.
- **GitHub**: 2 PRs mergeados, **11 colaboradores cadastrados (7 admin)**,
  mas **apenas 1 pessoa (Arthur Reis) fez os 9 commits do repositório**. Sem
  issues, sem board, PRs sem review. A `main` local está desatualizada
  (parada no commit inicial); a `develop` remota está 1 commit à frente da
  `main` remota (só documentação). A `main` não tem proteção de branch.

### Deploy em produção (Azure for Students, US$ 100 de crédito/12 meses,
regiões permitidas: centralus, mexicocentral, brazilsouth, canadacentral,
chilecentral)

| Camada | Onde | Plano | Medição de hoje |
|---|---|---|---|
| Backend | Azure App Service **Windows**, brazilsouth, `wxn-g16-backend.azurewebsites.net` | **F1 (grátis)**, runtime Java 21 | 1ª requisição após ociosidade: **28,2 s** (cold start; esse endpoint não toca o banco). Requisições seguintes: **0,28–0,40 s**. |
| Frontend | Azure Static Web Apps, centralus, `happy-desert-0e6abd410.5.azurestaticapps.net` | Free | **0,58 s** |
| Banco | Neon Postgres 18, **AWS us-east-2 (Ohio)**, endpoint pooler | Free tier, scale-to-zero após 5 min ocioso (não dá pra desligar essa opção no Free) | não medido diretamente hoje pelo backend |

**Correção a fazer em `docs/deploy.md`**: o texto atual afirma que "o Azure
não tem tier gratuito para banco relacional gerenciado" — isso ficou
desatualizado. A oferta de 12 meses do Azure for Students cobre o Postgres
Flexible Server B1MS (não confirmado se a cobertura é automática; checar no
Cost Management do Azure 24–48h depois de criar um).

### Decisões já tomadas pelo grupo
- Não colocar menção de IA em commits/PRs (isso é diferente de declarar o
  uso de IA aos orientadores semanalmente — ver checklist).
- Se algum integrante usar Eclipse, usar **Spring Tool Suite (STS)**, não o
  Eclipse genérico — orientação dos professores em 17/09, para evitar
  conflito de configuração entre quem usa VS Code e quem usa Eclipse.
- Sobre o termo "collection" (provavelmente Postman collection) citado pelo
  professor: decisão é **não usar Postman mock server** para mockar a API da
  WXN — o plano gratuito do Postman mock server permite só 1 usuário, o que
  não serve para um grupo de 8 pessoas colaborando. A alternativa é um
  controller Spring num perfil `mock` (ou WireMock mais adiante).
- Escopo do PoC (SR2) já decidido a partir de uma análise cruzada por três
  ângulos (critérios de aceite, risco e jornada de demo), que **convergiram
  para as mesmas 3 histórias**: US 1.1 (memória), US 1.4 (urgência/handoff)
  e US 3.1 (consulta via tool calling em API mockada da WXN). Detalhes na
  seção 4.

---

## 2. Pontos de atenção e riscos (priorizados)

### 🔴 Alta prioridade — agir esta semana

| # | Risco | Por quê importa | Evidência |
|---|---|---|---|
| 1 | **SR1 é amanhã (24/09) e os slides ainda não estão prontos** | Apresentação de 5-7 min (Design + CC), câmeras abertas, FaCT de cada integrante até 23:59 do dia. Entrega fora do prazo vale ZERO. | Guia oficial do SR1. |
| 2 | **Gate de aprovação dos orientadores** | "Só projetos com potencial de entregas reais são autorizados" a seguir para a fase seguinte — risco estrutural, não só de nota. | Regra da disciplina (estrutura do semestre). |
| 3 | **Nenhuma funcionalidade do chatbot existe ainda** (0 entidades, 0 endpoints de negócio) | O PoC do SR2 exige "elementos funcionais de backend para 3 histórias do usuário". Hoje só existe um health check. | `docs/modelagem-inicial.md` confirma que nada da lista está implementado. |
| 4 | **Só 1 pessoa (Arthur) commitou até hoje**, apesar de ~8 integrantes | A nota individual do SR2 tem peso 4 em "Técnica"; sem commits próprios, o resto do grupo fica sem evidência individual. | 9 commits, todos de Arthur Reis; sem issues, sem board. |
| 5 | **FaCT** (avaliação individual) | Peso 3 tanto no SR1 quanto no SR2; **vale zero se não entregue no dia exato do SR**, mesmo enviado depois. Fácil de esquecer por não aparecer em nenhum board técnico. | Guia oficial do SR1. |
| 6 | **Declaração semanal de uso de IA aos orientadores** | Exigência formal **separada** da decisão do grupo de não citar IA em commits. Modelo: "A metodologia de execução do grupo foi otimizada através de [ferramentas], que permitiram focar o esforço humano na [atividade]...". | Regra da disciplina. |
| 7 | **Presença obrigatória na orientação semanal** | 1 falta = 4 faltas no Lyceum; limite total 15. | Regra da disciplina. |
| 8 | **Testes nunca rodam** (CI pula com `-DskipTests`) | Sem evidência automatizada de "requisitos técnicos obedecidos" (peso 4 em Técnica no SR2). | `deploy-backend.yml`; `BackendApplicationTests`. |
| 9 | **Sem autenticação/autorização**, repositório público, dados pessoais no caminho (telefone, mensagens — LGPD) | Qualquer endpoint de dados criado hoje ficaria aberto na internet. | Ausência de `spring-boot-starter-security` no `pom.xml`. |
| 10 | **Latência do banco em Ohio (EUA) enquanto o backend está em São Paulo** | Pode ser exatamente o "banco de serviço lento" citado pelo professor. Detalhado na seção 3. | Medições de hoje + documentação do Neon. |

### 🟡 Média prioridade

- **Schema gerado por `ddl-auto=update`, sem migrations versionadas** —
  gera drift entre bancos locais e produção; precisa de Flyway antes da 1ª
  entidade.
- **Endpoint "pooler" do Neon usado para tudo** — o pooler (PgBouncer em
  modo transação) não suporta certos comandos SQL; migrations devem usar o
  endpoint direto.
- **Health check não verifica o banco** — `/api/health` sempre responde
  "ok", mesmo se o Postgres estiver fora do ar.
- **Nome de entidade "User" colide com palavra reservada do PostgreSQL** —
  já previsto em `modelagem-inicial.md`; usar `EndUser`/tabela `end_user`.
- **Isolamento multi-tenant não modelado** — a WXN é multi-empresa; falta
  `company_id` obrigatório em todo lugar.
- **Gestão de segredos**: SCM basic auth habilitado no publish profile do
  Azure; senha de desenvolvimento (não de produção) versionada no
  `.env.example`; 7 dos 11 colaboradores do repo são admin.
- **Risco de exclusão do projeto Neon Free por inatividade prolongada** —
  não confirmado com precisão, mas sinalizado pela documentação do Neon;
  relevante por causa do intervalo de férias entre os dois semestres do
  projeto. Vale monitorar/reativar o projeto periodicamente.
- **F1 (Windows) é grátis hoje, mas um upgrade pago futuro custaria mais em
  Windows do que em Linux** — o F1 atual é gratuito nos dois sistemas, isso
  não é um problema agora. Mas a documentação oficial da Azure indica que o
  App Service **Linux também suporta Java 21** nativamente hoje (o grupo
  havia checado antes e visto só até Java 11 — vale reconferir com
  `az webapp list-runtimes`). Se confirmado, um upgrade futuro para B1
  sairia bem mais barato em Linux (~US$14,60/mês) do que em Windows
  (~US$66,80/mês, mesma região) — diferença grande para o crédito.
- **WhatsApp Business (fora do escopo do PoC, mas relevante depois)**: a
  partir de 01/10/2026 a Meta passa a cobrar também por mensagens "de
  serviço" dentro da janela de 24h (hoje gratuitas). Quem quiser continuar
  enviando essas mensagens sem interrupção precisa cadastrar um método de
  pagamento na conta WhatsApp Business até 30/09/2026. Não bloqueia o PoC
  (que usa um simulador de chat), mas é um prazo próximo para quando o
  canal real entrar em cena.
- **Orçamento de crédito Azure ao longo dos 2 semestres** — se o plano B do
  banco (Azure Database for PostgreSQL B1MS pago, ~US$32,54/mês sem a
  cobertura gratuita) precisar entrar em uso, ele consome boa parte dos
  US$100 de crédito em poucos meses. Vale decidir isso só se a alternativa
  atual (Neon reposicionado para São Paulo) não resolver.

### 🟢 Baixa prioridade / quick wins
- GitHub Actions desatualizadas (`checkout`, `setup-java`, `setup-node`) e
  um input inválido no deploy do frontend.
- Logs de SQL ligados em produção (`show-sql=true` vale para todos os
  ambientes).
- `develop` à frente da `main`; `main` local desatualizada.
- Ambiente local do Arthur só tem Java 8 (sem JDK 21/Maven/psql) e o Norton
  intercepta TLS — o ciclo de feedback depende do CI.
- `spring.threads.virtual.enabled=true` no `application.properties` — uma
  linha, Java 21, ajuda em chamadas I/O-bound (HTTP/LLM/WhatsApp).
- Trocar `Map.of("status","ok")` por um record `HealthResponse` no
  `HealthController`.
- `frontend/index.html`: trocar `<html lang="en">` por `lang="pt-BR"`.
- Corrigir a afirmação desatualizada em `docs/deploy.md` sobre o Azure não
  ter tier gratuito de banco (ver seção 1).

---

## 3. A dúvida do banco de dados — explicação simples

**O que o professor disse:** não recomendaria um "banco de dados de
serviço" porque seria muito lento para algo que precisa responder rápido —
mas a alternativa que ele indicou não ficou anotada.

**O que isso provavelmente significa:** "banco de serviço" parece descrever
um **banco gerenciado serverless que dorme quando ninguém usa e fica
hospedado longe do servidor** — exatamente o que o grupo tem hoje. Não é
necessariamente "banco gerenciado é ruim"; o problema mais provável é
**ficar longe + desligar sozinho**.

**Os números (medido vs. estimado):**
- O backend fica em **São Paulo** (Azure brazilsouth); o banco (Neon) fica
  em **Ohio, EUA** (AWS us-east-2).
- Medido hoje a partir de Recife (aproximação, não medido de dentro do
  próprio Azure): até Ohio, **122–129 ms**; até São Paulo, **64–70 ms**.
  Fontes públicas de latência entre nuvens (Azure↔AWS, São
  Paulo↔Ohio) apontam a mesma ordem de grandeza, **~120–140 ms** de ida e
  volta. *(Não confirmado o valor exato medido de dentro do Azure.)*
- O Neon Free desliga o banco após 5 min sem uso, e essa opção **não pode
  ser desativada no plano gratuito**. Religar leva "algumas centenas de
  milissegundos" segundo a documentação oficial, mais o tempo de reconectar
  — estimativa de **+0,8 a 1,2 s extras** no primeiro turno após período
  ocioso. *(Estimativa, não medida.)*
- Uma mensagem de chat com memória de conversa e handoff provavelmente faz
  **10 a 15 idas e voltas ao banco** (buscar usuário, buscar conversa,
  gravar mensagem, ler histórico, gravar resposta, atualizar status etc.).
  Com o banco em Ohio e aquecido, isso soma **~1,5–1,75 s** só de rede de
  banco por mensagem; com o banco frio, **~2,3–2,9 s**. *(Estimativa, não
  medida em produção.)*
- A meta de latência das próprias pesquisas do grupo é de até ~3 s antes de
  a resposta parecer lenta. Ou seja: **o banco sozinho pode consumir de 50%
  a quase 100% desse orçamento**, antes de somar a chamada a um LLM.
- Isso é **separado** do cold start de 28,2 s do App Service (o servidor
  Java acordando) — o `/api/health` medido nem consulta o banco.

**Recomendação de baixo custo (schema ainda vazio, migrar agora custa quase
nada):**
1. **Recriar o projeto Neon em `aws-sa-east-1` (São Paulo)** — mesma nuvem
   AWS, região certa. Não dá para mudar a região de um projeto existente,
   só criar um novo; como não há dados ainda, isso é barato. Custo: **US$
   0**, não toca o crédito Azure.
2. **Manter o contexto da conversa em memória** (cache simples dentro do
   próprio processo Java) em vez de ler/gravar no banco a cada turno,
   persistindo no Postgres de forma assíncrona.
3. Se, depois de confirmar com o professor, ficar claro que ele quis dizer
   "banco sempre ligado" (não só "banco mais perto"), a alternativa
   seguinte é o Azure Database for PostgreSQL Flexible Server (B1MS) em
   brazilsouth — possivelmente coberto pela oferta gratuita de 12 meses do
   Azure for Students (não confirmado; checar no Cost Management).

**Pergunta objetiva para levar ao professor:**
> "Hoje nosso banco é um Postgres serverless (Neon) que desliga depois de 5
> min sem uso e fica nos EUA, a uns 120–140 ms de distância do nosso
> servidor no Brasil. O senhor recomendaria um banco sempre ligado e na
> mesma região do servidor (por exemplo, Azure Database for PostgreSQL em
> Brazil South), ou se referia a outro tipo, como um cache em memória
> (Redis) ou um banco embarcado (H2)? E o problema é ser 'serverless e
> remoto', ou é qualquer banco gerenciado como serviço?"

---

## 4. Escopo da Prova de Conceito (PoC) do SR2

As 3 histórias abaixo foram escolhidas porque, avaliadas de três formas
diferentes (rigor dos critérios de aceite, risco/viabilidade até dezembro,
e coerência da jornada de demo), **as três análises chegaram exatamente ao
mesmo resultado**. Juntas cobrem 3 dos 4 MUST do MoSCoW e não dependem de
LLM, WhatsApp aprovado pela Meta, nem infraestrutura nova além do Postgres
que o grupo já tem.

### US 1.1 — Memória de conversa (pré-requisito das outras duas)
- `POST /api/chat/messages` recebe `{conversationId ou telefone, texto}`,
  resolve/cria a `Conversation` (com `companyId` obrigatório) e grava a
  `Message`; uma resposta a um turno seguinte usa corretamente algo dito
  antes, de forma verificável sem depender de LLM.
- Últimas N mensagens recuperáveis com teste de integração real (Postgres
  via Testcontainers, migrations Flyway executadas de verdade), cobrindo
  conversa vazia, com menos e com mais de N mensagens.
- Reenviar a mesma mensagem (simulando reenvio de webhook) não duplica
  registro, graças a uma constraint `external_id UNIQUE`.
- Duas conversas diferentes nunca compartilham contexto; adicionar mensagem
  a uma conversa já encerrada lança erro de domínio.
- Depois de "reiniciar" o processo (simulando o F1 descarregando por
  ociosidade), a próxima mensagem recupera o histórico do Postgres sem
  pedir para o usuário repetir o que já disse.

### US 1.4 — Classificação de urgência e encaminhamento humano
- Quando o bot não resolve (palavra-chave, falha de integração, ou
  intenção não reconhecida), cria-se um `Handoff` com urgência calculada
  por regra (testada com pelo menos 5 casos, incluindo falsos positivos),
  motivo e resumo automático.
- `GET /api/handoffs/queue` retorna a fila ordenada por urgência e depois
  data de criação.
- **Sem atendente online**, o usuário recebe informação de horário e o
  pedido é registrado mesmo assim — é literalmente o MUST do backlog.
- `POST /api/handoffs/{id}/assign` muda o status; tentar assumir/resolver
  um handoff já finalizado retorna erro (HTTP 409), nunca sobrescreve
  silenciosamente.
- A criação do handoff publica um evento de domínio, verificável em teste.

### US 3.1 — Consulta de status via tool calling numa API mockada da WXN
*(a única história marcada "(cliente WXN)" no backlog — a que mais mostra
valor real de negócio)*
- Contrato mínimo (OpenAPI: `GET /demandas/{protocolo}`, `POST /chamados`)
  e um controller/mock ativado por um perfil `mock`, no mesmo processo —
  sem custo de host extra.
- Protocolo válido → a pergunta sobre status aciona a ferramenta e devolve
  exatamente o dado do mock, sem inventar nada.
- Protocolo inexistente → o bot diz claramente que não encontrou e oferece
  encaminhamento humano.
- Erro/timeout simulado → mensagem amigável ao usuário, evento registrado
  em log.
- Ação que alteraria dados (abrir chamado) **só executa após confirmação
  explícita** — testado garantindo que, sem confirmação, a escrita nunca
  acontece.
- O `companyId` sempre vem do contexto do servidor, nunca do texto
  digitado — testado tentando "injetar" outro `company_id` na mensagem.

### Roteiro de demo (~2min40s–3min, contra o simulador de chat)
1. **Preparação (fora do tempo cronometrado)**: `GET /api/health` uns
   30–60s antes (cold start de 28,2s do F1); consulta trivial no banco
   também, se estiver ocioso.
2. **0:00–0:20** — Abertura: simulador no lugar do WhatsApp.
3. **0:20–0:55** — Status de um protocolo (US 3.1): ferramenta chamada,
   resposta real do mock, log como prova.
4. **0:55–1:20** — Segunda pergunta sem repetir o protocolo (US 1.1).
5. **1:20–1:50** — Urgente (US 1.4): classificação ALTA, aviso de
   encaminhamento (ou horário), handoff com resumo automático.
6. **1:50–2:20** — Fila de handoff: card no topo por urgência, "assumir",
   tentar assumir de novo → erro 409.
7. **2:20–2:40** (opcional) — Falha: protocolo inexistente + API mockada
   fora do ar, mensagem amigável.
8. **2:40–3:00** — Fechamento: histórico persistido, testes automatizados
   no CI.

### Fora de escopo do PoC (deliberadamente)
Webhook real do WhatsApp, US 2.3 (e-mail com protocolo), US 1.3 (envio de
arquivos), painel completo de métricas (a fila de handoff já entrega uma
versão mínima), RAG sobre o manual operacional, extração das ferramentas
para um servidor MCP, classificação por LLM (fica determinística por
regra, para ter resultado reprodutível em teste), autenticação completa de
usuários (API key simples nas rotas administrativas basta), migração de
banco para fora do Neon, integração real com a API da WXN (ainda não
existe — o adaptador fica pronto para trocar depois).

---

## 5. Arquitetura da PoC e stack recomendada

**Ideia central**: o dado que o chatbot mostra (status da demanda,
urgência) **sempre vem de uma regra determinística ou de uma chamada de
ferramenta contra o adaptador mockado da WXN — nunca do LLM**. Esse é o
guardrail mais importante da PoC.

### Fluxo de dados
`ChatSimulatorController` recebe a mensagem → `ConversationOrchestrator`
resolve/cria usuário e conversa (tenant obrigatório) → persiste a mensagem
→ monta contexto com últimas mensagens do Postgres (US 1.1) → roteador
decide entre (a) chamar a ferramenta que consulta a API mockada (US 3.1)
ou (b) classificar urgência e criar handoff quando não resolve (US 1.4) →
resposta persistida e devolvida.

### Stack (versões conferidas em 22–23/09/2026, via Spring Initializr e Maven Central)

| Camada | Escolha | Detalhe verificado |
|---|---|---|
| Framework | Spring Boot **4.1.1**, Java **21** | já é o padrão do repo |
| Migrations | `spring-boot-starter-flyway` + `flyway-database-postgresql` | no Boot 4 é obrigatório o starter, não basta `flyway-core`; trocar `ddl-auto` para `validate` antes da 1ª entidade |
| Observabilidade | `spring-boot-starter-actuator` | expor só `/actuator/health/readiness`, incluindo checagem do banco |
| Segurança mínima | `spring-boot-starter-security` | API key/HTTP Basic nas rotas administrativas; `/api/health` e o simulador liberados |
| Testes | `spring-boot-testcontainers` + `testcontainers-postgresql` (gerenciado pelo Boot 4.1.1) | `@DataJpaTest` com Postgres real, migrations Flyway reais |
| Mock da API WXN | Controller num perfil `mock` no próprio backend | mais simples com o ambiente atual do grupo (sem Maven/JDK local); WireMock fica para quando a WXN entregar um Swagger real; Postman mock server descartado (plano free de 1 usuário) |
| Banco | Neon Postgres 18, **recriado em `aws-sa-east-1` (São Paulo)** | ver seção 3; usar endpoint direto (não o pooler) para migrations |
| LLM (opcional, cosmético) | Google Gemini free tier, `spring-ai-starter-model-google-genai` + `spring-ai-bom 2.0.1` | Spring AI 2.0.1 confirmado GA e compatível com Boot 4.1.1. **Free tier usa os dados para treino/revisão humana — só dado fictício.** |
| RAG / MCP | **Fora do escopo do PoC** | não exigidos pelos critérios de aceite das 3 histórias; ficam para fase seguinte |

### Domínio (pacote `domain`)
`BaseEntity` (id, createdAt, updatedAt) → `TenantScopedEntity` (companyId)
→ `Company`, `EndUser` (tabela `end_user`, nunca `User`), `Conversation`
(enum `ConversationStatus` com transições válidas/inválidas), `Message`
(tipos USER/BOT/AGENT/SYSTEM), `Agent`, `Handoff` (urgency, status,
resumo).

### Migrations Flyway sugeridas
`V1` company/end_user · `V2` conversation/message (com `external_id
UNIQUE` e índice em `conversation_id, created_at`) · `V3` agent/handoff
(índice em `status, urgency`) · `V4` opcional (ToolInvocationLog, para
provar a chamada da ferramenta na demo) · `V-seed` dados fictícios.

### CI
Novo `.github/workflows/ci.yml` disparado em `pull_request` para
`develop`/`main`, rodando `./mvnw verify` (com Testcontainers) e `npm run
lint && npm run build` no frontend. Só depois disso remover o
`-DskipTests` do deploy.

---

## 6. Roadmap até o SR2

> **Datas estimadas**, com base na cadência observada (Semana 05 venceu
> 04/09, Semana 06 venceu 11/09, Semana 08 vence 24/09). **As datas exatas
> de SR1 e SR2 são desconhecidas e precisam ser confirmadas.** O SR1 é a
> fase de Descoberta/validação da ideia — a validação em si já começou nas
> Semanas 06/07; o SR2 é quem exige a PoC com "elementos funcionais de
> backend para 3 histórias". O trabalho técnico abaixo é **preparação
> antecipada do SR2, feita durante a janela de tempo do SR1** — não é
> conteúdo formal do SR1 em si.

| Quando (estimado) | Foco |
|---|---|
| **Amanhã, 24/09 (SR1)** | **Prioridade máxima**: deck do SR1 pronto e ensaiado (5-7 min, câmeras abertas, FaCT de cada integrante até 23:59). Em paralelo, começar entidades/migrations base do PoC. |
| ~01/10 | Entidades JPA + migrations V1–V3 prontas; `ChatSimulatorController` + `ConversationOrchestrator` funcionando de ponta a ponta para US 1.1; CI com Testcontainers no ar. |
| ~08/10 | US 1.4: `UrgencyClassifier`, `HandoffService`, fila de handoff; telas de Design (simulador de chat + fila) em média fidelidade. |
| ~15/10 | US 3.1: contrato OpenAPI mockado, adaptador `DemandaGateway`, tool calling, confirmação antes de alterar dados. |
| **SR1 (data a confirmar)** | Apresentação/validação formal — a proposta de solução já foi produzida nas Semanas 06/07; falta a entrega formal no formato que os orientadores pedirem. |
| ~22/10 | Integração fim a fim das 3 histórias juntas; segurança mínima (API key); migração do banco para São Paulo. |
| ~29/10 | Reforço de testes (unitários de domínio, `@WebMvcTest`, mock/WireMock para o adaptador); issues e board organizados; commits distribuídos entre os integrantes de CC. |
| ~05/11 a ~12/11 | Buffer para imprevistos; polimento do frontend; ensaio da demo com o roteiro da seção 4. |
| **SR2 (data a confirmar)** | PoC com as 3 histórias, backend testado de ponta a ponta. Aquecer backend e banco 30–60s antes da apresentação. |

---

## 7. Divisão de trabalho sugerida (~8 pessoas)

*(Assumindo 5 de CC, incluindo Arthur, e 3 de Design — confirmar a
composição real com o grupo.)*

### Ciência da Computação (5 pessoas)
| Papel | Entregável | Ligação |
|---|---|---|
| **Arthur — coordenador técnico** | Arquitetura, code review, CI, migrations, deploy, desbloqueio dos demais | Transversal |
| **Domínio & memória** | Entidades, migrations V1–V2, testes com Testcontainers | US 1.1 |
| **Handoff & orquestração** | `UrgencyClassifier`, `HandoffService`, eventos de domínio, fila | US 1.4 |
| **Integração WXN** | Contrato OpenAPI, adaptador mockado, tool calling, confirmação de ação | US 3.1 |
| **Qualidade & infraestrutura** | CI, segurança mínima, issues/board, proteção da `main`, migração do banco | Transversal |

### Design (3 pessoas)
| Papel | Entregável |
|---|---|
| **UI do simulador e da fila** | Telas em média fidelidade (exigido pelo SR2), em Figma e depois integradas ao React |
| **Conteúdo conversacional** | Tom de voz do bot, textos de confirmação/erro, roteiro da demo, apoio nas "4 perguntas" à WXN |
| **Pesquisa e validação da jornada** | Testar o fluxo com colegas, ajustar a jornada de demo, material de apresentação |

Se algum integrante for usar Eclipse (em vez de VS Code), usar **Spring
Tool Suite (STS)**, conforme orientação dos professores.

---

## 8. Próximos passos desta semana (checklist)

- [ ] **Hoje/amanhã**: terminar e ensaiar o deck do SR1 (5-7 min, Design + CC — em andamento no Figma). Prazo 24/09, vale zero se atrasar.
- [ ] Cada integrante envia o próprio FaCT até 23:59 do dia do SR1.
- [ ] Confirmar presença na orientação desta semana, câmeras abertas na apresentação.
- [ ] Preparar a declaração semanal de uso de IA para os orientadores.
- [ ] Confirmar o que o FaCT exige e planejar a entrega individual de cada integrante no dia do SR.
- [ ] Transformar as 9 user stories da Entrega 6 em issues do GitHub, com labels por épico, e criar um board.
- [ ] Distribuir os componentes da PoC entre os integrantes de CC (seção 7) — hoje só Arthur tem commits.
- [ ] Proteger a branch `main` (exigir 1 revisão em PR).
- [ ] Abrir PR de `develop` → `main` para levar `docs/escopo-chatbot.md`; avisar o grupo para atualizar a `main` local.
- [ ] Recriar o projeto Neon em `aws-sa-east-1` (São Paulo) e atualizar `DB_URL/DB_USER/DB_PASSWORD` no App Service (schema vazio, custo zero).
- [ ] Adicionar Flyway, criar `V1__baseline.sql`, trocar `ddl-auto` para `validate` antes da 1ª `@Entity`.
- [ ] Adicionar `spring-boot-starter-actuator` com `/actuator/health/readiness` checando o banco.
- [ ] Criar `.github/workflows/ci.yml` (`pull_request`) rodando `./mvnw verify` (Testcontainers) e `npm run lint && npm run build`.
- [ ] Atualizar GitHub Actions (`checkout@v7`, `setup-java@v6`, `setup-node@v7`) e remover o input inválido do deploy do frontend.
- [ ] Quick wins: `spring.threads.virtual.enabled=true`; `HealthResponse` record; `lang="pt-BR"` no `index.html`; corrigir a afirmação sobre free tier de banco em `docs/deploy.md`.
- [ ] Registrar/monitorar o risco de o projeto Neon Free ser excluído por inatividade prolongada (relevante nas férias entre semestres).
- [ ] Design: iniciar os wireframes do simulador de chat e da fila de handoff.
- [ ] Formular e enviar à WXN o pedido das "4 perguntas mais frequentes" e do contrato/Swagger da API de demandas.

---

## 9. Decisões pendentes e perguntas

### Para o professor
1. A pergunta objetiva sobre o banco de dados (texto pronto na seção 3).
2. Data exata do SR2 (o SR1 já está confirmado: amanhã, 24/09).
3. Se um PoC com classificação **determinística** (sem LLM real na tomada
   de decisão) atende ao que ele espera de "guardrails, RAG, tool calling"
   — ou se ele espera ver RAG/MCP funcionando de fato já nesta fase. Vale
   também levar essa dúvida a Raoni Monteiro (arquitetura com IA/LLM) e
   Victor Costa (engenharia de IA), que são orientadores do time com
   contexto direto nisso.

### Para a WXN
1. As 4 perguntas mais frequentes dos clientes (pedido combinado em 17/09,
   ainda não enviado/recebido).
2. O contrato real (Swagger/OpenAPI) da API de status de demandas — a PoC
   usa mock enquanto isso não chega.
3. Formato exato de um "protocolo"/demanda (campos, status possíveis).
4. Cronograma para o número real de WhatsApp Business ficar disponível
   para testes (fora do escopo do PoC atual).

### Decisões internas do grupo
- Confirmar a composição exata do time (CC x Design) para validar a
  divisão de trabalho da seção 7.
- Decidir se vale usar o Gemini free tier como camada cosmética de texto
  (implica cadastrar uma chave de API e usar só dado fictício) ou manter a
  PoC 100% determinística por enquanto.
- Decidir se, perto do SR1/SR2, vale subir temporariamente o App Service
  para o plano pago B1 durante a janela de demonstração (~US$6,59 por 3
  dias) para eliminar o cold start de 28,2s na hora da apresentação.
- Reconferir se o App Service Linux já suporta Java 21 de fato (a
  documentação oficial diz que sim) — se confirmado, um upgrade pago
  futuro sairia bem mais barato em Linux do que em Windows.
