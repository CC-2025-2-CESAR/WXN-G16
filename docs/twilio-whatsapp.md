# WhatsApp pelo Twilio (para o MVP)

A Lily também atende pelo **WhatsApp do Twilio**, uma alternativa ao WhatsApp da Meta ([`whatsapp.md`](whatsapp.md)), que ficou
parado: o número de teste da Meta não entrega para o Brasil. O Twilio entra como solução **só para o MVP** (a orientação do
professor), para mostrar a Lily no WhatsApp de verdade; depois a gente ajusta (número próprio, empresa verificada etc.).

O código está em `backend/src/main/java/br/com/wxnchatbot/backend/canal/twilio`. Fica **desligado** até alguém configurar as
variáveis abaixo.

> **Atenção, risco real:** a página do sandbox do Twilio diz que o número de teste dele "pode ser temporariamente restrito de
> enviar para alguns países (por exemplo, Brasil ou Indonésia)". É parecido com o que travou a Meta. Só dá para saber testando
> com um celular brasileiro. Se o sandbox não entregar, o caminho passa a ser um *sender* próprio de WhatsApp no Twilio, que
> exige empresa verificada na Meta e pode custar. Decisão do grupo.

> Os fatos do Twilio daqui (sandbox, parâmetros do webhook, assinatura, API de envio, limites) foram conferidos na
> documentação oficial (<https://www.twilio.com/docs/whatsapp/sandbox>, `/docs/whatsapp/api`,
> `/docs/messaging/api/message-resource`, `/docs/messaging/guides/webhook-request`, `/docs/usage/security`) em 22/11/2026.
> O que a documentação não diz está marcado.

## Como funciona

```
Cliente ─WhatsApp─▶ Twilio ──POST /webhook/twilio/whatsapp──▶ confere a assinatura ──▶ ProcessadorDeMensagensTwilio ──▶ ChatServico ──▶ Lily
   ▲                                                           (200 + TwiML vazio na hora)                                                  │
   └───────────── WhatsApp ◀── Twilio ◀── POST /2010-04-01/Accounts/{sid}/Messages.json (resposta) ◀────────────────────────────────────────┘
```

- **Entrada:** o Twilio faz um `POST` (formulário `application/x-www-form-urlencoded`) a cada mensagem, com `MessageSid`,
  `From` (`whatsapp:+5581999990001`), `To`, `Body` (até 1600 caracteres), `ProfileName`, `NumMedia` e outros. O backend só
  aceita a chamada com uma assinatura `X-Twilio-Signature` válida, responde `200` com um TwiML vazio (`<Response/>`) e atende a
  mensagem em segundo plano.
- **Saída:** a resposta da Lily sai por uma chamada à API REST do Twilio (`To`, `From`, `Body`, autenticação básica com o
  Account SID e o Auth Token), dividida acima de 1600 caracteres. Não sai pelo corpo da resposta do webhook, porque a Lily pode
  levar segundos (Gemini, API da WXN).
- **Cliente:** identificado pelo telefone, igual ao WhatsApp da Meta (a Lily guarda só os dígitos, sem o 55 do Brasil). A
  conversa tem origem `WHATSAPP` e o dashboard mostra o selo "WhatsApp".
- **Atendente:** quando a Lily encaminha, a conversa vai para a fila; o atendente assume e **responde pelo dashboard**, e a
  mensagem chega ao WhatsApp do cliente pelo Twilio (veja [`telegram.md`](telegram.md#o-atendente-responde-pelo-dashboard) para
  a tela, que é a mesma).
- Mensagem repetida (o Twilio repete a chamada se não ouvir o `200` a tempo) é ignorada pelo `MessageSid`.

## Limites do sandbox (da documentação do Twilio)

- Quem recebe precisa ter **entrado no sandbox**: mandar `join <código do sandbox>` para o número do sandbox
  (`+14155238886`). O código aparece no console do Twilio. Quem não entrou recebe erro 63015.
- A entrada vale por **3 dias**; depois é preciso mandar o `join` de novo.
- **Janela de 24 horas:** depois que o cliente escreve, dá para responder com texto livre por 24 horas. Fora disso só com
  modelo aprovado, e o sandbox não aceita modelo próprio. Na prática: o atendente só consegue responder um cliente que
  escreveu nas últimas 24 horas (o dashboard mostra um aviso se o Twilio recusar, erro 63016).
- Uma mensagem a cada 3 segundos; é só para teste funcional, não para carga.
- Conta *trial* tem 100 mensagens de WhatsApp grátis; as mensagens do sandbox são cobradas no preço padrão.
- A documentação **não diz** se é preciso cartão de crédito para usar o sandbox.

## Variáveis de ambiente

| Variável | Para que serve |
|---|---|
| `TWILIO_HABILITADO` | `true` liga o canal. Com `true`, faltar qualquer uma das abaixo impede o backend de subir (a mensagem diz qual) |
| `TWILIO_ACCOUNT_SID` | O Account SID da conta (começa com `AC`) |
| `TWILIO_AUTH_TOKEN` | O Auth Token da conta. **É segredo**: nunca no repositório, no chat do grupo nem em print |
| `TWILIO_NUMERO_DE_ORIGEM` | O número que envia. No sandbox, `+14155238886` (com ou sem `whatsapp:`) |
| `TWILIO_URL_DO_WEBHOOK` | A URL **exata** que o Twilio chama, por exemplo `https://wxn-g16-backend.azurewebsites.net/webhook/twilio/whatsapp` |

A assinatura de cada chamada é calculada sobre a URL, e atrás do Azure o backend não tem como adivinhar o endereço público;
por isso ele vem da configuração. Ela precisa ser **igual**, caractere por caractere, à que está no console do Twilio. Não
coloque `?parametros` nessa URL.

## Passo a passo

1. **Criar a conta no Twilio** (quem for usar faz isso; eu não crio conta). No console, abra o sandbox de WhatsApp
   (*Messaging → Try it out → Send a WhatsApp message*, ou <https://www.twilio.com/console/sms/whatsapp/sandbox>).
2. **Entrar no sandbox** com o seu celular: mande `join <código>` para `+14155238886` e espere a confirmação.
3. **Pegar o Account SID e o Auth Token** no painel principal do console. Eles não vão para o chat.
4. **Configurar o webhook do sandbox:** em *Sandbox settings*, no campo **"When a message comes in"**, coloque a URL do
   `TWILIO_URL_DO_WEBHOOK`, método `POST`, e salve.
5. **Ligar no Azure.** Depois do deploy, grave as quatro variáveis no App Service `wxn-g16-backend` (a saída do `az` não pode
   listar os valores, e o Auth Token deve ser digitado escondido). A aplicação reinicia sozinha.
6. **Testar:** mande "Oi" do celular que entrou no sandbox. A primeira resposta pode levar uns 30 s se o app estiver dormindo
   (o Twilio repete a chamada; a repetição é ignorada).

Para testar **na máquina**, suba o backend com as quatro variáveis, exponha a porta com um túnel HTTPS (o Twilio só chama
endereços públicos) e use a URL do túnel no `TWILIO_URL_DO_WEBHOOK` e no console.

## Quando algo não funciona

| Sintoma | Causa provável |
|---|---|
| O log do backend mostra "Webhook do Twilio recusado: assinatura ausente ou inválida" | `TWILIO_URL_DO_WEBHOOK` diferente da URL do console (um `/` a mais, `http` no lugar de `https`, `?parametro`), ou `TWILIO_AUTH_TOKEN` errado |
| O backend não sobe depois de ligar o Twilio | Falta alguma variável, a URL não começa com `https://` ou o número de origem não é um número com DDI. A mensagem diz qual |
| O cliente manda mensagem e não chega nada no log | O webhook do sandbox não está configurado, o app ainda está acordando, ou o `TWILIO_HABILITADO` não está `true` |
| O cliente recebe a mensagem do `join` mas a Lily não responde, e no log aparece erro do Twilio | Veja o código no log: 63015 (cliente fora do sandbox), 63016 (fora da janela de 24 h), 401 (Account SID ou Auth Token errados) |
| O Twilio aceita o envio (`queued`) mas o celular brasileiro não recebe | Pode ser a restrição de país do sandbox (veja o aviso no começo). Olhe o status da mensagem no console do Twilio |
| O bot responde "Não consegui liberar o seu atendimento por aqui" | Não há empresa padrão configurada (`CHAT_EMPRESA_PADRAO`, ou o perfil `prod,demo`) |
| O atendente envia pelo dashboard e vê "A janela de 24 horas do WhatsApp fechou" | O cliente não escreve há mais de 24 h. É regra do WhatsApp, não do backend |

## O que a documentação do Twilio não diz (e como o backend se comporta)

- Se um `200` com TwiML vazio é a resposta esperada para mensagem **recebida** (a documentação só cita o `<Response/>` vazio
  para *status callbacks*). O backend responde assim, que é o padrão do Twilio.
- Em quantos segundos o webhook precisa responder, e quantas vezes o Twilio repete. O backend responde na hora e atende em
  segundo plano.
- O código HTTP de sucesso do envio (o backend aceita qualquer `2xx`).
- Os códigos de erro 63007, 21608, 21211 e 20003 (o backend repassa o código que vier no JSON de erro; só trata de forma
  especial o 63015, o 63016 e o HTTP 401).
- Se os valores do formulário entram na assinatura já decodificados ou não. O backend usa os decodificados, como no exemplo da
  documentação; o teste `AssinaturaDoTwilioTest` reproduz o exemplo oficial, e o teste de verdade é uma mensagem real.

## Limitações

- **Só texto.** Foto, áudio, figurinha e arquivo recebem "por enquanto só entendo mensagens de texto".
- **Sem mensagem iniciada pela empresa** (que exigiria modelo aprovado): o backend só responde a quem escreveu.
- **Um cliente por telefone**, e cada canal é um cliente diferente (o mesmo número no Telegram é outro cliente).
- **A lista de mensagens já vistas fica em memória**: uma reinicialização do backend a perde.
- **Privacidade:** as mensagens passam pelos servidores do Twilio e da Meta. Use só dados fictícios nos testes.
- Não há SMS: uma mensagem que não venha de `whatsapp:` é ignorada.
