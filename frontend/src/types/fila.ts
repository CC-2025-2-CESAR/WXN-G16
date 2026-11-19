export type TipoRemetente = 'CLIENTE' | 'CHATBOT' | 'ATENDENTE';

/** De onde a conversa veio: dado fictício, teste feito pelo grupo no simulador ou cliente real (WhatsApp ou Telegram). */
export type OrigemConversa = 'DEMONSTRACAO' | 'SIMULADOR' | 'WHATSAPP' | 'TELEGRAM';

export interface ConversaNaFila {
  posicao: number;
  conversaId: string;
  clienteNome: string;
  clienteTelefone: string;
  motivo: string;
  abertoEm: string;
  origem: OrigemConversa;
}

export interface MensagemResumo {
  tipoRemetente: TipoRemetente;
  conteudo: string;
  enviadaEm: string;
}

/**
 * Resposta do envio de uma mensagem do atendente ao cliente.
 * `entregue = false` quer dizer que a mensagem foi salva, mas não chegou ao cliente; `aviso` explica o motivo.
 */
export interface RespostaDoAtendente {
  mensagem: MensagemResumo;
  entregue: boolean;
  aviso: string | null;
}

export interface DetalheConversaFila {
  conversaId: string;
  clienteNome: string;
  clienteTelefone: string;
  motivo: string;
  abertoEm: string;
  origem: OrigemConversa;
  mensagens: MensagemResumo[];
}

export interface Atendente {
  id: string;
  nome: string;
  email: string;
  disponivel: boolean;
}

export interface EmAtendimento {
  detalhe: DetalheConversaFila;
  atendente: Atendente;
  assumidaEm: number;
}
