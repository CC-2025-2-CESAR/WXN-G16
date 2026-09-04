# Semana 05 — Ambiente de desenvolvimento e de deploy

**Disciplina:** Computação — Projeto 3
**Grupo:** G16
**Projeto:** WXN Chatbot (atendimento automatizado para a WXN Tecnologia)

## Objetivo desta etapa

A entrega da Semana 05 pede a montagem dos ambientes de desenvolvimento e de
deploy do projeto: repositório Git, IDE, banco de dados e ambiente de
execução local/nuvem que hospedará o dashboard. Não é objetivo desta etapa
implementar as funcionalidades do chatbot ou do dashboard — apenas construir
a base técnica sobre a qual elas serão desenvolvidas nas próximas semanas.
Por isso, o restante deste texto separa claramente o que **foi configurado
agora** do que está **planejado para etapas futuras**.

## Repositório Git

O repositório (`WXN-G16`, na organização da turma no GitHub) foi organizado
como um **monorepo**: `backend/` (Spring Boot) e `frontend/` (React) vivem
lado a lado no mesmo repositório, junto de uma pasta `docs/` para a
documentação técnica do projeto. Optamos por monorepo em vez de repositórios
separados por ser mais simples de acompanhar em um projeto acadêmico de
grupo pequeno — um único `git clone` já traz o projeto inteiro, sem precisar
sincronizar versões entre repositórios diferentes.

## Estratégia de branches

Foi adotado um fluxo simples de três tipos de branch:

- `main` — representa sempre uma versão estável do projeto.
- `develop` — branch de integração, onde o trabalho do grupo é somado antes
  de ir para a `main`.
- `feature/*` — uma branch por funcionalidade, criada a partir da `develop`
  e mesclada de volta nela quando pronta.

O fluxo é `feature/* → develop → main`. Deliberadamente não foram adotados
processos mais elaborados (ex.: `release/*`, `hotfix/*`, revisão obrigatória
por múltiplos aprovadores) — são práticas comuns em times maiores, mas
desnecessárias para o tamanho e o ritmo deste grupo nesta fase do projeto.

## IDE

O projeto foi construído para funcionar igualmente bem em **VS Code** e em
**IntelliJ IDEA**, sem nenhuma configuração específica de uma IDE só. Isso
foi garantido de duas formas: (1) o backend usa o **Maven Wrapper**
(`mvnw`/`mvnw.cmd`), então qualquer IDE (ou terminal) builda o projeto sem
precisar de Maven instalado globalmente nem de configuração própria da IDE;
e (2) nenhum arquivo de projeto específico de IDE (`.idea/`, `.vscode/`
com configurações obrigatórias etc.) foi commitado — cada integrante abre a
pasta com a IDE de sua preferência.

## Backend

Criado com o **Spring Initializr oficial** (start.spring.io), garantindo uma
estrutura de projeto consistente com o que a comunidade Spring gera hoje.
Stack: **Java 21 (LTS)**, **Spring Boot 4.1**, **Maven**, com as dependências
Spring Web, Spring Data JPA, Validation e o driver JDBC do PostgreSQL. Foi
implementado um único endpoint, `GET /api/health`, que devolve
`{"status": "ok"}` — usado para comprovar que o backend está no ar e que o
frontend consegue falar com ele. A conexão com o PostgreSQL já está
configurada (`application.properties`), com valores padrão para
desenvolvimento local e possibilidade de sobrescrita por variável de
ambiente para produção.

**Planejado para etapas futuras:** as entidades de domínio (`Company`,
`User`, `Conversation`, `Message`, `Agent`, `Handoff` — documentadas em
`docs/modelagem-inicial.md`, mas ainda não implementadas), autenticação,
regras de negócio do atendimento, e a integração com IA (Spring AI, RAG,
Tool Calling, MCP), conforme o levantamento técnico já realizado pelo grupo.

## Frontend

Criado com **Vite** (`npm create vite`), usando **React 19** e
**TypeScript**. Contém uma única página de dashboard que consulta
`GET /api/health` no backend e exibe "Backend conectado" ou "Backend
indisponível", apenas para demonstrar a comunicação ponta a ponta entre as
duas aplicações. A estrutura de pastas (`components/`, `pages/`,
`services/`, `hooks/`, `types/`) já está preparada para crescer, mas
propositalmente não foi antecipada nenhuma tela do dashboard real, já que as
funcionalidades levantadas (fila de conversas, histórico, indicadores) ainda
são hipóteses a validar com a WXN.

## Banco de dados

**PostgreSQL**, rodando localmente durante o desenvolvimento (banco
`wxn_chatbot`). Não foi adotado `pgvector` nesta etapa — ele só será
necessário quando (e se) a etapa de RAG for implementada, mais adiante no
projeto. Não há, ainda, nenhuma tabela de domínio: o schema está vazio, e o
Hibernate está configurado em modo `update`, adequado apenas para esta fase
inicial sem dados reais.

## Ambiente local

Backend e frontend rodam de forma independente na máquina de cada
integrante: backend em `http://localhost:8080`, frontend em
`http://localhost:5173`, banco em `localhost:5432`. O passo a passo completo
de instalação e execução está no `README.md` do repositório.

## Ambiente de nuvem

O deploy foi realizado nesta etapa (03/09/2026), depois de uma pesquisa
(documentada em `docs/deploy.md`, com data e fontes) sobre onde hospedar o
projeto, considerando suporte a Java/Spring Boot, PostgreSQL, variáveis de
ambiente e custo compatível com um projeto universitário — e,
especificamente, em não consumir os créditos do Azure for Students que o
grupo já possui. A solução usada é **Azure App Service e Azure Static Web
Apps, nos planos gratuitos (F1 e Free)**, combinados com **Neon** para o
PostgreSQL: o Azure não oferece banco relacional gerenciado gratuito, então
mantê-lo fora do Azure é o que permite hospedar o projeto inteiro sem custo
algum. URLs e detalhes de como foi feito estão em `docs/deploy.md`.

## Justificativa geral das escolhas

As decisões desta etapa seguiram um princípio comum: **a solução mais
simples que cumpre o objetivo**, evitando complexidade que o grupo ainda não
precisa — por isso não foi usado Docker, não há microsserviços, e nenhuma
funcionalidade do chatbot foi antecipada. Ao mesmo tempo, as escolhas de
stack (Java/Spring Boot, React/TypeScript, PostgreSQL) foram feitas
pensando na arquitetura maior já estudada pelo grupo na Desk Research e no
Levantamento Técnico, para que os próximos passos (modelagem de dados,
Spring AI, RAG, MCP, integração com o ERP da WXN) se encaixem sem exigir
retrabalho da fundação criada agora.
