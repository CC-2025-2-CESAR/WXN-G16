import type { MensagemResumo, NivelUrgencia, OrigemConversa } from './fila';

export interface HistoricoItem {
  conversaId: string;
  clienteNome: string;
  clienteTelefone: string;
  motivo: string;
  origem: OrigemConversa;
  urgencia: NivelUrgencia | null;
  atendenteId: string | null;
  atendenteNome: string | null;
  iniciadaEm: string;
  assumidaEm: string | null;
  encerradaEm: string;
  resumoEncerramento: string | null;
  totalMensagens: number;
}

export interface HistoricoDetalhe extends HistoricoItem {
  mensagens: MensagemResumo[];
}

export interface PaginaHistorico {
  itens: HistoricoItem[];
  total: number;
  pagina: number;
  tamanho: number;
}

export interface FiltrosHistorico {
  busca?: string;
  dataInicial?: string;
  dataFinal?: string;
  atendenteId?: string;
  origem?: OrigemConversa;
  pagina?: number;
  tamanho?: number;
}
