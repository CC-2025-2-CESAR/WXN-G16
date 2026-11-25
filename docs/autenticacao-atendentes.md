# Acesso dos atendentes ao painel

O painel usa contas de atendentes com e-mail e senha. A senha nunca é gravada em texto puro: o backend guarda apenas um hash BCrypt na coluna `atendente.senha_hash`. As três pessoas fictícias criadas pelo perfil `demo` continuam sem senha e não conseguem entrar até uma conta ser provisionada.

## Criar contas de funcionários

Configure as variáveis abaixo no ambiente do backend (ou como segredos da plataforma de hospedagem) para criar uma conta:

| Variável | Conteúdo |
| --- | --- |
| `ATENDENTE_PROVISIONAR_NOME` | Nome que aparecerá no painel |
| `ATENDENTE_PROVISIONAR_EMAIL` | E-mail do funcionário |
| `ATENDENTE_PROVISIONAR_SENHA` | Senha inicial com pelo menos 12 caracteres |

As três devem ser definidas juntas. Inicie o backend para criar a conta e depois remova a senha da configuração. Para cadastrar outro funcionário, repita com outro e-mail; contas já provisionadas não têm a senha alterada ao reiniciar. Não coloque valores reais em `.env.example` nem no Git. Redefinição de senha ainda requer um fluxo administrativo próprio. Os nomes antigos `ATENDENTE_INICIAL_*` continuam aceitos por compatibilidade.

## Contrato HTTP da SPA

| Ação | Requisição | Resultado |
| --- | --- | --- |
| Obter proteção CSRF | `GET /api/auth/csrf` | `{ "token": "..." }` |
| Entrar | `POST /api/auth/login` com `{ "email": "...", "senha": "..." }` | Dados públicos do atendente e sessão |
| Ver sessão | `GET /api/auth/me` | Dados públicos do atendente ou `401` |
| Sair | `POST /api/auth/logout` | `204`, sessão invalidada |

O frontend deve usar `credentials: 'include'` em todas as chamadas da API e enviar o token CSRF no cabeçalho `X-XSRF-TOKEN` em todo `POST`, `PUT`, `PATCH` ou `DELETE`. Obtenha um token antes de entrar e atualize-o após entrar ou sair. O cookie `JSESSIONID` é `HttpOnly`; o JavaScript não lê a sessão. O servidor troca o ID da sessão no login para evitar fixation.

As rotas `/api/fila`, `/api/atendentes`, `/api/conversas`, `/api/atendimentos` e a API do simulador exigem login. Ao assumir, responder ou encerrar, o backend usa o ID da sessão e confere se a conversa pertence ao atendente; o navegador não escolhe outro ID. `/api/health`, login, CSRF, Actuator health e webhooks externos continuam acessíveis sem sessão. O modo local `?demo=1` do frontend usa dados fictícios no navegador e não é uma autenticação real.

## Origem do frontend e cookies

`CORS_ALLOWED_ORIGINS` deve conter apenas as origens exatas do frontend, separadas por vírgula. No desenvolvimento, o padrão é `http://localhost:5173`. Quando frontend e backend estiverem em sites diferentes, use HTTPS e configure `SESSION_COOKIE_SAME_SITE=none` e `SESSION_COOKIE_SECURE=true`. No perfil `prod`, o cookie seguro já vem ativado por padrão.

O envio da resposta ao cliente depende do canal. Atualmente o Telegram pode receber a mensagem; WhatsApp e simulador ainda podem apenas registrá-la no histórico, com `entregue: false` e um aviso retornado pela API. Veja `docs/ui-fila-atendimento.md`.
