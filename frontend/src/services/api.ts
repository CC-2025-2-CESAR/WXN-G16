import type { Atendente, ConversaNaFila, DetalheConversaFila, EmAtendimento, MensagemResumo, RespostaDoAtendente } from '../types/fila';
import type { HealthResponse } from '../types/health';
import type { FiltrosHistorico, HistoricoDetalhe, PaginaHistorico } from '../types/historico';
import {
  abrirDemoConversa,
  assumirDemoConversa,
  detalharDemoHistorico,
  eConversaSimuladaDemo,
  encerrarDemoConversa,
  enviarDemoMensagem,
  listarDemoAtendimentosAtivos,
  listarDemoAtendentes,
  listarDemoFila,
  listarDemoHistorico,
  listarDemoMensagensDoAtendimento,
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
let tokenCsrf: string | null = null;
let buscaToken: Promise<string> | null = null;

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

export async function obterTokenCsrf(): Promise<string> {
  if (tokenCsrf) return tokenCsrf;
  if (!buscaToken) {
    buscaToken = fetch(`${API_URL}/api/auth/csrf`, { credentials: 'include' })
      .then(async (resposta) => {
        if (!resposta.ok) throw new ErroApi(await detalheDoProblema(resposta), resposta.status);
        const dados: { token?: unknown } = await resposta.json();
        if (typeof dados.token !== 'string' || !dados.token) throw new ErroApi('Não foi possível iniciar a sessão segura.', 0);
        tokenCsrf = dados.token;
        return tokenCsrf;
      })
      .finally(() => { buscaToken = null; });
  }
  return buscaToken;
}

async function requisitar(caminho: string, init?: RequestInit) {
  const limite = AbortSignal.timeout(TEMPO_LIMITE_MS);
  const signal = init?.signal ? AbortSignal.any([init.signal, limite]) : limite;
  const metodo = (init?.method ?? 'GET').toUpperCase();
  const headers = new Headers(init?.headers);
  if (!['GET', 'HEAD', 'OPTIONS'].includes(metodo)) headers.set('X-XSRF-TOKEN', await obterTokenCsrf());
  const resposta = await fetch(`${API_URL}${caminho}`, { ...init, credentials: 'include', headers, signal }).catch(() => {
    throw new ErroApi(limite.aborted ? MENSAGEM_TEMPO_ESGOTADO : 'Não foi possível falar com o servidor.', 0);
  });

  if (!resposta.ok) {
    if (resposta.status === 401 && caminho !== '/api/auth/me' && caminho !== '/api/auth/login') {
      window.dispatchEvent(new Event('wxn:session-expired'));
    }
    throw new ErroApi(await detalheDoProblema(resposta), resposta.status);
  }

  return resposta;
}

export async function sessaoAtual(): Promise<Atendente> {
  const resposta = await requisitar('/api/auth/me');
  return resposta.json();
}

export async function entrar(email: string, senha: string): Promise<Atendente> {
  const resposta = await requisitar('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha }),
  });
  tokenCsrf = null;
  return resposta.json();
}

export async function sair(): Promise<void> {
  await requisitar('/api/auth/logout', { method: 'POST' });
  tokenCsrf = null;
}

export async function listarAtendimentosAtivos(atendenteId: string, signal?: AbortSignal): Promise<EmAtendimento[]> {
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    return listarDemoAtendimentosAtivos(atendenteId);
  }
  const resposta = await requisitar('/api/atendimentos/ativos', { signal });
  const dados: Array<Omit<EmAtendimento, 'assumidaEm'> & { assumidaEm: string | null }> = await resposta.json();
  return dados.map((item) => ({ ...item, assumidaEm: item.assumidaEm ? Date.parse(item.assumidaEm) : null }));
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
  });
}

export async function encerrarConversa(conversaId: string, resumoEncerramento?: string) {
  if (DEMO_ATIVO) {
    if (!encerrarDemoConversa(conversaId, resumoEncerramento)) throw new ErroApi('Conversa não encontrada em atendimento.', 404);
    return;
  }
  await requisitar(`/api/conversas/${conversaId}/encerrar`, {
    method: 'POST',
    ...(resumoEncerramento ? {
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ resumoEncerramento }),
    } : {}),
  });
}

export async function listarHistorico(filtros: FiltrosHistorico, signal?: AbortSignal): Promise<PaginaHistorico> {
  const pagina = filtros.pagina ?? 0;
  const tamanho = filtros.tamanho ?? 20;
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    const normalizar = (valor: string) => valor.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
    const busca = normalizar(filtros.busca?.trim() ?? '');
    const formatoData = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit',
    });
    const dataLocal = (instante: string) => {
      const partes = Object.fromEntries(formatoData.formatToParts(new Date(instante)).map(({ type, value }) => [type, value]));
      return `${partes.year}-${partes.month}-${partes.day}`;
    };
    const itens = listarDemoHistorico().filter((item) => {
      const mensagens = detalharDemoHistorico(item.conversaId)?.mensagens.map((mensagem) => mensagem.conteudo).join(' ') ?? '';
      const texto = normalizar(`${item.clienteNome} ${item.clienteTelefone} ${item.clienteTelefone.replace(/\D/g, '')} ${item.motivo} ${item.resumoEncerramento ?? ''} ${mensagens}`);
      const data = dataLocal(item.encerradaEm);
      return (!busca || texto.includes(busca)) &&
        (!filtros.dataInicial || data >= filtros.dataInicial) &&
        (!filtros.dataFinal || data <= filtros.dataFinal) &&
        (!filtros.atendenteId || item.atendenteId === filtros.atendenteId) &&
        (!filtros.origem || item.origem === filtros.origem);
    });
    return { itens: itens.slice(pagina * tamanho, (pagina + 1) * tamanho), total: itens.length, pagina, tamanho };
  }
  const parametros = new URLSearchParams();
  for (const chave of ['busca', 'dataInicial', 'dataFinal', 'atendenteId', 'origem'] as const) {
    const valor = filtros[chave];
    if (valor) parametros.set(chave, valor);
  }
  parametros.set('pagina', String(pagina));
  parametros.set('tamanho', String(tamanho));
  const resposta = await requisitar(`/api/historico?${parametros}`, { signal });
  return resposta.json();
}

export async function detalharHistorico(conversaId: string, signal?: AbortSignal): Promise<HistoricoDetalhe> {
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    const detalhe = detalharDemoHistorico(conversaId);
    if (!detalhe) throw new ErroApi('Conversa encerrada não encontrada.', 404);
    return detalhe;
  }
  const resposta = await requisitar(`/api/historico/${encodeURIComponent(conversaId)}`, { signal });
  return resposta.json();
}

/** Histórico completo de uma conversa em atendimento, da mais antiga para a mais nova. */
export async function listarMensagensDoAtendimento(conversaId: string, signal?: AbortSignal): Promise<MensagemResumo[]> {
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    const mensagens = listarDemoMensagensDoAtendimento(conversaId);
    if (!mensagens) throw new ErroApi('Conversa não encontrada em atendimento.', 404);
    return mensagens;
  }
  const resposta = await requisitar(`/api/conversas/${conversaId}/atendimento/mensagens`, { signal });
  return resposta.json();
}

/** Envia ao cliente uma mensagem escrita pelo atendente (1 a 4000 caracteres). */
export async function enviarMensagemDoAtendente(conversaId: string, texto: string): Promise<RespostaDoAtendente> {
  if (DEMO_ATIVO) {
    const resultado = enviarDemoMensagem(conversaId, texto);
    if (resultado === 'conversa-ausente') throw new ErroApi('Esta conversa não está em atendimento.', 409);
    if (resultado === 'texto-invalido') throw new ErroApi('Escreva uma mensagem de 1 a 4000 caracteres.', 400);
    return {
      mensagem: resultado,
      entregue: eConversaSimuladaDemo(conversaId),
      aviso: null,
    };
  }
  const resposta = await requisitar(`/api/conversas/${conversaId}/atendimento/mensagens`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ texto }),
  });
  return resposta.json();
}

export async function listarAtendentes(signal?: AbortSignal): Promise<Atendente[]> {
  if (DEMO_ATIVO) {
    signal?.throwIfAborted();
    return listarDemoAtendentes();
  }
  const resposta = await requisitar('/api/atendentes', { signal });
  return resposta.json();
}
