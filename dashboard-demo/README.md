# WXN — Dashboard de atendimento

Demonstração em **React + TypeScript + CSS**, baseada no wireframe de atendimento. É independente do backend Java do projeto, com dados fictícios para apresentação.

## Rodar no VS Code

Abra a pasta `dashboard-demo` no VS Code. No terminal integrado:

```powershell
npm install
npm run dev
```

Abra o endereço exibido no terminal, normalmente **http://127.0.0.1:5174/**. O Vite atualiza a página quando você salva um arquivo.

Se `npm` não for reconhecido neste computador, execute:

```powershell
.\iniciar-demo.cmd
```

Esse atalho usa o Node e o pnpm já fornecidos pelo Codex quando não encontra npm. Em outro computador, instale o Node.js 24 antes de usar os comandos npm.

## Onde editar

```text
index.html                         Documento HTML e ponto de entrada
styles.css                         Cores, layout e adaptação para celular
src/main.tsx                       Inicialização do React
src/App.tsx                        Estado da aplicação e ações de atendimento
src/data.ts                        Clientes e conversas fictícias
src/icons.tsx                      Ícones SVG
src/components/Sidebar.tsx         Menu lateral
src/components/Metrics.tsx         Indicadores ilustrativos
src/components/ConversationTable.tsx  Filtros, fila e paginação
src/components/ConversationDetail.tsx Painel de conversa e resposta
src/components/Modal.tsx           Janelas de confirmação e configurações
```

## Roteiro para apresentar

1. Mostre os quatro indicadores e a fila de conversas.
2. Busque `joao`: a pesquisa também funciona sem acentos. Limpe a busca.
3. Filtre por empresa, atendente e status. Em **Mais filtros**, escolha canal ou prioridade.
4. Abra Mariana Costa e mostre o resumo da Lily e o histórico.
5. Clique em **Assumir conversa**, digite uma resposta e envie com Enter.
6. Use **Transferir** para escolher outro atendente, a Lily ou devolver à fila.
7. Assuma novamente e encerre. Abra **Histórico** para encontrar a conversa resolvida.
8. Em **Configurações → Restaurar demonstração**, volte ao estado inicial.

## Dados e comportamento

- Há 12 conversas de hoje e duas antigas para demonstrar o filtro de período.
- Os indicadores são um recorte ilustrativo fixo. O filtro de período altera a lista, não os indicadores.
- Respostas e mudanças de status ficam no `localStorage` deste navegador.
- Nenhuma mensagem real é enviada. Não há conexão com WhatsApp, LLM ou API WXN.
- O resumo da Lily é um texto de exemplo, não uma geração por IA.
- Em telas pequenas, a conversa abre ao selecionar um cliente; o botão fechar retorna à fila.
- As fontes usam Google Fonts quando há internet e fontes locais como alternativa.

## Compilar para apresentação

```powershell
npm run build
npm run preview
```

O build verifica os tipos TypeScript e gera `dist/`. Sirva essa pasta por HTTP; o código-fonte `.tsx` precisa do Vite e não abre diretamente com Live Server.

## Verificação realizada

Build de produção e TypeScript aprovados. Fluxos de busca sem acentos, filtro por empresa, assumir, responder, transferir, encerrar, consultar histórico e restaurar conferidos no navegador.
