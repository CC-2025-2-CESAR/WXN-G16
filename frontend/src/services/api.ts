import type { Atendente, ConversaNaFila, DetalheConversaFila } from '../types/fila';
import type { HealthResponse } from '../types/health';
import {
  abrirDemoConversa,
  assumirDemoConversa,
  encerrarDemoConversa,
  listarDemoAtendentes,
  listarDemoFila,
} from './demo';

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
export const DEMO_ATIVO = import.meta.env.DEV && new URLSearchParams(window.location.search).get('demo') === '1';

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
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    return { status: 'UP' };
  }
  const resposta = await requisitar('/api/health', { signal });
  return resposta.json();
}

export async function listarFila(signal?: AbortSignal): Promise<ConversaNaFila[]> {
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    return listarDemoFila();
  }
  const resposta = await requisitar('/api/fila', { signal });
  return resposta.json();
}

export async function abrirConversa(conversaId: string, signal?: AbortSignal): Promise<DetalheConversaFila> {
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    const conversa = abrirDemoConversa(conversaId);
    if (!conversa) throw new ErroApi('Esta conversa não está mais na fila.', 404);
    return conversa;
  }
  const resposta = await requisitar(`/api/fila/${conversaId}`, { signal });
  return resposta.json();
}

export async function assumirConversa(conversaId: string, atendenteId: string) {
  if (DEMO_ATIVO) {
    const resultado = assumirDemoConversa(conversaId, atendenteId);
    if (resultado === 'conversa-ausente') throw new ErroApi('Esta conversa não está mais na fila.', 409);
    if (resultado === 'atendente-ausente') throw new ErroApi('Atendente não encontrado.', 404);
    if (resultado === 'atendente-indisponivel') throw new ErroApi('Atendente indisponível.', 409);
    return;
  }
  await requisitar(`/api/fila/${conversaId}/assumir`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ atendenteId }),
  });
}

export async function encerrarConversa(conversaId: string) {
  if (DEMO_ATIVO) {
    if (!encerrarDemoConversa(conversaId)) throw new ErroApi('Conversa não encontrada em atendimento.', 404);
    return;
  }
  await requisitar(`/api/conversas/${conversaId}/encerrar`, { method: 'POST' });
}

export async function listarAtendentes(signal?: AbortSignal): Promise<Atendente[]> {
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    return listarDemoAtendentes();
  }
  const resposta = await requisitar('/api/atendentes', { signal });
  return resposta.json();
}
