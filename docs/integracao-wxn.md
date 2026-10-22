# Integração com a API da WXN (mock, ferramentas do chatbot e e-mail)

Cobre as tarefas T3.1.1, T3.1.2, T3.1.3 e T2.3.1. O contrato está em
[`api/wxn-demandas.openapi.yaml`](api/wxn-demandas.openapi.yaml).

A WXN ainda não entregou a API real de demandas. Até lá, o backend traz uma
**implementação simulada com dados fictícios**, que só existe com o perfil
`mock`. Quando a API real chegar, troca-se a base URL e a autenticação; o
restante do código não muda.

## Rodando com o mock

```bash
SPRING_PROFILES_ACTIVE=mock ./mvnw spring-boot:run
```

No PowerShell: `$env:SPRING_PROFILES_ACTIVE="mock"; ./mvnw spring-boot:run`.
Sem o perfil, o `DemandaGateway` aponta para um endereço que não existe e todas
as consultas viram "serviço indisponível" (de propósito: o bot nunca diz que um
protocolo não existe quando, na verdade, a API é que não está lá).

O perfil `mock` expõe endpoints **sem autenticação** com dados fictícios: use só
em desenvolvimento e na demonstração da PoC, nunca com dados reais de clientes.

```bash
curl -H "X-Empresa-Id: 00000000-0000-0000-0000-000000000001" \
  http://localhost:8080/mock/wxn/demandas/WXN-2026-0001
```

| Propriedade | Padrão | Para que serve |
|---|---|---|
| `WXN_API_BASE_URL` (`wxn.api.base-url`) | `http://localhost:${server.port}/mock/wxn` | Onde fica a API da WXN |
| `wxn.api.timeout-conexao` | `2s` | Tempo máximo para conectar |
| `wxn.api.timeout-leitura` | `3s` | Tempo máximo esperando a resposta |
| `wxn.mock.atraso-lento-ms` | `6000` | Atraso do protocolo `WXN-LENTO` (só no mock) |

### Dados do mock (todos fictícios)

| Empresa | Protocolo | Status |
|---|---|---|
| Demo (`...0001`) | `WXN-2026-0001` | em andamento |
| Demo | `WXN-2026-0002` | concluída |
| Demo | `WXN-2026-0003` | aguardando retorno do cliente |
| Outra (`...0002`) | `WXN-2026-9001` | aberta (a empresa Demo **não** a enxerga) |

Protocolos que simulam falhas: `WXN-ERRO-500` responde 500, e `WXN-LENTO` só
responde depois do atraso configurado (para testar *timeout*). Chamados abertos
pelo `POST /chamados` passam a existir para consulta até reiniciar o backend.

> O identificador da empresa de demonstração do mock deve ser o mesmo da
> empresa de seed do banco quando a `Empresa` existir (decisão DEC-01 do board
> do projeto).

## Como o orquestrador deve usar as ferramentas

O pacote `ferramenta/` tem três peças. A regra que as une: **a LLM nunca
decide a empresa nem grava nada sozinha.**

| Classe | Quem pode chamar | O que faz |
|---|---|---|
| `ConsultaDemandaFerramenta` | orquestrador **ou LLM** | Consulta o status pelo protocolo. Só aceita protocolo bem formado e sempre usa o `empresaId` do contexto do servidor |
| `SolicitacaoChamadoFerramenta` | orquestrador **ou LLM** | Registra um **pedido pendente** de abertura de chamado. Não tem acesso ao `DemandaGateway`, então não consegue gravar |
| `ConfirmacaoChamadoServico` | **só o orquestrador** | Único ponto que grava na API. Chamar só depois de reconhecer, por regra, um "sim" explícito do cliente |

Fluxo de uma consulta:

```java
switch (consultaDemanda.consultarStatus(empresaId, protocoloDigitado)) {
    case Encontrada e -> responder(e.resumoParaCliente());   // texto montado só com o dado da API
    case NaoEncontrada n -> responderNaoEncontrado(n.protocolo()); // e oferecer atendimento humano
    case ProtocoloInvalido p -> pedirProtocoloNovamente();
    case Indisponivel i -> responderInstabilidade();          // e oferecer atendimento humano
}
```

Fluxo de abertura de chamado:

1. A LLM (ou uma regra) chama `solicitarAbertura(empresaId, conversaId, titulo, descricao)`
   e recebe um `PedidoDeConfirmacao`; o bot mostra `resumoParaCliente()` e pergunta
   "Posso confirmar?".
2. Se o **cliente** responder com uma confirmação explícita, o orquestrador chama
   `confirmar(empresaId, conversaId)`. Se recusar, chama `cancelar(conversaId)`.
3. Sem confirmação, o pedido expira em 10 minutos e nada é gravado.

Detalhes que importam:

- **`empresaId` e `conversaId` vêm sempre do servidor.** Um pedido só pode ser
  confirmado na mesma conversa e na mesma empresa em que foi feito.
- **Idempotência:** cada pedido tem uma chave própria, enviada à API no
  cabeçalho `Idempotency-Key`. Se a API cair no meio da confirmação,
  `confirmar` devolve `Indisponivel` e **mantém o pedido**; tentar de novo
  reusa a mesma chave e não abre o chamado duas vezes.
- Os pedidos pendentes ficam **em memória**: reiniciar o backend descarta os
  pedidos ainda não confirmados (o cliente só precisa pedir de novo).
- Ao registrar a LLM (Spring AI, `@Tool`), exponha apenas
  `ConsultaDemandaFerramenta` e `SolicitacaoChamadoFerramenta`.

## E-mail de confirmação (T2.3.1)

`NotificationGateway` é a interface para enviar a confirmação de solicitação
registrada. A `NotificationGatewayFake` só guarda as mensagens em memória e
**avisa em log (WARN) que nada foi enviado de verdade**. Quando o provedor real
(SMTP) for configurado (T2.3.2), criar a implementação real e marcar a fake com
`@Profile("!prod")`.

## Suposições que a WXN precisa confirmar

- Formato do protocolo (hoje: letras maiúsculas, dígitos, `-` e `_`, de 3 a 40
  caracteres) e a lista de status possíveis.
- Como a API real autentica cada cliente e como identifica a empresa (hoje: o
  cabeçalho `X-Empresa-Id`).
- Se a API real aceita `Idempotency-Key` na abertura de chamado. Se não aceitar,
  é preciso outro jeito de evitar chamado duplicado depois de um *timeout*.

## Testes

Rodam sem banco de dados:

```bash
./mvnw -Dtest='!BackendApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false test
```

O mock é testado com HTTP de verdade (servidor embutido do JDK), cobrindo
protocolo existente, inexistente, de outra empresa, erro 500, *timeout* e
servidor fora do ar. As ferramentas são testadas com um gateway falso que
registra cada consulta e cada escrita, incluindo as tentativas de injetar outra
empresa pelo texto.
