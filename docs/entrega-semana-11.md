# Entrega — Semana 11 (CC): modelo de classes de entidade

**Atividade:** elaborar o modelo de classes de entidade, modelando em POO as
entidades centrais (Cliente, Atendente, Conversa, Mensagem, Chatbot e
Ticket/Atendimento) com atributos, encapsulamento e relacionamentos.

**Repositório:** <https://github.com/CC-2025-2-CESAR/WXN-G16>
(pacote `br.com.wxnchatbot.backend.model`)

## Texto da entrega

Modelamos as seis entidades centrais da Lily, o chatbot de atendimento da
WXN, como classes de domínio em Java, sem dependência de banco de dados ou
framework. O diagrama de classes está em
[`docs/modelo-de-classes.md`](modelo-de-classes.md).

**Atributos e encapsulamento.** Todos os atributos são privados. O que não
muda depois da criação é `final` (identificador, cliente e chatbot de uma
conversa, remetente e conteúdo de uma mensagem). Os construtores e os
*setters* validam os dados (nome obrigatório, telefone com 10 a 15 dígitos,
e-mail válido, mensagem não vazia). O histórico de mensagens é exposto como
lista somente leitura, e só a `Conversa` consegue criar mensagens e tickets
e mudar o estado deles.

**Herança e polimorfismo.** `Cliente` e `Atendente` herdam da classe
abstrata `Pessoa`. A interface `Remetente` é implementada por `Pessoa` e por
`Chatbot`, então uma `Mensagem` pode ter qualquer um dos três como autor, e a
`Conversa` decide quem pode falar a partir do tipo do remetente.

**Relacionamentos.** Uma `Conversa` se associa a um `Cliente`, a um `Chatbot`
e, depois do handoff, a um `Atendente`. A `Conversa` **compõe** suas
`Mensagem` (a mensagem não existe fora dela) e pode gerar um `Ticket`, que é
o pedido de atendimento humano.

**Regras de negócio no próprio modelo.** A conversa passa por quatro estados
(com o chatbot, aguardando atendente, com atendente, encerrada) e rejeita
transições inválidas: o chatbot não responde depois que o atendente assume,
uma conversa encerrada não recebe mensagens, e um atendente indisponível não
pode assumir um atendimento.

**Verificação.** 22 testes unitários (JUnit) cobrem a validação dos dados, o
fluxo completo chatbot → atendente → encerramento e as regras acima.

**Fora do escopo desta entrega:** persistência (JPA/banco), a entidade
`Company` (multi-tenant) e qualquer funcionalidade de IA.
