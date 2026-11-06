# Como a Lily decide o que fazer (intenção, triagem e encaminhamento)

Este documento descreve o caminho de uma mensagem, da entrada pela API (ou pelo WhatsApp) até a resposta, e como ligar
a LLM real. Código em `backend/src/main/java/br/com/wxnchatbot/backend/chatbot`, `service/ChatServico`,
`service/OrquestradorConversaServico` e `llm`.

## O caminho de uma mensagem

```
canal (API de chat ou webhook do WhatsApp)
  └─ ChatServico            acha o cliente pelo telefone, acha ou abre a conversa
       └─ OrquestradorConversaServico   grava a mensagem do cliente
            ├─ conversa com um atendente (ou na fila)? só grava; a Lily não responde
            └─ conversa com a Lily → RoteadorDeIntencao
                 ├─ tem um pedido de chamado esperando "sim"? → InterpretadorDeConfirmacao
                 └─ senão → ClassificadorDeIntencao → intenção → ação
```

O **orquestrador** é quem grava as mensagens e abre o ticket quando a Lily encaminha; o **roteador** só decide. A conversa
passa a `AGUARDANDO_ATENDENTE` pelo `TicketServico` (que calcula a urgência e o resumo), e entra na fila do dashboard.

## Intenções e ações

| Intenção | O que a Lily faz |
|---|---|
| `CONSULTAR_DEMANDA` | Acha o protocolo na mensagem (regex, nunca a LLM) e chama `ConsultaDemandaFerramenta`. Sem protocolo, pede. Se o cliente responde que não tem, encaminha |
| `ABRIR_CHAMADO` | Pede os detalhes (se a mensagem for curta), mostra título e descrição e pergunta "Posso confirmar?". **Só abre depois de um "sim" claro** |
| `DUVIDA` | Responde pela LLM, com o prompt da Lily mais a base de conhecimento (`resources/conhecimento/base-wxn.md`). Também cobre cumprimentos e agradecimentos |
| `FALAR_COM_HUMANO` | Responde e coloca a conversa na fila com um ticket |
| `FORA_DE_ESCOPO` | Recusa com gentileza, sem LLM |
| `INCERTO` | Encaminha para um atendente ("na dúvida, chame um humano") |

Abaixo do limiar de confiança (`chatbot.intencao.limiar-confianca`, 0,7) qualquer intenção vira `INCERTO`.

### Por que o chamado não passa pela LLM

Abrir um chamado grava na API da WXN. Por isso o "sim" é interpretado por regras (`InterpretadorDeConfirmacao`): só vale
uma mensagem feita *apenas* de palavras de concordância ("sim", "pode abrir", "isso mesmo"). "Ok, mas qual o prazo?" não
confirma; qualquer "não", "cancela" ou "deixa" cancela. O pedido pendente vale 10 minutos (`PedidosDeChamadoPendentes`).

## Os classificadores

`ClassificadorDeIntencao` tem duas implementações, escolhidas por `chatbot.llm`:

* **`fake`** (padrão): só `ClassificadorPorRegras` (palavras-chave) e `LlmClientFake` (resposta fixa). Não gasta cota.
* **`gemini`**: `ClassificadorComLlm`. As regras decidem sozinhas quando são claras (pedir atendente, citar protocolo,
  "abrir um chamado"), sem chamar a LLM. O resto vai ao Gemini, que devolve um JSON `{intencao, confianca}`. Se o Gemini
  falhar ou devolver algo inválido, valem as regras.

Dá para trocar por outro classificador (por exemplo um modelo próprio para decisões, como o Jev) implementando
`ClassificadorDeIntencao` e marcando o bean como `@Primary`. O roteador não muda.

## Configuração (variáveis de ambiente)

| Variável | Padrão | Para que serve |
|---|---|---|
| `CHATBOT_LLM` | `fake` | `fake` ou `gemini` |
| `GEMINI_API_KEY` | vazio | Chave do Google AI Studio. Obrigatória com `CHATBOT_LLM=gemini`. **Nunca vai para o repositório** |
| `GEMINI_MODELO` | `gemini-3.5-flash-lite` | Modelo usado |
| `GEMINI_NIVEL_DE_PENSAMENTO` | `low` | Nível de raciocínio do Gemini 3 (menos = mais rápido). Vazio desliga o campo |
| `CHAT_API_HABILITADA` | `false` (`true` no perfil `dev`) | Liga `POST /api/chat/mensagens` e `POST /api/conversas/{id}/mensagens` |
| `CHAT_EMPRESA_PADRAO` | vazio (`Empresa Demonstração` no perfil `dev`) | Empresa onde entram telefones ainda sem cadastro. Vazio = só atende telefones cadastrados |
| `CHAT_EMPRESA_PADRAO_ID` | vazio (`00000000-0000-0000-0000-000000000001` no perfil `dev`) | Id da empresa padrão. Quando definido, vale no lugar do nome e a empresa é criada com esse id se não existir. No `dev` é o id que a API simulada da WXN conhece |

A API de chat não tem autenticação e cada chamada pode gastar a cota da LLM, por isso fica desligada em produção. No perfil
`prod,demo` ela fica ligada (o `demo` traz o `dev`): desligue com `CHAT_API_HABILITADA=false` se for expor com Gemini.

## Testando pela API

Com o backend em `dev` (`./mvnw spring-boot:run`):

```bash
curl -s localhost:8080/api/chat/mensagens -H 'Content-Type: application/json' \
  -d '{"telefone":"81999990101","nome":"Maria","texto":"Qual o status da WXN-2026-0001?"}'
# {"conversaId":"...","resposta":"Demanda WXN-2026-0001 - ... Status: em andamento ...","status":"COM_CHATBOT"}

curl -s localhost:8080/api/chat/mensagens -H 'Content-Type: application/json' \
  -d '{"telefone":"81999990101","texto":"Quero falar com um atendente"}'
# status AGUARDANDO_ATENDENTE: a conversa já aparece em GET /api/fila
```

A resposta traz `"resposta": null` quando a conversa está com um atendente ou na fila: a Lily não fala, e a mensagem do
cliente só fica gravada.

Os protocolos de exemplo (`WXN-2026-0001` a `0003`) só aparecem para clientes da empresa de id
`00000000-0000-0000-0000-000000000001`, a empresa padrão do perfil `dev`. Um telefone já cadastrado em outra empresa
(por exemplo, os da demonstração criada antes dessa configuração existir) recebe "não encontrei a demanda"; veja
[`docs/integracao-wxn.md`](integracao-wxn.md).

## Limitações conhecidas

* O atendente ainda não responde ao cliente pelo canal: a mensagem do atendente não sai pelo WhatsApp.
* A base de conhecimento tem só o que o repositório já documenta. Falta o manual da WXN (horários, prazos, preços).
* O pedido de chamado pendente e a lista de mensagens do WhatsApp já vistas ficam em memória: reiniciar o backend os perde.
* O limiar 0,7 e o texto do prompt de classificação são pontos de partida. Calibre com mensagens reais em português
  (tarefa IA-07 do backlog).
