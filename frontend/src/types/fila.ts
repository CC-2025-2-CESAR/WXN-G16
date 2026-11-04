export type TipoRemetente = 'CLIENTE' | 'CHATBOT' | 'ATENDENTE';

export interface ConversaNaFila {
  posicao: number;
  conversaId: string;
  clienteNome: string;
  clienteTelefone: string;
  motivo: string;
  abertoEm: string;
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
