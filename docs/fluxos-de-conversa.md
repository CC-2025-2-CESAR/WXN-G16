# Fluxos de conversa da Lily

Este documento mostra, com exemplos de mensagens, o que a Lily faz em cada situação. Os textos vêm de
`RoteadorDeIntencao` e `OrquestradorConversaServico`. Com `chatbot.llm=gemini`, o `Redator` pode reescrever as
palavras, mas nunca os fatos (protocolo, status, datas, título do chamado). Com `fake`, valem os textos abaixo.
O caminho geral e as intenções estão em [`chatbot-lily.md`](chatbot-lily.md).

## 1. Consulta por protocolo

A intenção é `CONSULTAR_DEMANDA`. O protocolo é achado na mensagem por regex, nunca pela LLM.

**Com protocolo na mensagem**

> Cliente: Qual o status da WXN-2026-0001?
> Lily: Demanda WXN-2026-0001 - ... Status: em andamento ...

**Sem protocolo**

> Cliente: Quero saber como está minha demanda
> Lily: Claro! Qual é o protocolo da demanda? Ele tem o formato WXN-2026-0001.
> Cliente: WXN-2026-0001
> Lily: Demanda WXN-2026-0001 - ... Status: em andamento ...

Se o cliente responde que não tem o protocolo, a Lily encaminha para um atendente (fluxo 3).
Se o formato do protocolo for inválido, ela pede o protocolo de novo.

**Protocolo não encontrado**

> Lily: Não encontrei a demanda WXN-2026-9999. Confira o número ou me diga se prefere falar com um atendente.

Os protocolos de exemplo só aparecem para clientes da empresa de teste do perfil `dev`.

## 2. Abertura de chamado

A intenção é `ABRIR_CHAMADO`. O chamado só é aberto depois de um "sim" claro, interpretado por regras
(`InterpretadorDeConfirmacao`), nunca pela LLM.

**Mensagem vaga: a Lily pergunta primeiro** (menos de 3 palavras com conteúdo ou o Gemini achar pouco)

> Cliente: Preciso de ajuda com aquele problema do relatório
> Lily: Posso abrir um chamado para você. Me conta o que está acontecendo, com o máximo de detalhes que puder.

Depois de perguntar uma vez, ela aceita o que o cliente mandar.

**Confirmação**

> Cliente: O relatório de vendas não carrega desde ontem e aparece erro 500.
> Lily: (mostra o título e a descrição e pergunta se pode confirmar)
> Cliente: sim
> Lily: Pronto! Abri o chamado <protocolo>. Guarde esse protocolo para acompanhar o andamento comigo.

| Resposta do cliente | O que a Lily faz |
|---|---|
| "sim", "pode abrir", "isso mesmo" (só concordância) | Abre o chamado |
| "não", "cancela", "deixa" | "Tudo bem, não abri o chamado. Posso ajudar com mais alguma coisa?" |
| Outra coisa (ex.: "Ok, mas qual o prazo?") | "Responda sim para eu abrir o chamado ou não para cancelar." |
| Pede uma pessoa | Cancela o pedido e encaminha (fluxo 3) |
| "sim" depois de 10 minutos | "Não encontrei um pedido de chamado esperando confirmação. Se quiser, me conte de novo o que está acontecendo." |

## 3. Encaminhamento para um atendente (handoff)

A conversa passa a `AGUARDANDO_ATENDENTE`, o `TicketServico` abre o ticket (com urgência e resumo) e ela entra na fila
do dashboard. A partir daí a Lily não responde mais (`"resposta": null`) e a mensagem do cliente só é gravada.

| Quando acontece | Mensagem |
|---|---|
| Cliente pede um atendente (`FALAR_COM_HUMANO`) | "Claro! Vou chamar um atendente para te ajudar. Você já está na fila e logo alguém assume a conversa." |
| Intenção incerta (`INCERTO`, confiança abaixo de 0,7) | "Não tenho certeza de que consigo ajudar com isso, então vou chamar um atendente para você." |
| Cliente não tem o protocolo | "Sem o protocolo eu não consigo localizar a demanda com segurança. Vou chamar um atendente para buscar pelo seu cadastro." |

## 4. Fora do escopo

A intenção é `FORA_DE_ESCOPO`. A Lily recusa com gentileza e oferece um atendente, sem encaminhar sozinha.

> Cliente: Qual a previsão do tempo para amanhã?
> Lily: Eu só consigo ajudar com o status de demandas, a abertura de chamados e dúvidas sobre o atendimento da WXN. Se preferir, chamo um atendente.

## 5. Falhas e plano B

A conversa não para por falha de uma parte.

| Falha | O que acontece |
|---|---|
| Consulta de demanda fora do ar | "Não consegui consultar a demanda agora. Tente de novo em instantes ou peça para falar com um atendente." |
| Abertura de chamado fora do ar | "Não consegui abrir o chamado agora. Responda sim para eu tentar de novo em instantes ou peça para falar com um atendente." O pedido continua pendente (só é descartado depois que a API abre o chamado) e a chave de idempotência evita chamado duplicado na nova tentativa |
| LLM fora do ar ou demorando (`LlmIndisponivelException`) | O orquestrador responde "Desculpe, não consegui processar sua mensagem agora. Vou chamar um atendente para continuar com você." e a conversa vai para a fila com um ticket aberto |
| `FiltroDeSaida` barra a resposta da LLM (repete o prompt ou promete uma ação que a Lily não faz) | Encaminha com "Não tenho certeza de que consigo ajudar com isso, então vou chamar um atendente para você." |
| Classificador Gemini falha ou devolve algo inválido | Valem as regras (`ClassificadorPorRegras`) |
| Redator Gemini perde um fato, passa de 800 caracteres ou falha | Vale o rascunho com o texto pronto |

> Cliente: Qual o status da minha demanda?
> (LLM fora do ar)
> Lily: Desculpe, não consegui processar sua mensagem agora. Vou chamar um atendente para continuar com você.
