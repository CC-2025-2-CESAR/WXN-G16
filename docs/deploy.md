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
`jdbc:postgresql://<host>.neon.tech/wxn_chatbot?sslmode=require`.

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
  UTC** (as requisições respondem HTTP 403 "Quota exceeded" nesse meio
  tempo) — isso é bem improvável no uso normal do grupo (abrir o dashboard
  algumas vezes por dia), mas evite deixar algo testando/pingando o backend
  em loop.
- **Static Web Apps (frontend):** 100 GB de banda por mês, 0,5 GB de
  armazenamento do app, 2 domínios customizados com SSL grátis — bem acima
  do que um projeto acadêmico consome.

Ou seja: a troca por Azure é parecida com o que já valia para o Render Free
(processo que pode "dormir"/esquentar de novo), só que sem custo nenhum de
crédito. Vale dar um "esquenta" no backend antes de uma demonstração ao
vivo, do mesmo jeito que já valeria com Render.

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
   Static Web App). Como o build precisa de JDK 21 (a máquina usada não
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
