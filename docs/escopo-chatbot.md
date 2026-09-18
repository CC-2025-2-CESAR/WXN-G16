# Escopo do chatbot — conversas com professores e WXN

> **Status:** registro de decisões e esclarecimentos de escopo, não
> implementação. Nada aqui existe como código ainda — é a base pra quando
> as próximas semanas começarem a implementar o chatbot de verdade.
> Registrado em **17/09/2026**, a partir de conversa do grupo com os
> professores e com a WXN.

## Canal: WhatsApp, não site

O chatbot não é uma interface web separada — ele roda **via WhatsApp**. O
backend vai expor um *webhook* que a API do WhatsApp chama a cada mensagem
recebida; o backend processa e responde por ali. O dashboard React que já
existe neste repositório é uma coisa diferente: é a ferramenta **interna**
de gestão/atendimento (ver `docs/modelagem-inicial.md`), não a interface
que o usuário final usa pra conversar.

Isso não muda nada do que já foi montado na fundação técnica — só define
que, quando a etapa do chatbot chegar, o backend ganha um endpoint de
webhook novo (`POST /webhook/whatsapp` ou similar), além do que já existe.

## Acesso a dados do cliente da WXN

Pra consultar informações de um cliente específico da WXN (status de uma
demanda, por exemplo), o grupo vai pedir duas coisas a cada cliente:

1. **API REST** com o status das demandas — caminho preferido, permite
   consulta em tempo real. Idealmente documentada em Swagger/OpenAPI (ver
   glossário abaixo).
2. **Manual operacional** — pra quando não existir API, ou como
   complemento. Vira base de conhecimento (RAG) em vez de fonte
   consultável em tempo real.

Os dois casos já estavam previstos no "Levantamento Técnico – Sistema
Conversacional WXN" que o grupo produziu antes desta etapa (RAG pra
conteúdo documental, Tool Calling pra dado vivo via API).

## Glossário — o que os termos técnicos significam aqui

| Termo | Na prática deste projeto |
|---|---|
| **RAG** (Retrieval-Augmented Generation) | Antes de responder, busca o trecho certo num documento/base (ex.: o manual operacional de um cliente) e manda esse trecho pra IA reformular — em vez dela responder de memória. |
| **Tool Calling** | A IA não responde direto: ela pede ao backend pra rodar uma função (ex.: `consultarStatusDemanda(id)`), recebe o dado real vindo da API do cliente, e só então formula a resposta. |
| **MCP** (Model Context Protocol) | Padrão pra catalogar essas funções (Tool Calling) de forma reutilizável — em vez de cada integração de cliente ser feita de um jeito diferente. O "catálogo MCP" é cada API de cliente virando uma ferramenta catalogada nesse padrão. |
| **Swagger / OpenAPI** | O "manual de instruções" de uma API REST: descreve quais endpoints existem e o que cada um espera/devolve. Se o cliente documentar a API dele assim, dá pra saber exatamente o que consultar sem ficar perguntando. |
| **Guardrails** | Regras que travam o que a IA pode fazer ou dizer — nunca misturar dado de um cliente com o de outro, nunca responder fora do escopo da empresa, transferir pra humano quando não tiver certeza. É o que reduz alucinação. |

Nada disso está implementado ainda — é a etapa de IA, já listada em
"Próximos passos" no `README.md` desde a fundação técnica (Semana 05). O
foco do grupo é o chatbot/orquestração, não treinar um modelo do zero.

## Priorização (MoSCoW, feito no Figma)

### Must have
- Histórico de conversa — o chatbot lembra do contexto daquela sessão.
- Transferência pra humano em caso de problema de comunicação.
- Chatbot conectado aos sistemas do cliente (ERP/API — alinhado ao
  posicionamento da própria WXN como integradora de ERP).
- Confirmação por e-mail de que a solicitação do cliente foi registrada.

### Should have
- Classificação de urgência das mensagens no encaminhamento pra humano.
- Verificação de histórico de usuário a partir de um ID exclusivo.
- Salvar histórico de conversas e solicitações passadas de um cliente.
- Abordagens diferentes conforme o que o cliente quer (ex.: contratar
  serviço vs. consultar uma demanda existente).
- Usuário poder anexar print/arquivo na conversa.
- **Painel de gestão** (dashboard interno): número de atendimentos,
  quantos resolvidos pelo bot vs. encaminhados a humano, assuntos mais
  perguntados, tempo médio de atendimento, perguntas que o bot não
  conseguiu responder.

O Must Have e o Should Have batem com o que a fundação técnica já foi
desenhada pra sustentar (fila, histórico, transferência, indicadores) — o
grupo está confirmando o rumo, não mudando de direção.

## Pendente de validar com a WXN

- As 4 perguntas mais frequentes que a WXN recebe dos clientes dela — pra
  treinar o chatbot a responder de forma natural e específica do domínio.
  Adicionar à base de perguntas do grupo (planilha compartilhada da turma).
- Se cada cliente vai oferecer API REST, manual operacional, ou os dois.
