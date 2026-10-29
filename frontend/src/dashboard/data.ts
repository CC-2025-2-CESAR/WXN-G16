export type Status = "waiting" | "active" | "ai" | "resolved";
export type Message = {
  id: string;
  text: string;
  kind: "client" | "bot" | "agent" | "system";
  time: string;
};
export type Conversation = {
  id: number;
  name: string;
  initials: string;
  company: string;
  phone: string;
  color: string;
  status: Status;
  channel: "WhatsApp" | "Webchat";
  priority: "Alta" | "Média" | "Baixa";
  subject: string;
  agent: string;
  preview: string;
  ago: string;
  daysAgo: number;
  summary: string;
  messages: Message[];
};
export const statuses: Record<Status, string> = {
  waiting: "Aguardando humano",
  active: "Em atendimento",
  ai: "Com a Lily",
  resolved: "Resolvida",
};
const seeds: Array<
  [
    number,
    string,
    string,
    string,
    Status,
    string,
    string,
    Conversation["priority"],
    Conversation["channel"],
  ]
> = [
  [
    4521,
    "Mariana Costa",
    "Alfa Comércio",
    "peach",
    "waiting",
    "",
    "Quero falar com uma pessoa.",
    "Média",
    "WhatsApp",
  ],
  [
    4520,
    "Carlos Nunes",
    "Grupo Norte",
    "blue",
    "ai",
    "Lily",
    "Qual o prazo do chamado #8537?",
    "Baixa",
    "WhatsApp",
  ],
  [
    4519,
    "Renata Lima",
    "Vértice Ltda.",
    "purple",
    "active",
    "Paulo M.",
    "Enviei o comprovante agora.",
    "Média",
    "WhatsApp",
  ],
  [
    4518,
    "João Santos",
    "Beta Serviços",
    "green",
    "waiting",
    "",
    "Não consigo emitir a nota.",
    "Alta",
    "WhatsApp",
  ],
  [
    4517,
    "Beatriz Melo",
    "Alfa Comércio",
    "pink",
    "resolved",
    "Lily",
    "Obrigada pela ajuda!",
    "Baixa",
    "WhatsApp",
  ],
  [
    4516,
    "Pedro Almeida",
    "Grupo Norte",
    "blue",
    "active",
    "Ana Martins",
    "O sistema apresentou esse erro.",
    "Alta",
    "Webchat",
  ],
  [
    4515,
    "Camila Rocha",
    "Beta Serviços",
    "peach",
    "ai",
    "Lily",
    "Como faço para atualizar meu cadastro?",
    "Baixa",
    "WhatsApp",
  ],
  [
    4514,
    "Lucas Ferreira",
    "Vértice Ltda.",
    "green",
    "waiting",
    "",
    "Preciso de ajuda com o acesso.",
    "Média",
    "Webchat",
  ],
  [
    4513,
    "Juliana Alves",
    "Alfa Comércio",
    "purple",
    "active",
    "Paulo M.",
    "Consegui acessar, pode conferir?",
    "Baixa",
    "WhatsApp",
  ],
  [
    4512,
    "Rafael Souza",
    "Grupo Norte",
    "blue",
    "waiting",
    "",
    "Fui cobrado duas vezes.",
    "Alta",
    "WhatsApp",
  ],
  [
    4511,
    "Fernanda Dias",
    "Beta Serviços",
    "pink",
    "ai",
    "Lily",
    "Onde encontro meus relatórios?",
    "Baixa",
    "Webchat",
  ],
  [
    4510,
    "Diego Ribeiro",
    "Vértice Ltda.",
    "green",
    "resolved",
    "Ana Martins",
    "Tudo certo por aqui. Obrigado!",
    "Baixa",
    "WhatsApp",
  ],
  [
    4509,
    "Amanda Torres",
    "Alfa Comércio",
    "peach",
    "resolved",
    "Paulo M.",
    "Problema resolvido, obrigada.",
    "Baixa",
    "Webchat",
  ],
  [
    4508,
    "Bruno Martins",
    "Grupo Norte",
    "blue",
    "resolved",
    "Ana Martins",
    "A integração voltou a funcionar.",
    "Média",
    "WhatsApp",
  ],
];

export function createInitialData(): Conversation[] {
  return seeds.map(
    (
      [id, name, company, color, status, agent, preview, priority, channel],
      i,
    ) => ({
      id,
      name,
      initials: name
        .split(" ")
        .map((n) => n[0])
        .slice(0, 2)
        .join(""),
      company,
      color,
      status,
      agent,
      preview,
      priority,
      channel,
      phone: `+55 81 9****-${String(2000 + i * 31).slice(-4)}`,
      daysAgo: i < 12 ? 0 : i === 12 ? 2 : 10,
      ago: i === 0 ? "agora" : `há ${i * 2} min`,
      subject:
        i === 0
          ? "Status de pedido"
          : priority === "Alta"
            ? "Suporte técnico"
            : "Dúvida sobre o sistema",
      summary:
        i === 0
          ? "Mariana consultou o pedido #4521. A API retornou “Em separação”, com prazo de entrega até amanhã. A resposta não resolveu sua dúvida e ela solicitou atendimento humano."
          : `${name.split(" ")[0]} entrou em contato sobre ${priority === "Alta" ? "um problema que precisa de atenção" : "uma dúvida no sistema"}. ${status === "resolved" ? "Atendimento finalizado com sucesso." : status === "ai" ? "Lily está conduzindo o atendimento inicial." : "O histórico está disponível para dar continuidade ao atendimento."}`,
      messages:
        i === 0
          ? [
              {
                id: "m1",
                kind: "bot",
                text: "Olá, sou a Lily, assistente virtual da WXN 👋 Como posso ajudar você hoje?",
                time: "09:38",
              },
              {
                id: "m2",
                kind: "client",
                text: "Qual o status do meu pedido #4521?",
                time: "09:39",
              },
              {
                id: "m3",
                kind: "system",
                text: "API consultada com sucesso",
                time: "09:39",
              },
              {
                id: "m4",
                kind: "bot",
                text: "Seu pedido #4521 está em separação. A previsão de entrega é até amanhã, às 18h.",
                time: "09:39",
              },
              {
                id: "m5",
                kind: "client",
                text: "Isso não resolveu. Quero falar com uma pessoa.",
                time: "09:41",
              },
              {
                id: "m6",
                kind: "system",
                text: "Lily solicitou a transferência para um humano",
                time: "09:41",
              },
            ]
          : [
              {
                id: `b${id}`,
                kind: "bot",
                text: `Olá, ${name.split(" ")[0]}! Sou a Lily, assistente virtual da WXN. Como posso ajudar?`,
                time: "09:30",
              },
              { id: `c${id}`, kind: "client", text: preview, time: "09:35" },
              ...(status === "active"
                ? [
                    {
                      id: `s${id}`,
                      kind: "system" as const,
                      text: `${agent} assumiu a conversa`,
                      time: "09:36",
                    },
                  ]
                : []),
              ...(status === "resolved"
                ? [
                    {
                      id: `r${id}`,
                      kind: "system" as const,
                      text: "Atendimento encerrado",
                      time: "09:37",
                    },
                  ]
                : []),
            ],
    }),
  );
}

export const STORAGE_KEY = "wxn-dashboard-react-demo-v1";
export function loadConversations(): Conversation[] {
  try {
    const stored: unknown = JSON.parse(
      localStorage.getItem(STORAGE_KEY) || "null",
    );
    if (
      Array.isArray(stored) &&
      stored.length === 14 &&
      stored.every(
        (c) =>
          c &&
          typeof c.id === "number" &&
          typeof c.name === "string" &&
          typeof c.company === "string" &&
          c.status in statuses &&
          Array.isArray(c.messages) &&
          typeof c.preview === "string" &&
          typeof c.summary === "string",
      )
    )
      return stored as Conversation[];
  } catch {
    /* Sem localStorage, a demo continua em memória. */
  }
  return createInitialData();
}
