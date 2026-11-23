# Tela da fila de atendimento

A tela (em `frontend/src/dashboard`) mostra as conversas que aguardam um
atendente humano e deixa o atendente abrir, assumir e encerrar cada uma. Ela
consome o `FilaConversasServico` por REST; o contrato está em
[`api/fila-conversas.openapi.yaml`](api/fila-conversas.openapi.yaml).

## Como subir

1. **Banco:** o PostgreSQL local do [README](../README.md) (banco
   `wxn_chatbot`, usuário `wxn_user`). As tabelas são criadas pelo Flyway
   quando o backend sobe.

2. **Backend** com o perfil `demo`, que cria dados fictícios:

   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
   ```

   No PowerShell: `./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"`.

3. **Frontend**, em outro terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Abra `http://localhost:5173`. O frontend procura o backend em
   `http://localhost:8080`; para outro endereço, defina `VITE_API_URL` em
   `frontend/.env`. Se o frontend não rodar na porta 5173, o backend precisa
   liberar a origem dele em `CORS_ALLOWED_ORIGINS`.

### Dados de demonstração

Só o perfil `demo` cria dados (sem ele, nada é inserido). São todos fictícios:
a "Empresa Demonstração", três atendentes (Ana Martins, Carla Nogueira, que
está **indisponível** para mostrar a regra, e Paulo Mendes) e dez conversas,
sendo sete na fila com horários de pedido diferentes, duas ainda com o chatbot
e uma já encerrada.

A criação acontece uma vez: se a "Empresa Demonstração" já existe, o backend
não cria nada de novo. Conversas assumidas ou encerradas não voltam para a
fila, então, para repetir a demonstração do zero, recrie o banco.

## O que a tela faz

- Lista a fila (`GET /api/fila`) da mais urgente para a menos urgente (e por ordem de chegada entre iguais), ao abrir, no botão de
  atualizar e a cada 15 segundos.
- Abre uma conversa e mostra as mensagens trocadas com a Lily
  (`GET /api/fila/{conversaId}`).
- O atendente escolhe quem está atendendo (lista de `GET /api/atendentes`, a
  escolha fica no `localStorage` do navegador) e assume a conversa
  (`POST /api/fila/{conversaId}/assumir`).
- Encerra uma conversa assumida (`POST /api/conversas/{conversaId}/encerrar`).
- No painel de uma conversa **em atendimento** há um campo para responder ao cliente. Enter envia, Shift+Enter quebra a
  linha, o limite é de 4000 caracteres e o campo fica desabilitado enquanto a mensagem é enviada
  (`POST /api/conversas/{conversaId}/atendimento/mensagens`, corpo `{"texto": "..."}`). A mensagem aparece na hora no
  histórico. Se a resposta vier com `entregue = false`, a mensagem ficou salva mas não chegou ao cliente, e o painel mostra
  o `aviso` acima do campo; se o envio falhar, o painel mostra o erro e **mantém** o texto digitado. Numa conversa que
  ainda está na fila o campo não aparece: é preciso assumir antes.
- O histórico de uma conversa em atendimento se atualiza sozinho a cada 3 segundos
  (`GET /api/conversas/{conversaId}/atendimento/mensagens`), com as mensagens novas do cliente e as do atendente. A
  atualização só roda com a aba visível; se o servidor ficar fora do ar, as mensagens já mostradas continuam na tela e
  aparece um aviso discreto até a conexão voltar. Se o servidor responder `404` ou `409` (a conversa saiu do
  atendimento), a atualização para e o campo de resposta é desabilitado. A tela só rola sozinha para as mensagens novas
  se o atendente já estava perto do fim da conversa.
- Quando o servidor recusa algo (conversa já assumida, atendente indisponível,
  conversa que sumiu), mostra o `detail` da resposta e recarrega a fila.
- Mostra um **selo de origem** ao lado do cliente, na lista e no detalhe, para ninguém confundir um teste com um cliente
  de verdade: **Demonstração** (contorno tracejado: dados fictícios do perfil `demo`), **Simulador** (teste escrito por
  alguém do grupo no simulador de WhatsApp ou pela API de chat), **WhatsApp** e **Telegram** (clientes reais, cada um pelo
  seu canal; o cliente do Telegram aparece como "Telegram #id", porque não tem telefone). A origem é gravada quando a
  conversa é aberta (`conversa.origem`) e não muda depois; as conversas que já existiam antes da coluna contam como
  demonstração.

As regras vêm do backend: a fila segue a urgência e, entre iguais, a ordem de chegada, só atendente
disponível assume e, se dois assumem a mesma conversa ao mesmo tempo, um
consegue e o outro recebe `409`.

## Limitações conhecidas

- O backend não tem listagem de conversas assumidas. A lista "em atendimento"
  da tela só existe na memória da página: ao recarregar, as conversas saem da
  tela, mas continuam com o atendente no banco.
- A transferência para outro atendente não existe; fica para a próxima história. Também não há histórico de conversas
  já encerradas na tela.
- Não há autenticação: o atendente é só uma escolha na tela, e a API aceita
  chamadas de qualquer cliente que a alcance.
- A atualização é por consulta periódica (15 s na fila, 3 s no histórico da conversa em atendimento), sem tempo real.
- O texto digitado no campo de resposta só existe na memória da página: trocar de conversa ou recarregar descarta o rascunho.
