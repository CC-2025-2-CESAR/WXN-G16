# WhatsApp (Cloud API da Meta)

A Lily recebe e responde mensagens do WhatsApp pelo backend: a Meta chama um **webhook** (uma URL nossa) a cada mensagem
recebida, e o backend responde chamando a API de envio da Meta. O código está em
`backend/src/main/java/br/com/wxnchatbot/backend/canal/whatsapp`. Fica **desligado** até alguém configurar as variáveis abaixo.

O que acontece com uma mensagem: webhook assinado → `200` imediato → em segundo plano `ChatServico` (acha o cliente pelo
telefone e a conversa) → orquestrador → resposta enviada pelo WhatsApp. Veja `docs/chatbot-lily.md` para o miolo.

## Variáveis de ambiente

| Variável | Para que serve |
|---|---|
| `WHATSAPP_HABILITADO` | `true` liga o webhook e o envio. Com `true`, faltar qualquer uma das quatro abaixo impede o backend de subir |
| `WHATSAPP_TOKEN_DE_VERIFICACAO` | Texto que vocês inventam; a Meta o devolve na verificação do webhook |
| `WHATSAPP_SEGREDO_DO_APP` | "App Secret" do app (Configurações do app > Básico). Valida a assinatura de cada entrega |
| `WHATSAPP_TOKEN_DE_ACESSO` | Token para enviar mensagens |
| `WHATSAPP_ID_DO_NUMERO` | "Phone number ID" do número (não é o telefone) |
| `WHATSAPP_VERSAO_DA_API` | Versão da Graph API (padrão `v26.0`; a doc da Meta mostrava v26.0 e v27.0 em páginas diferentes, confira o changelog) |
| `CHAT_EMPRESA_PADRAO` | Empresa onde entram números sem cadastro. Vazio (padrão em produção) = o número desconhecido recebe "não encontrei o seu número no cadastro" |

Os segredos ficam só no ambiente (App Service do Azure ou variável local). Nunca no repositório, no chat nem no PR.

## Configurando a Meta

1. No Meta for Developers, crie um app do tipo Business e adicione o produto **WhatsApp**. O app já vem com um número de
   teste.
2. Em **API Setup**, cadastre os números que vão conversar com o de teste (o campo "To"; a Meta pede um código que chega
   pelo WhatsApp). O limite de 5 destinatários vem de fontes indiretas, confirme na tela.
3. Pegue o *Phone number ID* e um token. O token temporário do painel expira rápido. Para um token duradouro, crie um
   *usuário do sistema* em Business Settings, dê a ele o app e a conta do WhatsApp e gere um token com as permissões
   `whatsapp_business_messaging` e `whatsapp_business_management`.
4. Coloque as variáveis no Azure e **reinicie o backend antes de configurar o webhook**: a Meta verifica a URL na hora de salvar, e a
   verificação só passa com o token já configurado.
5. Em **Configuration > Webhook**, a URL de callback é `https://wxn-g16-backend.azurewebsites.net/webhook/whatsapp`, o token
   de verificação é o que vocês definiram, e assine o campo `messages`.
6. Mande uma mensagem de um dos números cadastrados para o número de teste.

## O que o backend garante

* Só aceita entregas assinadas com o App Secret (`X-Hub-Signature-256`, HMAC-SHA256 do corpo exatamente como chegou). Sem
  assinatura válida, `403`.
* Responde `200` na hora e processa depois: a Meta reenvia quando demora, e o Azure F1 leva uns 30 s para acordar.
* Ignora a mesma mensagem entregue duas vezes (pelo id `wamid`). A lista de ids fica em memória: uma reinicialização a perde.
* Divide respostas acima de 4096 caracteres (limite de texto da Meta).
* Casa o número da Meta com o cadastro mesmo com 55, sem o 55, com e sem o nono dígito. Para DDDs de 31 em diante a Meta
  costuma entregar o número sem o 9.

## Limitações

* **Só texto.** Áudio e imagem recebem o aviso "por enquanto só entendo mensagens de texto".
* **Usuários com username (sem telefone).** Desde abril de 2026 a Meta pode entregar a mensagem sem o telefone (só com um
  identificador do usuário). O backend ignora essas mensagens e registra um aviso no log. Suportar isso exige identificar o
  cliente por esse identificador e enviar com o campo `recipient`.
* **O atendente ainda não responde pelo WhatsApp.** A mensagem do cliente é gravada, mas a resposta do atendente não sai.
* **Janela de 24 h.** Texto livre só vale até 24 h depois da última mensagem do cliente; fora disso a Meta exige template
  aprovado, e o backend não envia templates.
* **Custo.** Segundo a documentação da Meta lida em 05/11/2026, desde 01/10/2026 mensagens de serviço (texto livre dentro da
  janela) são cobradas por mensagem entregue, com 1.000 gratuitas por mês por número, e quem não tem forma de pagamento deixa
  de entregar depois da franquia. A documentação não esclarece o número de teste. Não cadastrem cartão sem o grupo decidir.

## Testando sem a Meta

Dá para assinar uma entrega à mão e ver o webhook aceitar (a resposta só chega de verdade com um token válido da Meta; sem
ele o envio falha e o erro aparece no log):

```bash
corpo='{"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{"contacts":[{"profile":{"name":"Maria"},"wa_id":"5581999990101"}],"messages":[{"from":"5581999990101","id":"wamid.TESTE1","type":"text","text":{"body":"Oi"}}]}}]}]}'
assinatura="sha256=$(printf '%s' "$corpo" | openssl dgst -sha256 -hmac "$WHATSAPP_SEGREDO_DO_APP" | sed 's/^.* //')"
curl -i localhost:8080/webhook/whatsapp -H 'Content-Type: application/json' -H "X-Hub-Signature-256: $assinatura" -d "$corpo"
```

Sem o cabeçalho, ou com a assinatura errada, a resposta é `403`.
