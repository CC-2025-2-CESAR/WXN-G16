export type Remetente = 'cliente' | 'lily' | 'sistema';

export type StatusConversa = 'COM_CHATBOT' | 'AGUARDANDO_ATENDENTE' | 'COM_ATENDENTE' | 'ENCERRADA';

/** enviando: a espera da resposta; lida: a Lily já processou; falhou: o envio deu erro. */
export type EstadoDoEnvio = 'enviando' | 'lida' | 'falhou';

export type MensagemSimulada = {
  id: string;
  de: Remetente;
  texto: string;
  /** Instante em milissegundos desde 1970. */
  em: number;
  /** Só existe nas mensagens do cliente. */
  estado?: EstadoDoEnvio;
};

/** Corpo devolvido por POST /api/chat/mensagens. A resposta vem nula quando a conversa já está com um atendente. */
export type RespostaChat = {
  conversaId: string;
  resposta: string | null;
  status: StatusConversa;
};

export type Cliente = {
  /** Só dígitos, com DDD (10 a 13 dígitos, com ou sem o 55). */
  telefone: string;
  nome: string;
};
