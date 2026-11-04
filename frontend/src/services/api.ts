import type { Atendente, ConversaNaFila, DetalheConversaFila } from '../types/fila';
import type { HealthResponse } from '../types/health';

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export class ErroApi extends Error {
  status: number;

  constructor(mensagem: string, status: number) {
    super(mensagem);
    this.status = status;
  }
}

const TEMPO_LIMITE_MS = 10_000;
const MENSAGEM_TEMPO_ESGOTADO = 'O servidor demorou demais para responder.';

export function mensagemDoErro(erro: unknown) {
  if (erro instanceof ErroApi) return erro.message;
  if (erro instanceof DOMException && erro.name === 'TimeoutError') return MENSAGEM_TEMPO_ESGOTADO;
  return 'Ocorreu um erro inesperado.';
}

async function detalheDoProblema(resposta: Response) {
  const problema: { detail?: unknown } | null = await resposta.json().catch(() => null);
  return typeof problema?.detail === 'string' && problema.detail
    ? problema.detail
    : `O servidor respondeu com o erro ${resposta.status}.`;
}

async function requisitar(caminho: string, init?: RequestInit) {
  const limite = AbortSignal.timeout(TEMPO_LIMITE_MS);
  const signal = init?.signal ? AbortSignal.any([init.signal, limite]) : limite;
  const resposta = await fetch(`${API_URL}${caminho}`, { ...init, signal }).catch(() => {
    throw new ErroApi(limite.aborted ? MENSAGEM_TEMPO_ESGOTADO : 'Não foi possível falar com o servidor.', 0);
  });

  if (!resposta.ok) {
    throw new ErroApi(await detalheDoProblema(resposta), resposta.status);
  }

  return resposta;
}

export async function getHealth(signal?: AbortSignal): Promise<HealthResponse> {
  const resposta = await requisitar('/api/health', { signal });
  return resposta.json();
}

export async function listarFila(signal?: AbortSignal): Promise<ConversaNaFila[]> {
  const resposta = await requisitar('/api/fila', { signal });
  return resposta.json();
}

export async function abrirConversa(conversaId: string, signal?: AbortSignal): Promise<DetalheConversaFila> {
  const resposta = await requisitar(`/api/fila/${conversaId}`, { signal });
  return resposta.json();
}

export async function assumirConversa(conversaId: string, atendenteId: string) {
  await requisitar(`/api/fila/${conversaId}/assumir`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ atendenteId }),
  });
}

export async function encerrarConversa(conversaId: string) {
  await requisitar(`/api/conversas/${conversaId}/encerrar`, { method: 'POST' });
}

export async function listarAtendentes(signal?: AbortSignal): Promise<Atendente[]> {
  const resposta = await requisitar('/api/atendentes', { signal });
  return resposta.json();
}
