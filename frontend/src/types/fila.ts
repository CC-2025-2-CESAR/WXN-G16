export type TipoRemetente = 'CLIENTE' | 'CHATBOT' | 'ATENDENTE';

/** De onde a conversa veio: dado fictício, teste feito pelo grupo no simulador ou cliente real pelo WhatsApp. */
export type OrigemConversa = 'DEMONSTRACAO' | 'SIMULADOR' | 'WHATSAPP';

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
