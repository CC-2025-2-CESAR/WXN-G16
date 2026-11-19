# Telegram (Bot API)

A Lily também atende pelo Telegram. É o canal que dá para usar de verdade hoje: o bot é grátis, não precisa de empresa
verificada nem de cartão, funciona com número do Brasil e a gente cria em 2 minutos. (O WhatsApp da Meta ficou parado:
o número de teste não entrega para o Brasil, veja [`whatsapp.md`](whatsapp.md).)

O código está em `backend/src/main/java/br/com/wxnchatbot/backend/canal/telegram`. Fica **desligado** até alguém
configurar as variáveis abaixo.

> Os fatos da Bot API daqui (cabeçalho do segredo, limites, webhook x polling) foram conferidos na documentação oficial
> (<https://core.telegram.org/bots/api>, `/bots/webhooks` e `/bots/faq`) em 16/11/2026. O que a documentação não diz está
> marcado.

## Como funciona

```
Telegram ──webhook──▶ POST /webhook/telegram ──▶ ProcessadorDeAtualizacoesTelegram ──▶ ChatServico ──▶ Lily
   ▲                                                                                                   │
   └────────────────────────────── sendMessage (resposta) ◀──────────────────────────────────────────┘
```

- **Webhook:** o Telegram chama `POST /webhook/telegram` a cada mensagem. É o modo do Azure.
- **Polling:** o backend pergunta ao Telegram (`getUpdates`) se há mensagens. Serve para testar **na máquina**, sem
  endereço público.

O Telegram **não deixa usar os dois ao mesmo tempo no mesmo bot** (documentação: "two mutually exclusive ways"). Por isso
use **um bot só para testes locais** e outro para o Azure.

## 1. Criar o bot

1. No Telegram, abra uma conversa com o **@BotFather**.
2. Mande `/newbot`, escolha um nome (por exemplo "Lily WXN") e um *username* que termine em `bot`.
3. O BotFather devolve o **token** do bot, no formato `123456:ABC-DEF…`. **O token é a senha do bot.** Nunca no
   repositório, no chat do grupo nem em print. Se vazar, mande `/revoke` ao BotFather para trocar.

(Esse passo a passo do BotFather é o fluxo de sempre, mas a página da API não o descreve: confirme na conversa com ele.)

## 2. Variáveis de ambiente

| Variável | Para que serve |
|---|---|
| `TELEGRAM_HABILITADO` | `true` liga o canal. Com `true`, faltar o token (ou, no webhook, o segredo) impede o backend de subir |
| `TELEGRAM_TOKEN_DO_BOT` | O token do BotFather |
| `TELEGRAM_MODO` | `webhook` (padrão) ou `polling` |
| `TELEGRAM_SEGREDO_DO_WEBHOOK` | Só no modo webhook. Texto que **a gente inventa**: o Telegram o devolve em cada chamada, no cabeçalho `X-Telegram-Bot-Api-Secret-Token`, e o backend recusa (403) o que vier sem ele. De 1 a 256 caracteres, só `A-Z a-z 0-9 _ -` |

Os segredos ficam só no ambiente (App Service do Azure ou variável local). Para defini-los sem deixar o valor na tela nem
no histórico do PowerShell:

```powershell
function Ler-Segredo($nome) { $s = Read-Host $nome -AsSecureString; [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)) }
$env:TELEGRAM_TOKEN_DO_BOT = Ler-Segredo "Token do bot"
```

## 3. Testar na máquina (modo polling)

Com um bot **só de testes** (o token dele, não o do Azure):

```powershell
$env:TELEGRAM_HABILITADO = "true"
$env:TELEGRAM_MODO = "polling"
$env:TELEGRAM_TOKEN_DO_BOT = Ler-Segredo "Token do bot de testes"
.\mvnw.cmd spring-boot:run
```

Abra o bot no Telegram, mande `/start` e depois uma mensagem ("Qual o status da WXN-2026-0001?"). A resposta vem em
segundos. A conversa aparece na fila do dashboard (quando a Lily encaminha) com o selo **Telegram**.

- A API de chat e a empresa padrão (`Empresa Demonstração`) já vêm ligadas no perfil `dev`.
- Para a Lily responder com a Gemini, suba também com `CHATBOT_LLM=gemini` e a chave (veja
  [`chatbot-lily.md`](chatbot-lily.md)).
- Se o log mostrar `HTTP 409 ... webhook configurado`, esse bot ainda tem um webhook (de outro teste): use outro bot, ou
  remova o webhook com `deleteWebhook` (veja "Quando algo não funciona").

## 4. Ligar no Azure (modo webhook)

O Telegram só chama endereços **HTTPS com certificado válido**, nas portas 443, 80, 88 ou 8443 (IPv4). O Azure App
Service já atende isso: `https://wxn-g16-backend.azurewebsites.net/webhook/telegram`.

1. **Gere o segredo do webhook** (qualquer texto com os caracteres aceitos; o comando abaixo cria um e o copia para a área
   de transferência, sem imprimir):

   ```powershell
   $segredo = [guid]::NewGuid().ToString("N"); Set-Clipboard $segredo
   ```

2. No **portal do Azure**, no App Service `wxn-g16-backend`, em *Variáveis de ambiente*, crie:
   `TELEGRAM_HABILITADO=true`, `TELEGRAM_MODO=webhook`, `TELEGRAM_TOKEN_DO_BOT` (o token **do bot do Azure**) e
   `TELEGRAM_SEGREDO_DO_WEBHOOK` (cole o que está na área de transferência). Salve e deixe o app reiniciar.

3. **Empresa dos clientes do Telegram.** O Telegram não entrega telefone, e o cliente novo entra na empresa padrão. Em
   produção ela vem vazia (nesse caso quem escreve recebe "Não consegui liberar o seu atendimento"). Para a demonstração,
   defina também `CHAT_EMPRESA_PADRAO=Empresa Demonstração` e
   `CHAT_EMPRESA_PADRAO_ID=00000000-0000-0000-0000-000000000001`, ou deixe o App Service com `prod,demo`.

4. **Registre o webhook no Telegram** (uma vez, depois que o backend subiu; o token e o segredo são pedidos escondidos):

   ```powershell
   $token = Ler-Segredo "Token do bot"
   $segredo = Ler-Segredo "Segredo do webhook (o mesmo do App Service)"
   Invoke-RestMethod -Method Post -Uri "https://api.telegram.org/bot$token/setWebhook" -ContentType "application/json" -Body (@{ url = "https://wxn-g16-backend.azurewebsites.net/webhook/telegram"; secret_token = $segredo; allowed_updates = @("message") } | ConvertTo-Json)
   ```

   Deve responder `ok: True` e "Webhook was set". Para conferir depois:
   `Invoke-RestMethod "https://api.telegram.org/bot$token/getWebhookInfo"` (veja `url`, `pending_update_count` e
   `last_error_message`).

5. Mande `/start` ao bot. No plano grátis o backend dorme: o primeiro acesso leva uns 30 s, e o Telegram repete a
   entrega que não for respondida (a documentação diz "um número razoável de tentativas", sem dizer quantas).

## O que a Lily faz no Telegram

- `/start` recebe as boas-vindas, sem chamar a Lily.
- Texto vai para a Lily, igual ao WhatsApp e ao simulador (consultar protocolo, abrir chamado, dúvida, atendente).
- Foto, áudio, figurinha e arquivo recebem "por enquanto só entendo mensagens de texto".
- Só conversa **de um a um** com o bot: grupos, supergrupos e canais são ignorados. Mensagens de outros bots também.
- Respostas acima de 4096 caracteres (o limite do `sendMessage`) são divididas.
- O cliente é identificado pelo **id do chat** com o bot, guardado como `tg:<id>` no lugar do telefone (o Telegram só dá
  o telefone se a pessoa o compartilhar). O nome vem do nome da pessoa no Telegram. A fila mostra "Telegram #id".
- A conversa ganha a origem `TELEGRAM`, e o dashboard mostra o selo **Telegram**.

## O atendente responde pelo dashboard

Quando a Lily encaminha a conversa, ela entra na fila do dashboard (selo **Telegram**). O atendente a assume e escreve
no campo de mensagem do painel da conversa; o texto vai para o cliente pelo Telegram (`sendMessage` no chat dele) e fica
no histórico. As mensagens que o cliente mandar depois aparecem no painel em poucos segundos, e a Lily continua calada
até a conversa ser encerrada. A API é `POST/GET /api/conversas/{id}/atendimento/mensagens`
([contrato](api/fila-conversas.openapi.yaml)).

## Quando algo não funciona

| Sintoma | Causa provável |
|---|---|
| O bot não responde e o log do Azure mostra "Webhook do Telegram recusado: segredo ausente ou inválido" | `TELEGRAM_SEGREDO_DO_WEBHOOK` diferente do `secret_token` usado no `setWebhook`. Rode o `setWebhook` de novo com o mesmo segredo |
| O bot não responde e não há nada no log | O webhook não está registrado (veja `getWebhookInfo`), `TELEGRAM_HABILITADO` não está `true`, ou o app ainda está acordando |
| `getWebhookInfo` mostra `last_error_message` | O Telegram não conseguiu chamar o endereço (HTTPS inválido, backend fora do ar ou erro 5xx) |
| O backend não sobe depois de ligar o Telegram | Falta `TELEGRAM_TOKEN_DO_BOT` (ou o segredo, no webhook), ou o segredo tem caractere que o Telegram não aceita. A mensagem diz qual |
| Log do polling com `HTTP 409` | Esse bot tem um webhook ativo, ou outro processo lê as mensagens dele. Use outro bot, ou desligue o webhook: `Invoke-RestMethod "https://api.telegram.org/bot$token/deleteWebhook"` |
| O bot responde "Não consegui liberar o seu atendimento" | Não há empresa padrão configurada (passo 3 do Azure) |
| O bot responde "Desculpe, tive um problema…" | Erro no backend ao atender; veja o log (o texto da mensagem do cliente não é registrado) |
| Mensagem sem resposta depois de a conversa ir para um atendente | É assim: com a conversa na fila ou com um atendente, a Lily fica calada. Quem responde é o atendente, pelo dashboard (veja "O atendente responde pelo dashboard") |

## Limitações

- **A resposta do atendente chega ao Telegram, mas o cliente não é avisado se ela falhar do lado dele.** Se o Telegram
  recusar a entrega (por exemplo, o cliente bloqueou o bot), a mensagem continua no histórico e o dashboard avisa o
  atendente. No WhatsApp e no simulador a resposta do atendente ainda não sai.
- **Cada canal é um cliente diferente:** a mesma pessoa pelo Telegram e pelo WhatsApp aparece como dois clientes, porque
  a identificação do cliente vem do canal (telefone ou `tg:<id>`).
- **A ordem das mensagens e os repetidos:** o processamento é em segundo plano e a lista de atualizações já vistas fica em
  memória; uma reinicialização do backend a perde.
- **Privacidade:** as mensagens passam pelos servidores do Telegram. Use só dados fictícios nos testes, como no resto do
  projeto.
- **Limites do Telegram:** em uma conversa, evite mais de uma mensagem por segundo; o bot não passa de umas 30 por segundo
  no total (FAQ da Bot API). O backend só responde a quem escreveu antes. A documentação que li não diz o que acontece
  quando alguém bloqueia o bot nem se o bot pode puxar conversa: se o `sendMessage` falhar, o erro aparece no log.
- **Só texto**, sem formatação: o backend manda texto puro, sem Markdown.
