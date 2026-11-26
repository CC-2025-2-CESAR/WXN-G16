import type { OrigemConversa } from '../types/fila';
import './SeloDeOrigem.css';

const SELOS: Record<OrigemConversa, { rotulo: string; dica: string }> = {
  DEMONSTRACAO: {
    rotulo: '',
    dica: '',
  },
  SIMULADOR: {
    rotulo: 'Simulador',
    dica: 'Conversa de teste, escrita por alguém do grupo no simulador de WhatsApp.',
  },
  WHATSAPP: {
    rotulo: 'WhatsApp',
    dica: 'Cliente de verdade, pelo WhatsApp.',
  },
  TELEGRAM: {
    rotulo: 'Telegram',
    dica: 'Cliente de verdade, pelo Telegram.',
  },
};

/** Mostra o canal das conversas recebidas pelos canais conectados. */
export function SeloDeOrigem({ origem }: { origem: OrigemConversa }) {
  // Um servidor mais antigo pode não mandar a origem: sem ela, a tela só não mostra o selo.
  const selo = SELOS[origem];
  if (!selo?.rotulo) return null;
  return (
    <span className={`selo-origem selo-origem--${origem.toLowerCase()}`} title={selo.dica}>
      {selo.rotulo}
    </span>
  );
}
