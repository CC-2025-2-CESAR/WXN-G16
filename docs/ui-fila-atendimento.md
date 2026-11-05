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

- Lista a fila na ordem de chegada (`GET /api/fila`), ao abrir, no botão de
  atualizar e a cada 15 segundos.
- Abre uma conversa e mostra as mensagens trocadas com a Lily
  (`GET /api/fila/{conversaId}`).
- O atendente escolhe quem está atendendo (lista de `GET /api/atendentes`, a
  escolha fica no `localStorage` do navegador) e assume a conversa
  (`POST /api/fila/{conversaId}/assumir`).
- Encerra uma conversa assumida (`POST /api/conversas/{conversaId}/encerrar`).
- Quando o servidor recusa algo (conversa já assumida, atendente indisponível,
  conversa que sumiu), mostra o `detail` da resposta e recarrega a fila.

As regras vêm do backend: a fila segue a ordem de chegada, só atendente
disponível assume e, se dois assumem a mesma conversa ao mesmo tempo, um
consegue e o outro recebe `409`.

## Limitações conhecidas

- O backend não tem listagem de conversas assumidas. A lista "em atendimento"
  da tela só existe na memória da página: ao recarregar, as conversas saem da
  tela, mas continuam com o atendente no banco.
- Histórico, transferência para outro atendente e envio de mensagem pelo
  atendente não existem; ficam para a próxima história.
- Não há autenticação: o atendente é só uma escolha na tela, e a API aceita
  chamadas de qualquer cliente que a alcance.
- A atualização é por consulta periódica (15 s), sem tempo real.
