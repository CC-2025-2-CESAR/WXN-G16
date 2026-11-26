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

Com o Vite em execução, abra `http://localhost:5173/?demo=1`. A prévia local
aceita estas contas de teste:

| Atendente | E-mail | Senha |
| --- | --- | --- |
| Ana Martins | `ana.martins@demo.wxn.example` | `Demo@2026` |
| Paulo Mendes | `paulo.mendes@demo.wxn.example` | `Paulo@2026` |

Elas permitem testar a fila, assumir conversas e responder pelo dashboard sem
iniciar o backend. São dados fictícios no navegador: nenhuma resposta é
enviada a um cliente real. A sessão e os atendimentos de exemplo ficam no
`sessionStorage` da aba e sobrevivem a uma atualização da página. Sem
`?demo=1`, o funcionário entra com seu próprio e-mail e senha, validados pela
API real em `VITE_API_URL`. A tela não permite escolher a identidade de outro
funcionário.

No modo `?demo=1`, o simulador de WhatsApp responde com mensagens locais e
permite encaminhar uma conversa nova para a fila da prévia. Essas respostas não
usam a Lily real nem chegam ao WhatsApp. Sem `?demo=1`, o simulador conversa
com a API do backend e requer uma sessão real de funcionário.

## Tela de atendimento

A tela mostra a fila de conversas que aguardam um atendente humano. Todos os
dados vêm da API do backend (`VITE_API_URL`, padrão `http://localhost:8080`),
que precisa estar no ar e com o CORS liberado para a origem do Vite.

| O que a tela faz | Endpoint |
| --- | --- |
| Entrar e manter sessão de funcionário | `GET /api/auth/csrf`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` |
| Carregar a fila: ao abrir, no botão "Atualizar fila" e a cada 15 s (só com a aba visível) | `GET /api/fila` |
| Abrir uma conversa da fila | `GET /api/fila/{conversaId}` |
| Assumir uma conversa como funcionário autenticado | `POST /api/fila/{conversaId}/assumir` |
| Retomar as conversas do funcionário após recarregar | `GET /api/atendimentos/ativos` |
| Ver histórico e responder ao cliente | `GET/POST /api/conversas/{conversaId}/atendimento/mensagens` |
| Encerrar uma conversa assumida | `POST /api/conversas/{conversaId}/encerrar` |
| Mostrar o status do backend na lateral | `GET /api/health` |

Quando o servidor recusa uma operação, a tela mostra o campo `detail` da
resposta de erro (RFC 9457) e recarrega a fila. Se a conversa aberta já saiu da
fila (404 ou 409), o painel avisa e a fila é recarregada.

- **Acesso da equipe**: o backend cria uma sessão protegida por cookie `HttpOnly`
  e CSRF. Veja [como provisionar contas](../docs/autenticacao-atendentes.md).
- **Em atendimento**: o menu lateral abre as conversas atribuídas ao funcionário
  autenticado. A lista é recuperada do servidor após recarregar. Ao selecionar
  um cliente, abre-se uma área de conversa no centro da tela, com histórico,
  respostas e acesso às outras conversas do atendente.
- **Entrega da resposta**: a API informa se a mensagem chegou ao canal do
  cliente. Se ela foi apenas salva no histórico, a tela mostra um aviso.
- **Ainda em breve**: histórico geral e transferência de atendimento.

## Estrutura de `src`

| Pasta | Conteúdo |
| --- | --- |
| `services/api.ts` | chamadas HTTP e tratamento de erro |
| `types/` | tipos fiéis ao contrato da API |
| `hooks/` | fila, detalhe da conversa, atendentes, status do backend e atualização automática |
| `dashboard/` | componentes e estilos da tela |
| `components/` | `StatusBadge` |
