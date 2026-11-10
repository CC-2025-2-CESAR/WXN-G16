import { API_URL, ErroApi } from '../services/api';
import type { RespostaChat, StatusConversa } from './tipos';

// A resposta passa pela Lily (e, em produção, pelo Gemini) e o servidor gratuito do Azure leva uns 30 s para acordar,
// então o limite é bem maior que os 10 s das telas de fila.
const TEMPO_LIMITE_MS = 60_000;
const MENSAGEM_TEMPO_ESGOTADO = 'A Lily demorou demais para responder.';
const MENSAGEM_CHAT_DESLIGADO =
  'A API de chat está desligada neste servidor. Suba o backend no perfil dev ou defina CHAT_API_HABILITADA=true.';
// Quando chat.api.habilitada é false o controller nem existe, e o backend responde 404 com este texto (TratadorDeErros).
const DETALHE_DE_ROTA_INEXISTENTE = 'O recurso solicitado não existe';
const STATUS_CONHECIDOS: readonly StatusConversa[] = [
  'COM_CHATBOT',
  'AGUARDANDO_ATENDENTE',
  'COM_ATENDENTE',
  'ENCERRADA',
];

type Entrada = { telefone: string; nome: string; texto: string };

async function detalheDoErro(resposta: Response) {
  const problema: { detail?: unknown } | null = await resposta.json().catch(() => null);
  const detalhe = typeof problema?.detail === 'string' ? problema.detail : '';
  if (
    resposta.status === 404 &&
    (!detalhe || detalhe === DETALHE_DE_ROTA_INEXISTENTE || detalhe.startsWith('No static resource'))
  ) {
    return MENSAGEM_CHAT_DESLIGADO;
  }
  return detalhe || `O servidor respondeu com o erro ${resposta.status}.`;
}

function lerResposta(dados: unknown): RespostaChat {
  const corpo = dados as { conversaId?: unknown; resposta?: unknown; status?: unknown } | null;
  if (
    corpo &&
    typeof corpo.conversaId === 'string' &&
    STATUS_CONHECIDOS.includes(corpo.status as StatusConversa)
  ) {
    return {
      conversaId: corpo.conversaId,
      resposta: typeof corpo.resposta === 'string' ? corpo.resposta : null,
      status: corpo.status as StatusConversa,
    };
  }
  throw new ErroApi('O servidor respondeu em um formato inesperado.', 0);
}

/** Manda a mensagem de um telefone para a Lily (POST /api/chat/mensagens), como o canal do WhatsApp faz. */
export async function enviarParaLily(entrada: Entrada, signal?: AbortSignal): Promise<RespostaChat> {
  const limite = AbortSignal.timeout(TEMPO_LIMITE_MS);
  const sinal = signal ? AbortSignal.any([signal, limite]) : limite;
  const resposta = await fetch(`${API_URL}/api/chat/mensagens`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      telefone: entrada.telefone,
      nome: entrada.nome || undefined,
      texto: entrada.texto,
    }),
    signal: sinal,
  }).catch((erro: unknown) => {
    if (signal?.aborted) throw erro;
    throw new ErroApi(limite.aborted ? MENSAGEM_TEMPO_ESGOTADO : 'Não foi possível falar com o servidor.', 0);
  });

  if (!resposta.ok) {
    throw new ErroApi(await detalheDoErro(resposta), resposta.status);
  }
  return lerResposta(await resposta.json().catch(() => null));
}
