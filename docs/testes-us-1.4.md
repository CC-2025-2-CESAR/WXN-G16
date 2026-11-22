# Testes da US 1.4: urgência e encaminhamento para atendente humano

Onde cada critério de aceite da US 1.4 ([#7](https://github.com/CC-2025-2-CESAR/WXN-G16/issues/7)) é verificado
por testes automáticos. Os nomes são classes e métodos em `backend/src/test/java/br/com/wxnchatbot/backend/`.

Situação em 22/11/2026: 5 critérios cobertos e 1 parcial.

## 1. Quando o bot não resolve, é criado um ticket com motivo, urgência e resumo

**Parcial.**

| Situação | Abre ticket? | Teste |
|---|---|---|
| Cliente pede um atendente | Sim | `chatbot/RoteadorDeIntencaoTest.pedidoDeHumanoEncaminhaComOMotivo`, `service/OrquestradorConversaServicoTest.pedidoDeAtendenteResponderEColocaAConversaNaFilaComTicket` |
| Intenção não reconhecida (confiança abaixo do limiar) | Sim | `chatbot/RoteadorDeIntencaoTest.intencaoIncertaEncaminha`, `chatbot/RoteadorDeIntencaoTest.confiancaAbaixoDoLimiarViraIncertaMesmoComIntencaoDefinida` |
| Falha de integração: LLM fora do ar | Sim | `service/OrquestradorConversaServicoTest.llmForaDoArRespondeMensagemPadraoEAbreTicketParaUmHumano` |
| Falha de integração: API da WXN fora do ar | Não, por decisão do time: a Lily pede para tentar de novo ou chamar um atendente (`docs/fluxos-de-conversa.md`, seção 5) | `service/OrquestradorConversaServicoTest.consultaComFerramentaForaDoArRespondeMensagemPadraoEGuardaNaConversa` |
| Palavra-chave de problema ("o sistema caiu"), sem pedir atendente | **Não implementado** | — |

O conteúdo do ticket (motivo, urgência calculada pelas mensagens do cliente e resumo com as 3 últimas) é verificado em:

- `service/TicketServicoTest`: `mensagemAnteriorUrgenteDeixaOTicketComUrgenciaAlta`, `conversaSemNadaUrgenteGeraTicketBaixa`, `prevaleceONivelMaisAltoEntreAsMensagens`, `resumoTrazSoAsTresUltimasMensagensDoCliente`, `conversaSemMensagensGeraResumoNulo`.
- De ponta a ponta, pela API e conferindo o banco: `controller/ChatFluxoIntegracaoTest.pedidoDeAtendenteLevaAFilaOAtendenteAssumeEAsMensagensSeguintesFicamComEle` (motivo, urgência ALTA e resumo).

**Lacuna:** uma mensagem com palavra-chave de problema que não pede atendente não é encaminhada; o
`ClassificadorUrgencia` só roda quando um ticket já está sendo aberto. Decidir se (e a partir de qual urgência) a Lily
deve encaminhar sozinha fica com o grupo e a P3.

## 2. Urgência por regra, com pelo menos 5 casos de teste, incluindo falsos positivos

**Coberto.** `service/ClassificadorUrgenciaTest`: 17 testes. Falsos positivos: `naoEUrgenteNaoDeveSerAlta`,
`semUrgenciaNaoDeveSerAlta`, `problemaQueJaPassouNaoDeveSerAlta`, `sistemaQueJaNormalizouNaoDeveSerAlta`,
`semProblemaNaoDeveSerMedia`.

## 3. Fila ordenada por urgência e, depois, por data de criação

**Coberto.**

- `service/FilaConversasServicoIntegracaoTest.filaColocaOsMaisUrgentesNaFrenteMesmoQueTenhamChegadoDepois`: tickets chegando na ordem inversa da urgência saem ALTA, MEDIA e BAIXA.
- `service/FilaConversasServicoIntegracaoTest.filaMostraOsPedidosDoMaisAntigoParaOMaisNovo`: dentro da mesma urgência, o mais antigo primeiro.
- `service/FilaConversasServicoIntegracaoTest.cadaItemDaFilaTrazAUrgenciaDoTicket` e `controller/FilaConversasControllerTest.filaDevolve200ComOsItensNaOrdemDoServico`: a urgência aparece em cada item do `GET /api/fila`.

## 4. Sem atendente online, o cliente recebe o horário de atendimento e o pedido é registrado

**Coberto.**

- `service/OrquestradorConversaServicoTest.semAtendenteDisponivelOPedidoFicaNaFilaEOClienteRecebeOHorario`: o ticket é aberto, a conversa fica na fila e a resposta (e a mensagem salva) traz o horário.
- `service/OrquestradorConversaServicoTest.comAtendenteDisponivelARespostaNaoFalaDeHorario`: com atendente, a resposta não muda.

## 5. Assumir muda o status; assumir ou resolver um ticket finalizado retorna 409 e nunca sobrescreve o estado

**Coberto.**

- Assumir muda o status: `model/ConversaTest.fluxoCompletoDoChatbotAoAtendenteHumanoAteEncerrar`, `controller/FilaConversasApiIntegracaoTest.fluxoCompletoDaFilaAoEncerramentoConferindoOBanco`.
- Assumir um ticket já resolvido: `model/ConversaTest.assumirConversaJaEncerradaFalhaENaoMudaNada` e, pela API (409), `controller/FilaConversasApiIntegracaoTest.fluxoCompletoDaFilaAoEncerramentoConferindoOBanco`.
- Resolver um ticket já resolvido: `model/ConversaTest.conversaEncerradaNaoAceitaMensagensNemNovoEncerramento` e, pela API (409), `controller/ConversaControllerTest.encerrarConversaJaEncerradaDevolve409`.
- Concorrência (dois atendentes ao mesmo tempo, ou assumir e encerrar ao mesmo tempo): `service/AtribuicaoConcorrenteIntegracaoTest`, `controller/FilaConversasApiIntegracaoTest.doisAssumirSimultaneosNaMesmaConversaDaUmNoContentEUmConflito`, `service/AssumirEEncerrarConcorrentesIntegracaoTest`.

## 6. A criação do ticket publica um evento de domínio, verificável em teste

**Coberto.** `service/TicketServicoTest`: `abrirPublicaOEventoComOsDadosDoTicket`, `conversaInexistenteNaoPublicaEvento`,
`pedidoRepetidoNaoPublicaUmSegundoEvento`.

## Como rodar

```bash
cd backend
./mvnw verify