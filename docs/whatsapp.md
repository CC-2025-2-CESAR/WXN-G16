# WhatsApp (Cloud API da Meta)

A Lily recebe e responde mensagens do WhatsApp pelo backend: a Meta chama um **webhook** (uma URL nossa) a cada mensagem
recebida, e o backend responde chamando a API de envio da Meta. O código está em
`backend/src/main/java/br/com/wxnchatbot/backend/canal/whatsapp`. Fica **desligado** até alguém configurar as variáveis abaixo.

O que acontece com uma mensagem: webhook assinado → `200` imediato → em segundo plano `ChatServico` (acha o cliente pelo
telefone e a conversa) → orquestrador → resposta enviada pelo WhatsApp. Veja `docs/chatbot-lily.md` para o miolo.

> Este documento foi conferido contra a documentação oficial da Meta em 05/11/2026. O que só aparece em fontes de terceiros
> está marcado como "reportado por terceiros": confirme na tela antes de contar com isso. Em 09/11/2026 uma tentativa real
> mostrou que o **número de teste da Meta não entrega para celulares do Brasil** (erro `130497`): veja "Limitações".

## Variáveis de ambiente

| Variável | Para que serve |
|---|---|
| `WHATSAPP_HABILITADO` | `true` liga o webhook e o envio. Com `true`, faltar qualquer uma das quatro abaixo impede o backend de subir |
| `WHATSAPP_TOKEN_DE_VERIFICACAO` | Texto que vocês inventam; a Meta o devolve na verificação do webhook |
| `WHATSAPP_SEGREDO_DO_APP` | "App secret" do app (Configurações do app > Básico). Valida a assinatura de cada entrega |
| `WHATSAPP_TOKEN_DE_ACESSO` | Token para enviar mensagens |
| `WHATSAPP_ID_DO_NUMERO` | "Phone number ID" do número. Não é o telefone nem o "Messaging account ID" (o antigo WABA ID): trocar os dois dá erro 100 |
| `WHATSAPP_VERSAO_DA_API` | Versão da Graph API. O padrão `v26.0` funciona. A Meta lançou a `v27.0` em 04/11/2026 e mantém cada versão por pelo menos dois anos; para subir basta trocar esta variável |
| `CHAT_EMPRESA_PADRAO` | Empresa onde entram números sem cadastro. Vazio (padrão em produção) = o número desconhecido recebe "não encontrei o seu número no cadastro" |
| `CHAT_EMPRESA_PADRAO_ID` | Id da empresa padrão; quando definido vale no lugar do nome. O perfil `dev` já define o id que a API simulada da WXN conhece |

Os segredos ficam só no ambiente (App Service do Azure ou variável local). Nunca no repositório, no chat nem no PR. Para
definir sem deixar o valor na tela nem no histórico do PowerShell:

```powershell
function Ler-Segredo($nome) { $s = Read-Host $nome -AsSecureString; [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)) }
$env:WHATSAPP_SEGREDO_DO_APP = Ler-Segredo "App secret"
$env:WHATSAPP_TOKEN_DE_ACESSO = Ler-Segredo "Token de acesso"
$env:WHATSAPP_ID_DO_NUMERO = Read-Host "Phone number ID"
$verificacao = [guid]::NewGuid().ToString("N"); $env:WHATSAPP_TOKEN_DE_VERIFICACAO = $verificacao; Set-Clipboard $verificacao
$env:WHATSAPP_HABILITADO = "true"
```

A linha do `WHATSAPP_TOKEN_DE_VERIFICACAO` gera um texto aleatório e o copia para a área de transferência (cole no campo
"Verify token" da Meta; ele não é impresso).

## Configurando a Meta

Para criar o app e o número de teste não precisa de CNPJ, de verificação de empresa nem de cartão: o número de teste da
Meta não exige forma de pagamento. Precisa de uma conta do Facebook, do registro como desenvolvedor no Meta for Developers
e de um *business portfolio* (a Meta oferece criar um na hora, durante a criação do app).

> **Atenção:** o número de teste é dos EUA (`+1 555…`) e, na tentativa de 09/11/2026, a Meta aceitou o envio mas a entrega
> a um celular brasileiro (DDD 81) falhou com `130497`. Dá para criar o app, gerar o token e fazer o backend aceitar
> entregas, mas **uma conversa de verdade com um telefone do Brasil não funciona com o número de teste**. Veja "Limitações".

No painel atual o app ganha um guia em três etapas: **Etapa 1 (Experimente)** usa o número de teste e é a única que este
roteiro usa; **Etapa 2 (Configuração da produção)** pede número próprio, conta de mensagens paga e forma de pagamento;
**Etapa 3 (Verificação da empresa)** pede documentos. Não faça as etapas 2 e 3 sem decisão do grupo.

1. No Meta for Developers, **Create App**: nome e e-mail, caso de uso **"Connect with customers through WhatsApp"**, escolha
   ou crie o *business portfolio* e confirme. (Os guias antigos falam em app "do tipo Business"; o fluxo atual escolhe o caso
   de uso.) Depois, **Customize use case > Connect on WhatsApp > Quickstart > Start using the API** leva à tela
   **API Setup**, onde se conecta (ou cria) a conta de mensagens. O app já vem com um número de teste.
2. Em **API Setup**, no campo **To**, cadastre os números que vão conversar com o de teste. Reportado por terceiros: a
   tela chama isso de "Manage phone number list", o número vai no formato `+5581...`, o dono recebe um código pelo WhatsApp
   e há um limite de 5 destinatários. A documentação oficial atual só diz "Add a To phone number": confirme na tela.
3. Anote o **Phone number ID** (campo **From**) e o **Messaging account ID** (é o antigo WABA ID; o nome do campo na API
   continua `waba_id`). Em **Configurações do app > Básico**, o **App secret** (botão "Mostrar").
4. Token de acesso:
   * **Generate access token**, em API Setup, dá um token temporário. A Meta só diz que "expira rápido" (terceiros falam
     em cerca de 24 h). Serve para o primeiro teste, não para uma demonstração no dia seguinte.
   * Token duradouro: em **Business Settings > Users > System users**, crie um usuário do sistema, **Assign assets** com o
     app em "Manage app" e a conta do WhatsApp em "Manage WhatsApp Business accounts", os dois com **Full control**
     (recarregue a página e confira; pode levar alguns minutos). **Generate token** com as permissões
     `whatsapp_business_messaging` e `whatsapp_business_management` (o guia da Meta também marca `business_management`,
     que raramente é necessária) e escolha a preferência de expiração (reportado por terceiros: 60 dias ou nunca; para uma
     demonstração, o que não expirar antes dela). Quem só mexe nos próprios ativos não precisa passar por App Review.
5. Defina as variáveis acima, **suba o backend** e deixe-o acessível por **HTTPS com certificado válido** antes de configurar
   o webhook: a Meta faz um `GET` de verificação na hora de salvar, e a verificação só passa com o token já configurado.
   (Para um backend que dorme, como o Azure F1, abra `/actuator/health` antes de salvar para acordá-lo: a documentação não
   diz quanto a Meta espera.)
6. Em **Use cases > Customize > Configuration** (em apps padrão: **WhatsApp > Configuration**), informe a **Callback URL**
   (`https://wxn-g16-backend.azurewebsites.net/webhook/whatsapp` no Azure, ou a do túnel, veja abaixo) e o **Verify token**, e
   clique em **Verify and save**. Depois da verificação aparecem os campos: assine **`messages`**.
7. Primeiro contato: em **API Setup**, **Send message** manda um modelo de exemplo (`hello_world`; o painel de 09/11/2026
   ofereceu `jaspers_market_order_confirmation_v1`) ao número do campo **To**. **Responda essa mensagem pelo WhatsApp**:
   isso abre a janela de 24 h e dispara o webhook. A Meta não diz se o número de teste aceita uma primeira mensagem
   escrita pelo cliente; comece pelo modelo. Para um destinatário no Brasil, o envio hoje falha com `130497` (veja
   "Limitações"), e nesse caso não há mensagem para responder.

### Testando na máquina, com um túnel

A Meta só chama URLs públicas em HTTPS. Para testar sem mexer no Azure, suba o backend local (perfil `dev`, porta 8080)
e abra um túnel para ele, por exemplo (serviço gratuito de terceiros, sem conta, não testado aqui; a URL muda a cada
execução, então reconfigure a Callback URL toda vez):

```powershell
ssh -R 80:localhost:8080 nokey@localhost.run
```

Use a URL `https://...` que ele mostrar, mais `/webhook/whatsapp`. Só mensagens de teste passam por ele.

## Quando algo não funciona

| Sintoma | Causa provável |
|---|---|
| "Verify and save" falha | O backend não está no ar, o token de verificação difere do configurado, a URL não é HTTPS válido ou o app dormia (F1) |
| Verificou, o botão **Test** chega, mas mensagem real não | O app não está assinado na conta do WhatsApp: confira com `GET https://graph.facebook.com/v26.0/<MESSAGING_ACCOUNT_ID>/subscribed_apps` (token do mesmo app) e, se vier vazio, assine com `POST` no mesmo endereço. Confira também se o campo `messages` foi assinado |
| Tudo retorna `403` no backend | `WHATSAPP_SEGREDO_DO_APP` errado (ou resetado na Meta). A Meta reenvia por até 7 dias, então chega rajada depois do conserto |
| Envio falha com `131030` | Destinatário fora da lista **To** do número de teste (reportado por terceiros; a página geral de erros da Meta não lista) |
| A Meta aceita o envio (volta um `wamid`), mas o aviso de status chega `failed` com `130497` | A conta está restrita de enviar a usuários de certos países. Visto com o número de teste dos EUA enviando a um celular do Brasil. Não é a lista **To** nem o nono dígito (o `wa_id` voltou sem o 9, como esperado para o DDD 81). Não se resolve só com configuração: veja "Limitações" |
| O assistente "Create your WhatsApp profile" reclama que a empresa não cumpriu a política e pede dados | É o fluxo de cadastrar **número próprio** (Etapa 2). Para o teste, feche o assistente e use o número de teste da tela **API Setup**. Se for mesmo cadastrar, preencha só dados verdadeiros da empresa (nome legal, endereço, telefone, site) em Configurações da empresa > Informações da empresa |
| Envio falha com `131047` | Mais de 24 h desde a última mensagem do cliente: só modelo aprovado |
| Envio falha com `190` | Token expirou (o temporário vale cerca de um dia): gere outro ou use o do usuário do sistema |
| Envio falha com `100` | `WHATSAPP_ID_DO_NUMERO` com o ID da conta de mensagens em vez do Phone number ID |
| O envio deu certo mas o cliente não recebeu | O `200` só diz que a Meta aceitou o pedido. A falha chega depois no webhook de status (`failed`, com o código); o backend hoje não registra status |
| Mensagem do seu próprio número é ignorada | Seu WhatsApp usa username e a Meta não entregou o telefone; veja Limitações |

## O que o backend garante

* Só aceita entregas assinadas com o App secret (`X-Hub-Signature-256`, HMAC-SHA256 do corpo exatamente como chegou). Sem
  assinatura válida, `403` (escolha nossa: a Meta manda responder algo diferente de `200`, que ela trata como falha e reenvia).
* Responde `200` na hora e processa depois: a Meta reenvia quando não recebe `200`, por até 7 dias e com frequência
  decrescente, e o Azure F1 leva uns 30 s para acordar. A documentação não diz o tempo limite de resposta; 5 s é o teto
  prático usado em outros produtos da Meta.
* Ignora a mesma mensagem entregue duas vezes (pelo id `wamid`). A lista de ids fica em memória: uma reinicialização a perde.
* Divide respostas acima de 4096 caracteres (limite de texto da Meta).
* Casa o número da Meta com o cadastro mesmo com 55, sem o 55, com e sem o nono dígito. A documentação só diz que, para
  Brasil e México, o número "pode ser modificado" pela API; terceiros relatam que de DDD 31 em diante ele chega sem o 9, e
  para o 81 (Recife) é o esperado: `558199990101`. Por isso o casamento é por variantes, não por regra fixa.

## Limitações

* **O número de teste da Meta não entrega para o Brasil (observado em 09/11/2026).** O envio de um modelo do número de
  teste (`+1`, EUA) para um celular de DDD 81 foi aceito pela API, e o aviso de status chegou `failed` com o erro `130497`
  ("Business account is restricted from messaging users in this country"). A documentação oficial da Meta só diz que a
  conta "está restrita de enviar a usuários de certos países" e remete à política de mensagens; **não cita o Brasil**.
  Reportado por terceiros (sem fonte da Meta, e sem falar de números de teste): empresas de fora do país têm o envio a
  destinatários no Brasil e na Indonésia limitado, para qualquer tipo de mensagem, até passarem por verificação de
  negócio e atingirem volume mínimo. Para funcionar com telefones brasileiros seria preciso um número de uma empresa
  brasileira verificada (Etapas 2 e 3 do painel), o que pede documentos, CNPJ e forma de pagamento; num produto real, a
  conta tem que ser da própria WXN. O canal real fica fora do PoC do SR2 (Sprint 5 do
  [`backlog-implementacao.md`](backlog-implementacao.md)). Enquanto isso, o backend se valida com o teste assinado
  ("Testando sem a Meta") e a demonstração usa o simulador de chat. Não confirmado: se uma resposta dentro da janela de
  24 h também seria barrada.
* **Só texto.** Áudio e imagem recebem o aviso "por enquanto só entendo mensagens de texto".
* **Usuários com username (sem telefone).** Desde abril de 2026 a Meta pode entregar a mensagem sem `from` nem `wa_id`
  (só com `from_user_id`, o identificador do usuário, e `profile.username`). O backend ignora essas mensagens e registra um
  aviso no log. Suportar exige ler `from_user_id`/`contacts[].user_id`, guardar o identificador (até 135 caracteres) e
  responder com o campo `recipient` no lugar de `to`.
* **O atendente ainda não responde pelo WhatsApp.** A mensagem do cliente é gravada, mas a resposta do atendente não sai.
* **Janela de 24 h.** Texto livre só vale até 24 h depois da última mensagem do cliente; fora disso a Meta exige modelo
  aprovado, e o backend não envia modelos.
* **Status das mensagens não são registrados.** Falhas de entrega (`failed`) chegam pelo webhook de status e hoje são
  descartadas.
* **Custo.** Pela página de preços da Meta (lida em 05/11/2026), desde 01/10/2026 cada número tem 1.000 mensagens de
  serviço (texto livre dentro da janela) gratuitas por mês, e a partir da 1.001ª elas são cobradas; sem forma de pagamento
  a entrega para de funcionar depois da franquia. Modelos de marketing, utilidade e autenticação são cobrados desde a
  primeira. O número de teste não exige forma de pagamento. Não cadastrem cartão sem o grupo decidir; se um dia cadastrarem,
  usem cartão virtual com limite e uma conta em BRL (a cobrança no Brasil passou a ser em reais).

## Testando sem a Meta

Dá para assinar uma entrega à mão e ver o webhook aceitar (a resposta só chega de verdade com um token válido da Meta; sem
ele o envio falha e o erro aparece no log):

```bash
corpo='{"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{"contacts":[{"profile":{"name":"Maria"},"wa_id":"558199990101"}],"messages":[{"from":"558199990101","id":"wamid.TESTE1","type":"text","text":{"body":"Oi"}}]}}]}]}'
assinatura="sha256=$(printf '%s' "$corpo" | openssl dgst -sha256 -hmac "$WHATSAPP_SEGREDO_DO_APP" | sed 's/^.* //')"
curl -i localhost:8080/webhook/whatsapp -H 'Content-Type: application/json' -H "X-Hub-Signature-256: $assinatura" -d "$corpo"
```

O número do exemplo está sem o nono dígito, como a Meta costuma entregar para o DDD 81; o backend casa com o cadastro dos
dois jeitos. Sem o cabeçalho, ou com a assinatura errada, a resposta é `403`.
