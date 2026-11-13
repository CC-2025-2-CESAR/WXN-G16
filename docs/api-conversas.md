# Atualização da API - Módulo de Conversas (MEM-06)

Este documento descreve os novos endpoints de leitura (GET) adicionados à API de Conversas para alimentar o Dashboard com as informações de listagem e detalhamento do histórico.

---

## 1. Listar Conversas Resumidas

Retorna uma lista contendo o resumo de todas as conversas registradas no banco de dados. O payload é otimizado para a exibição de listas no Dashboard.

- **URL:** `/api/conversas`
- **Método HTTP:** `GET`
- **Parâmetros:** Nenhum.

### Comportamento
O endpoint varre todas as conversas ativas/encerradas. Se o cliente vinculado à conversa ainda não estiver identificado no sistema, o campo `clienteNome` retornará o valor padrão `"Anônimo"`.

### Resposta de Sucesso (200 OK)

```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "clienteNome": "Maria Souza",
    "status": "COM_ATENDENTE"
  },
  {
    "id": "987fcdeb-51a2-43d7-9012-345678901234",
    "clienteNome": "Anônimo",
    "status": "COM_CHATBOT"
  }
]
```

---

## 2. Detalhar Conversa e Histórico

Retorna os detalhes completos de uma conversa específica baseada no seu identificador único, incluindo o histórico sequencial de mensagens (turnos).

- **URL:** `/api/conversas/{id}`
- **Método HTTP:** `GET`
- **Parâmetros de Rota:**
  - `id` (UUID, obrigatório): O identificador único da conversa.

### Comportamento
Busca a conversa pelo ID e mapeia suas mensagens internas. O histórico mantém a ordem de envio para que o Dashboard possa renderizar o balão de chat corretamente.

### Resposta de Sucesso (200 OK)

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "clienteNome": "Maria Souza",
  "status": "COM_CHATBOT",
  "mensagens": [
    {
      "tipoRemetente": "CHATBOT",
      "texto": "Oi, sou a Lily!"
    },
    {
      "tipoRemetente": "CLIENTE",
      "texto": "Preciso de ajuda com a nota"
    }
  ]
}
```

### Respostas de Erro

**404 Not Found**
Retornado caso o ID fornecido não corresponda a nenhuma conversa existente.
```json
{
  "status": 404,
  "detail": "Conversa com identificador 123e4567-e89b-12d3-a456-426614174000 não foi encontrada."
}
```

**400 Bad Request**
Retornado automaticamente pelo Spring caso o `id` passado na URL não seja um UUID válido (ex: `/api/conversas/123`).