# Ambiente de nuvem / deploy — pesquisa e recomendação

> **Status:** nada foi implantado ainda. Isto é uma recomendação para o
> grupo decidir e executar juntos — deploy é uma ação que expõe o projeto
> publicamente e cria contas em serviços de terceiros, então não foi feito
> automaticamente.
>
> Pesquisado em **31/08/2026** (revisado no mesmo dia para trocar a
> recomendação de compute para Azure). Planos gratuitos mudam com
> frequência — revalide na documentação oficial antes de criar as contas,
> principalmente se isso acontecer bem depois desta data.

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
| Backend (Spring Boot) | [Azure App Service](https://learn.microsoft.com/en-us/azure/app-service/overview-hosting-plans) (Linux, pilha Java) | **F1 – Free** | Não. US$ 0/mês, sempre. |
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

### Limites do plano Free do Azure a ter em mente

- **App Service F1 (backend):** 60 minutos de CPU **por dia** (compartilhado
  entre todos os apps Free do mesmo grupo/região na assinatura), 1 GB de
  RAM, 1 GB de armazenamento, sem "Always On" (o processo pode ficar ocioso
  e o próximo request "esquenta" de novo) — [Microsoft Learn – App Service plans](https://learn.microsoft.com/en-us/azure/app-service/overview-hosting-plans).
  O JVM usado é sempre 64-bit (Java 21 roda normalmente; a antiga limitação
  de 32-bit do F1 é do modelo Windows legado, não se aplica ao App Service
  Linux usado aqui) — [Microsoft Learn – Java on App Service](https://learn.microsoft.com/en-us/azure/app-service/configure-language-java-deploy-run).
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

## O que falta decidir antes de deployar de verdade

1. Confirmar com a WXN se há alguma exigência de hospedagem (ex.: dados não
   podem sair do Brasil, precisa ser em nuvem específica etc.) — nada disso
   apareceu nos documentos levantados até agora.
2. Ativar o **Azure for Students** com o e-mail acadêmico de quem for
   administrar a assinatura (ou verificar via GitHub Student Developer
   Pack) e criar o projeto no **Neon** com um e-mail do grupo — para o
   acesso não ficar preso a uma única pessoa.
3. Depois que o grupo aprovar esta recomendação, o deploy em si é:
   - Neon: criar um projeto → copiar a connection string → usar como
     `DB_URL`/`DB_USER`/`DB_PASSWORD` (lembrando do `?sslmode=require`
     mencionado acima).
   - Azure App Service: criar um "Web App" Linux, pilha **Java 21 (SE)**,
     tier **F1**; configurar `DB_URL`/`DB_USER`/`DB_PASSWORD`/
     `CORS_ALLOWED_ORIGINS` em Configuration → Application settings; fazer
     deploy do jar com o [Maven Plugin for Azure Web Apps](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/deploy-spring-boot-java-app-on-linux)
     (`mvn azure-webapp:deploy`, configurado em `backend/pom.xml` quando o
     grupo decidir seguir com o deploy) ou pelo botão de deploy do próprio
     portal.
   - Azure Static Web Apps: criar apontando para o repositório GitHub,
     pasta `frontend/`, build command `npm run build`, output location
     `dist`; configurar `VITE_API_URL` com a URL pública do App Service.
     O Static Web Apps configura CI/CD via GitHub Actions automaticamente.

Nenhuma dessas contas foi criada — isso fica para o grupo decidir e executar
com calma, fora do escopo desta entrega.
