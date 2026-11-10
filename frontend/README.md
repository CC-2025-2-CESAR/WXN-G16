# WXN Chatbot — frontend

React + TypeScript + Vite. Instruções completas de instalação e execução
estão no [README da raiz do repositório](../README.md).

Resumo:

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # build de produção em dist/
npm run lint     # oxlint
```

Variáveis de ambiente: veja [`.env.example`](.env.example).

### Prévia local com conversas fictícias

Com o Vite em execução, abra `http://localhost:5173/?demo=1` para testar a
fila, o histórico e as ações de assumir e encerrar sem iniciar o backend. Esse
modo funciona apenas no servidor de desenvolvimento e mostra um aviso de
demonstração na tela. As alterações feitas nos exemplos ficam só na memória da
página e são reiniciadas ao recarregar. Sem `?demo=1`, a tela continua usando a
API real configurada em `VITE_API_URL`.

## Tela de atendimento

A tela mostra a fila de conversas que aguardam um atendente humano. Todos os
dados vêm da API do backend (`VITE_API_URL`, padrão `http://localhost:8080`),
que precisa estar no ar e com o CORS liberado para a origem do Vite.

| O que a tela faz | Endpoint |
| --- | --- |
| Carregar a fila: ao abrir, no botão "Atualizar fila" e a cada 15 s (só com a aba visível) | `GET /api/fila` |
| Abrir uma conversa da fila | `GET /api/fila/{conversaId}` |
| Assumir uma conversa | `POST /api/fila/{conversaId}/assumir` com `{ "atendenteId": "..." }` |
| Encerrar uma conversa assumida | `POST /api/conversas/{conversaId}/encerrar` |
| Listar os atendentes | `GET /api/atendentes` |
| Mostrar o status do backend na lateral | `GET /api/health` |

Quando o servidor recusa uma operação, a tela mostra o campo `detail` da
resposta de erro (RFC 9457) e recarrega a fila. Se a conversa aberta já saiu da
fila (404 ou 409), o painel avisa e a fila é recarregada.

- **Atendendo como**: o atendente é escolhido em Configurações (ou pelo perfil
  na lateral) e fica salvo no `localStorage` do navegador. Sem essa escolha não
  dá para assumir conversas. Atendentes indisponíveis aparecem marcados e o
  servidor recusa a atribuição.
- **Em atendimento (nesta sessão)**: as conversas assumidas ficam nessa lista
  só na memória da página. Ao recarregar, elas saem da tela, mas continuam em
  atendimento no servidor.
- **Ainda não existe**: histórico (item "Em breve" no menu), envio de mensagens
  e transferência, porque o backend ainda não tem esses recursos.

## Estrutura de `src`

| Pasta | Conteúdo |
| --- | --- |
| `services/api.ts` | chamadas HTTP e tratamento de erro |
| `types/` | tipos fiéis ao contrato da API |
| `hooks/` | fila, detalhe da conversa, atendentes, status do backend e atualização automática |
| `dashboard/` | componentes e estilos da tela |
| `components/` | `StatusBadge` |
