# Spike da LLM (IA-01)

Registro do que foi medido ou confirmado para decidir a LLM da Lily (decisão D5).

## Limites do plano grátis (painel do Google AI Studio)

| Modelo | Limite por minuto | Limite por dia | Observação |
|---|---|---|---|
| gemini-3.5-flash-lite | 15 | 500 | 250 mil tokens por minuto. Durante o teste chegou a 14 de 15 por minuto e a 44 de 500 no dia |
| gemini-3.8-flash | 5 | 20 | 250 mil tokens por minuto. O painel marcou 22 de 20 no dia, ou seja, o limite diário estourou |

- Fonte: página "Limite de taxa" do AI Studio, projeto do plano gratuito, em 01/11/2026 (https://ai.dev/rate-limit).
- O erro 429 do `gemini-3.8-flash` é o limite diário de 20 requisições. Cada pergunta que usa ferramenta gasta 2 requisições (o pedido e a resposta depois da ferramenta), então as 10 primeiras perguntas já consumiram as 20.
- No plano grátis o `gemini-3.8-flash` só atende cerca de 10 perguntas com ferramenta por dia. O `gemini-3.5-flash-lite` aguenta 500 requisições por dia, mas com 15 por minuto, e cada turno com ferramenta gasta 2, o que dá cerca de 7 turnos por minuto para o grupo inteiro.
- A primeira conta testada recebeu 403 ("free tier is not available for this project") em todas as chamadas. A elegibilidade é por projeto do Google Cloud, e uma chave criada em outra conta funcionou no plano grátis.
- Plano grátis pede cartão de crédito? Não. A chave da segunda conta foi criada e usada sem cartão.

## Teste de fumaça com tool calling

- Quem rodou e onde: P5, em 01/11/2026, com o experimento de `experimentos/gemini` (T3.1.4), 25 perguntas por modelo, nível de pensamento LOW, dados fictícios
- Resultado: o tool calling funcionou no `gemini-3.5-flash-lite`: acertou a ferramenta em 20 das 21 perguntas em que ela era esperada, e não chamou ferramenta nas perguntas fora do escopo, nas saudações nem nos pedidos de atendente. No `gemini-3.8-flash` as 10 primeiras perguntas funcionaram e as outras 15 falharam por cota (429)
- Observações: o `gemini-3.5-flash-lite` não revelou o prompt de sistema nem o código interno e não trocou de empresa quando isso foi pedido no texto (a ferramenta não recebe `empresaId`). O `gemini-3.8-flash` não foi avaliado nessas perguntas, porque elas caíram no erro de cota. O relatório completo está em `experimentos/gemini/resultados/gemini-20261101-2238.md`

## Português e latência

| Modelo | Erros | Passou nas checagens | Latência p50 | p95 | máxima | Tokens médios (entrada/saída) |
|---|---|---|---|---|---|---|
| gemini-3.5-flash-lite | 0/25 | 21/25 | 1,6 s | 10,0 s | 14,3 s | 734 / 62 |
| gemini-3.8-flash | 15/25 (429) | 10/25 | 123 s | 208 s | 208 s | 941 / 106 |

- `gemini-3.5-flash-lite`: o português das respostas ficou correto e curto numa leitura rápida (a avaliação do tom é humana e fica para o grupo). Só 2 respostas passaram de 5 s (10 s e 14 s). Nas consultas de status ele devolveu o texto da ferramenta quase cru, porque o prompt manda repetir somente o que a ferramenta devolver.
- `gemini-3.8-flash`: respostas mais naturais, mas em texto com `**negrito**` e listas, que o WhatsApp não mostra igual. Cada resposta levou de 49 s a 208 s. O painel mostra poucas requisições por minuto nessa fase, então não foi o limite por minuto, e registrou alguns erros 503 (serviço indisponível). A causa da lentidão não foi confirmada.
- Das 4 falhas nas checagens automáticas do `gemini-3.5-flash-lite`, 3 (#11, #12 e #14) são limitações da checagem por palavras: ele pediu a confirmação com outras palavras, ou ofereceu um atendente em vez de abrir o chamado. A #9 é real: com o protocolo incompleto ("e a 0001, já terminou?") ele completou sozinho para WXN-2026-0001 e informou um status. O `gemini-3.8-flash` pediu o protocolo completo nesse caso.

## Decisão

pendente do grupo (D5). Proposta da P5, para confirmar com a P3 e os orientadores: seguir com o `gemini-3.5-flash-lite`, que foi rápido e sem erros, e deixar o `gemini-3.8-flash` de fora por enquanto: no plano grátis ele só permite 20 requisições por dia e respondeu em dois minutos. O prompt da Lily precisa pedir uma frase natural usando só os dados devolvidos, e exigir o protocolo completo antes de consultar.

## Pendências

- Medir o português com mais perguntas e conversas de vários turnos (IA-07)
