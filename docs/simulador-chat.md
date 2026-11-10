# Simulador de WhatsApp

Uma tela que imita o WhatsApp para conversar com a Lily sem depender da Meta (tarefa FE-03 do
[`backlog-implementacao.md`](backlog-implementacao.md)). O cliente "escreve" num celular desenhado na tela e a Lily
responde, passando pelo mesmo caminho que o canal real usará: `POST /api/chat/mensagens` (veja
[`chatbot-lily.md`](chatbot-lily.md)). O código está em `frontend/src/simulador`.

Serve para testar a Lily, ensaiar a demonstração e ver a conversa cair na fila de atendimento. Não serve para testar a
Meta: o webhook, a assinatura e o envio real continuam descritos em [`whatsapp.md`](whatsapp.md), e o número de teste da
Meta não entrega para o Brasil.

## Como subir

1. **Banco e backend**, no perfil `dev` (o padrão), que liga a API de chat e a API simulada da WXN. Veja o
   [README](../README.md) para o banco e `docs/chatbot-lily.md` para `CHATBOT_LLM`:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

   No PowerShell: `.\mvnw.cmd spring-boot:run`.

2. **Frontend**, em outro terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. Abra `http://localhost:5173/#/simulador`. Na fila de atendimento, o botão **Simulador de chat** do menu leva para
   a mesma tela, e **← Fila de atendimento** volta.

A API de chat só existe com `chat.api.habilitada=true` (perfil `dev`, ou `CHAT_API_HABILITADA=true`). Se estiver
desligada, a tela avisa. No Azure ela fica desligada em `prod`; ligue só para uma demonstração.

Com o padrão `CHATBOT_LLM=fake`, a Lily decide por regras (consulta de protocolo, pedido de atendente, chamado) e
responde "[resposta simulada da Lily] Recebi: ..." para o que as regras não reconhecem. Para respostas de verdade, suba o
backend com `CHATBOT_LLM=gemini` e a chave em `GEMINI_API_KEY` (veja `docs/chatbot-lily.md`).

## O que a tela faz

- **Cliente simulado:** o telefone (com DDD) e o nome opcional. O servidor reconhece o cliente pelo telefone: o mesmo
  telefone continua a mesma conversa, e **Cliente novo** gera um telefone aleatório para começar do zero. O nome só vale
  na primeira mensagem de um cliente.
- **Celular:** bolhas do cliente e da Lily com hora, "digitando…" enquanto espera, "lida" quando a Lily responde, e aviso
  de que o servidor está acordando se a resposta demorar mais de 4 s (o servidor gratuito do Azure leva uns 30 s na
  primeira vez). Enter envia, Shift+Enter quebra a linha.
- **Frases prontas:** botões com exemplos (consulta de protocolo, abrir chamado, urgência, falar com atendente).
- **Estado no servidor:** mostra se a conversa está com a Lily, aguardando atendente, com atendente ou encerrada. Quando
  a Lily encaminha, aparece um aviso no celular e um link para a fila de atendimento, onde a conversa já está.
- **Erros:** se o envio falhar, a mensagem fica marcada como "não enviada" com o botão **Tentar de novo**.

## Limitações

- **A tela não busca o histórico no servidor.** As mensagens ficam no `localStorage` do navegador, por telefone, e voltam
  ao recarregar a página. **Limpar a tela** apaga só essa cópia; a conversa no servidor continua, então para recomeçar
  use **Cliente novo**.
- **As respostas do atendente não chegam ao celular.** O backend ainda não envia a resposta do atendente ao canal
  (mesma limitação do WhatsApp real, veja `whatsapp.md`). Depois do encaminhamento, a Lily fica calada e a tela só avisa
  que a mensagem foi registrada.
- Só texto, até 4000 caracteres por mensagem.
- Não há autenticação: qualquer pessoa que alcance a API de chat pode simular um cliente. Por isso ela fica desligada em
  produção.
- Não há testes automatizados do frontend ainda (tarefa FE-09); a tela foi verificada à mão, no navegador, contra um
  backend real.
