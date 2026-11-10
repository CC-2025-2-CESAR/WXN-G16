# Ambiente de nuvem / deploy — pesquisa, recomendação e status

> **Status: implantado em 03/09/2026.** URLs de produção:
> - Frontend: <https://happy-desert-0e6abd410.5.azurestaticapps.net>
> - Backend: <https://wxn-g16-backend.azurewebsites.net/api/health>
>
> Recursos criados na assinatura Azure for Students (grupo de recursos
> `rg-wxn-chatbot`, região `Brazil South` para o backend — a assinatura do
> grupo está restrita a `centralus`, `mexicocentral`, `brazilsouth`,
> `chilecentral`, `canadacentral` por política; o frontend ficou em
> `centralus` porque Static Web Apps não está disponível nas demais). Banco
> em um projeto Neon separado (fora do Azure — ver justificativa abaixo).
>
> Pesquisado em **31/08/2026**, implantado em **03/09/2026**. Planos
> gratuitos mudam com frequência — revalide na documentação oficial antes
> de recriar/alterar os recursos, principalmente se isso acontecer bem
> depois desta data.

## O que precisamos hospedar

- Frontend React (build estático via Vite)
- Backend Spring Boot (processo Java de longa duração)
- PostgreSQL

## Recomendação: Azure (frontend + backend, planos que não custam nada) + Neon (PostgreSQL)

O grupo tem créditos do **Azure for Students** (US$ 100, válidos por 12
meses) e quer guardá-los em vez de gastar com hospedagem — então a ideia
não é "o plano mais barato do Azure", e sim **os planos do Azure que são
genuinamente gratuitos** (SKUs "Free", cobrança zero, não descontam do
saldo de créditos):

| Componente | Serviço | Plano | Consome crédito? |
|---|---|---|---|
| Backend (Spring Boot) | [Azure App Service](https://learn.microsoft.com/en-us/azure/app-service/overview-hosting-plans) (Windows, pilha Java) | **F1 – Free** | Não. US$ 0/mês, sempre. |
| Frontend (estático) | [Azure Static Web Apps](https://azure.microsoft.com/en-us/pricing/details/app-service/static/) | **Free** | Não. US$ 0/mês, sempre. |
| Banco de dados | [Neon](https://neon.com/) (PostgreSQL gerenciado, fora do Azure) | Free | Não é Azure — US$ 0, sem prazo de expiração. |

Como nenhuma dessas três peças cobra nada, **o projeto inteiro roda sem
tocar no crédito do Azure for Students** — o que é melhor até do que "o
plano mais barato": mesmo o SKU pago mais barato de App Service (tier
Basic B1) teria custo mensal recorrente e comeria o crédito ao longo do
semestre. Com tudo em Free, o crédito fica de reserva para quando (e se) o
projeto precisar de algo que não tenha tier gratuito — ex.: mais CPU/RAM
para a etapa de IA, mais adiante.

### Por que o banco continua sendo o Neon, e não o Azure Database for PostgreSQL

O Azure **não tem** tier gratuito para banco de dados relacional gerenciado
— o Azure Database for PostgreSQL Flexible Server é cobrado por hora desde
o SKU mais barato (Burstable B1ms), então usá-lo consumiria os créditos de
estudante continuamente, o oposto do que foi pedido. O
[Neon](https://neon.com/) continua sendo a melhor opção porque roda fora do
Azure, é PostgreSQL de verdade, e o plano free **não expira** ("no time
limits, no credit card required") — [Neon Free Tier FAQ](https://neon.com/faqs/managed-postgres-databases-free-tier).
Não há problema técnico em misturar provedores assim (Azure hospedando o
código, Neon hospedando o banco): o backend só precisa de uma
connection string, e para o Neon ela é padrão JDBC.

O Neon exige SSL, então o `DB_URL` de produção precisa terminar com
`?sslmode=require`, diferente do banco local (que não usa SSL) — ex.:
`jdbc:postgresql://<host>.neon.tech/wxn_chatbot?sslmode=require`. Use o
endereço **direto** do Neon (host sem `-pooler`): o pooler roda em modo
transação e não é indicado para as migrations do Flyway.

### Por que Windows, e não Linux, no App Service

O plano original era Linux (mais comum para Java). Na hora de criar o
recurso de verdade, `az webapp list-runtimes --os-type linux --runtime
java` mostrou que a pilha Java **built-in do App Service Linux vai só até a
versão 11** hoje — não tem 17 nem 21. Como o projeto (Spring Boot 4.1)
exige Java 17+, isso inviabilizava Linux sem usar um container Docker
(que o grupo decidiu não usar nesta etapa). A mesma checagem no Windows
(`--os-type windows`) mostrou Java 21 e até 25 como pilhas ativas — por
isso o backend foi criado como **App Service Windows**, runtime `JAVA:21`.
O JVM usado é sempre 64-bit (a limitação de 32-bit do F1 é de um modelo
ainda mais antigo, não se aplica aqui) — [Microsoft Learn – Java on App Service](https://learn.microsoft.com/en-us/azure/app-service/configure-language-java-deploy-run).

### Limites do plano Free do Azure a ter em mente

- **App Service F1 (backend):** 60 minutos de CPU **por dia** (compartilhado
  entre todos os apps Free do mesmo grupo/região na assinatura), 1 GB de
  RAM, 1 GB de armazenamento, sem "Always On" (o processo pode ficar ocioso
  e o próximo request "esquenta" de novo) — [Microsoft Learn – App Service plans](https://learn.microsoft.com/en-us/azure/app-service/overview-hosting-plans).
  **Se os 60 minutos de CPU do dia acabarem, o Azure para o app até meia-noite
  UTC** (21h em Brasília; as requisições respondem HTTP 403 "This web app is
  stopped" nesse meio tempo). Também existe um limite curto, por janela de 5
  minutos, e o app pode ser parado e religado sozinho. No uso normal do grupo
  (abrir o dashboard algumas vezes por dia) isso não acontece, mas acontece
  se o app cair ao subir e o Azure ficar reiniciando (foi o que ocorreu em
  03/11/2026), e também se algo ficar testando/pingando o backend em loop.
- **Static Web Apps (frontend):** 100 GB de banda por mês, 0,5 GB de
  armazenamento do app, 2 domínios customizados com SSL grátis — bem acima
  do que um projeto acadêmico consome.

Ou seja: a troca por Azure é parecida com o que já valia para o Render Free
(processo que pode "dormir"/esquentar de novo), só que sem custo nenhum de
crédito. Vale dar um "esquenta" no backend antes de uma demonstração ao
vivo, do mesmo jeito que já valeria com Render.

## Configuração de produção (App Service)

Configurações do `wxn-g16-backend` (portal → Environment variables → App
settings). Os valores com `<...>` são segredos ou endereços do grupo e nunca
entram no repositório:

| Nome | Valor | Observação |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Sem ele o backend sobe como `dev` (o perfil padrão), expõe o `/mock/wxn` sem senha e imprime o SQL no log |
| `DB_URL` | `jdbc:postgresql://<host direto>.neon.tech/<banco>?sslmode=require` | Endereço direto do Neon, sem `-pooler` |
| `DB_USER`, `DB_PASSWORD` | `<credenciais do Neon>` | |
| `CORS_ALLOWED_ORIGINS` | `<URL do Static Web App>` | |
| `WXN_API_BASE_URL` | `https://api-wxn.invalid` | A API da WXN ainda não existe. O `.invalid` nunca resolve, então as consultas viram "serviço indisponível". Trocar pela URL real quando ela existir |

O perfil `prod` não traz valores padrão de desenvolvimento. Salvar uma
configuração no portal reinicia o app (1 a 2 minutos).

### Chatbot, Gemini e WhatsApp (opcionais)

Sem nenhuma destas, o backend responde com a LLM de mentira e o chat pela API e o WhatsApp ficam desligados. Detalhes em
`docs/chatbot-lily.md` e `docs/whatsapp.md`.

| Nome | Valor | Observação |
|---|---|---|
| `CHATBOT_LLM` | `gemini` | Liga o Gemini. Exige `GEMINI_API_KEY`; sem ela o backend não sobe |
| `GEMINI_API_KEY` | `<chave do Google AI Studio>` | Segredo, só aqui no portal |
| `CHAT_API_HABILITADA` | `false` | Já é o padrão em `prod`. Com `prod,demo` o chat pela API fica ligado (o `demo` traz o `dev`): coloque `false` se for deixar o Gemini ligado |
| `CHAT_EMPRESA_PADRAO` | vazio | Empresa onde entram números sem cadastro; vazio = só atende números cadastrados |
| `CHAT_EMPRESA_PADRAO_ID` | vazio | Id da empresa padrão; quando definido vale no lugar do nome. No `prod,demo` o perfil `dev` já define o id que a API simulada conhece |
| `WHATSAPP_HABILITADO` | `true` | Exige as quatro `WHATSAPP_*` abaixo |
| `WHATSAPP_TOKEN_DE_VERIFICACAO`, `WHATSAPP_SEGREDO_DO_APP`, `WHATSAPP_TOKEN_DE_ACESSO`, `WHATSAPP_ID_DO_NUMERO` | `<da Meta>` | Segredos; veja `docs/whatsapp.md` |

O limite do Gemini no plano grátis (15 requisições por minuto no `gemini-3.5-flash-lite`, ver `docs/spike-llm.md`) vale para o
projeto inteiro. Pedir atendente e citar protocolo não gastam requisição, mas cada dúvida gasta uma ou duas.

## Banco (Neon) e o Flyway

- As tabelas são criadas pelo Flyway (`backend/src/main/resources/db/migration`)
  e o Hibernate só valida (`ddl-auto=validate`). O banco de produção só pode
  ter tabelas criadas por ele.
- Se o backend cair ao subir com `Found non-empty schema(s) "public" but no
  schema history table`, o banco tem tabelas que o Flyway não criou. Foi o
  que aconteceu em 03/11/2026: o Neon tinha tabelas do Hibernate da época do
  `ddl-auto=update`, e a primeira promoção com Flyway para a `main` derrubou o
  app. Receita:
  1. Conferir no SQL Editor do Neon que são só dados de teste (contar as linhas
     de cada tabela em `public`).
  2. Apagar as tabelas, por exemplo `DROP TABLE IF EXISTS mensagem, ticket,
     conversa, cliente, chatbot, atendente CASCADE;` (isto apaga os dados).
  3. Reiniciar o app uma vez. O Flyway cria tudo do zero.
  4. Conferir `select version, success from flyway_schema_history order by
     installed_rank;`.
- Não use `spring.flyway.baseline-on-migrate` nem desligue o Flyway para
  "contornar": foi testado e deixa o banco inconsistente (o Hibernate acusa
  coluna faltando).
- Numeração das migrations: a nova é o maior número entre a `main` e a
  `develop` mais 1, e quem mescla depois renumera antes do merge. Migration
  já mesclada não se edita. Se o banco já aplicou V4 e chega uma V2, o Flyway
  se recusa a subir.

## Quando o site cai

- Ver o estado: `az webapp show -g rg-wxn-chatbot -n wxn-g16-backend --query
  "{state:state, usageState:usageState}"`. `QuotaExceeded` e `Exceeded`
  significam que a cota de CPU do plano gratuito acabou (a página responde 403
  "This web app is stopped"). Normalmente é consequência de o app cair ao
  subir; corrija a causa em vez de ficar reiniciando.
- Não fique abrindo a URL nem reiniciando: cada acesso a um app parado o
  religa e gasta cota.
- Para verificar depois do conserto: `GET /api/health` deve responder
  `{"status":"ok"}` (a primeira chamada pode levar uns 30 s) e
  `GET /actuator/health/readiness` confere também o banco.
- O Azure CLI atrás do Norton precisa de `REQUESTS_CA_BUNDLE` apontando para
  o certificado do antivírus, senão dá `CERTIFICATE_VERIFY_FAILED`.
- Os logs da aplicação não ficam ligados no App Service; sem eles, o erro de
  subida só aparece reproduzindo localmente.

## Promoção para a `main`

A `main` só recebe da `develop`, por PR. Cada push na `main` que muda
`backend/` publica em produção, e o deploy fica verde mesmo se o app não subir
(ele só entrega o pacote). Antes de promover:

1. Conferir as migrations novas contra o schema de produção.
2. Conferir as configurações da tabela acima.
3. Depois do deploy, abrir `/api/health` uma vez.

## Alternativas consideradas

- **Render** (recomendação anterior deste documento). Continua sendo uma
  opção sólida — Web Service Free (750h/mês) + Static Site Free — mas não
  ajuda a preservar o crédito do Azure for Students, que é o critério que o
  grupo priorizou agora. Fica registrado como plano B caso o grupo prefira
  não depender do Azure.
- **Railway.** Ótima experiência de deploy, mas hoje o plano gratuito
  contínuo dá apenas **US$ 1 de crédito por mês** (o trial inicial de US$ 5
  expira em 30 dias); um backend rodando 24/7 tende a consumir mais que
  isso — [Railway Docs – Free Trial](https://docs.railway.com/pricing/free-trial").
  Descartado pelo mesmo motivo do Render: não usa o crédito do Azure.
- **Azure Database for PostgreSQL** (em vez do Neon). Descartado — ver
  seção acima; é cobrado por hora, não tem tier free.

## Como foi feito (registro do deploy real, 03/09/2026)

Recursos criados via Azure CLI (`az`), na assinatura Azure for Students já
logada, dentro do grupo de recursos `rg-wxn-chatbot`:

1. **Resource group** `rg-wxn-chatbot` em `brazilsouth`.
2. **App Service Plan** `plan-wxn-chatbot` — Windows, SKU `F1`, `brazilsouth`.
3. **Web App** `wxn-g16-backend` — runtime `JAVA:21`. App settings
   configuradas (`DB_URL`, `DB_USER`, `DB_PASSWORD` apontando para o
   projeto Neon do grupo, `CORS_ALLOWED_ORIGINS` apontando para a URL do
   Static Web App; depois entraram `SPRING_PROFILES_ACTIVE` e
   `WXN_API_BASE_URL`, ver "Configuração de produção"). Como o build precisa de JDK 21 (a máquina usada não
   tinha) e o Azure App Service Linux não oferecia Java 17/21 de verdade,
   o build+deploy roda por **GitHub Actions** —
   [`.github/workflows/deploy-backend.yml`](../.github/workflows/deploy-backend.yml) —
   a cada push em `backend/`, usando o publish profile do Web App guardado
   como secret do repositório (`AZURE_WEBAPP_PUBLISH_PROFILE`), nunca
   commitado. **Pegadinha encontrada:** Web Apps novos vêm com as
   credenciais de publish (SCM Basic Auth) desabilitadas por padrão — foi
   preciso habilitar (`basicPublishingCredentialsPolicies/scm`) antes do
   primeiro deploy funcionar.
4. **Static Web App** `wxn-g16-frontend` — `centralus` (única região do
   Static Web Apps permitida pela política de região da assinatura, que
   restringe a `centralus`, `mexicocentral`, `brazilsouth`, `chilecentral`,
   `canadacentral`). Build feito localmente (`npm run build`, com
   `VITE_API_URL` apontando para a URL do backend) e publicado com o
   [Azure Static Web Apps CLI](https://github.com/Azure/static-web-apps-cli)
   via token de deployment — não foi conectado ao GitHub Actions ainda
   (fica como próximo passo, se o grupo quiser deploy automático do
   frontend também a cada push).
5. **Neon**: projeto `WXN` criado pelo grupo (fora do Azure), connection
   string usada diretamente nas app settings do backend.

Verificado depois do deploy: `GET /api/health` no backend responde
`{"status":"ok"}`, e o frontend mostra "Backend conectado" — ver as URLs no
topo deste documento.

### Pendências conhecidas para depois desta entrega

- Confirmar com a WXN se há alguma exigência de hospedagem (dados não saírem
  do Brasil, nuvem específica etc.) — nada disso apareceu nos documentos
  levantados até agora, e o backend ficou no Brasil (`brazilsouth`); o
  frontend e o banco (Neon, `us-east-2`) não.
- Automatizar o deploy do frontend também via GitHub Actions (hoje é manual).
- Trocar o e-mail/conta usada para o Neon e para o Azure por algo do grupo,
  se o projeto crescer além desta entrega, para o acesso não ficar preso a
  uma única pessoa.
