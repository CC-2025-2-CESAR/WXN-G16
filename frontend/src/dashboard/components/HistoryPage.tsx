import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { SeloDeOrigem } from '../../components/SeloDeOrigem';
import { DEMO_ATIVO, detalharHistorico, listarAtendentes, listarHistorico, mensagemDoErro } from '../../services/api';
import type { Atendente, MensagemResumo, OrigemConversa, TipoRemetente } from '../../types/fila';
import type { HistoricoDetalhe, HistoricoItem, PaginaHistorico } from '../../types/historico';
import { dia, duracao, formatarTelefone, hora, iniciais } from '../formato';
import { Icon } from '../icons';
import './history-page.css';

type Periodo = 'hoje' | '7' | '30' | '90' | 'todos';
const TAMANHO_PAGINA = 10;
const ORIGENS: { valor: OrigemConversa; rotulo: string }[] = [
  { valor: 'WHATSAPP', rotulo: 'WhatsApp' },
  { valor: 'SIMULADOR', rotulo: 'Simulador' },
  { valor: 'TELEGRAM', rotulo: 'Telegram' },
];
const fuso = 'America/Sao_Paulo';
const formatoData = new Intl.DateTimeFormat('en-CA', {
  timeZone: fuso, year: 'numeric', month: '2-digit', day: '2-digit',
});
const formatoDataHora = new Intl.DateTimeFormat('pt-BR', {
  timeZone: fuso, day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

function dataLocal(instante: Date): string {
  const partes = Object.fromEntries(formatoData.formatToParts(instante).map(({ type, value }) => [type, value]));
  return `${partes.year}-${partes.month}-${partes.day}`;
}

function inicioDoPeriodo(periodo: Periodo): string | undefined {
  if (periodo === 'todos') return undefined;
  if (periodo === 'hoje') return dataLocal(new Date());
  const [ano, mes, diaAtual] = dataLocal(new Date()).split('-').map(Number);
  return new Date(Date.UTC(ano, mes - 1, diaAtual - Number(periodo) + 1)).toISOString().slice(0, 10);
}

function idDaRota(): string | null {
  const prefixo = '#/historico/';
  if (!window.location.hash.startsWith(prefixo)) return null;
  try { return decodeURIComponent(window.location.hash.slice(prefixo.length)) || null; }
  catch { return null; }
}

const rotuloDoRemetente: Record<TipoRemetente, string> = {
  CLIENTE: 'Cliente', CHATBOT: 'Lily · Assistente virtual', ATENDENTE: 'Atendente',
};

function Detalhe({
  id, item, carregando, erro, onVoltar, onRecarregar,
}: {
  id: string;
  item: HistoricoDetalhe | null;
  carregando: boolean;
  erro: string | null;
  onVoltar: () => void;
  onRecarregar: () => void;
}) {
  return (
    <section className="history-detail" aria-label="Conversa encerrada">
      <button type="button" className="history-back" onClick={onVoltar}>
        <Icon name="chevron-left" /> Voltar ao histórico
      </button>
      {carregando && !item && <div className="history-detail-state" role="status">Carregando conversa…</div>}
      {erro && !item && (
        <div className="history-detail-state" role="alert">
          <strong>Não foi possível abrir esta conversa.</strong>
          <span>{erro}</span>
          <button type="button" className="button secondary" onClick={onRecarregar}>Tentar novamente</button>
        </div>
      )}
      {item && (
        <>
          <header className="history-detail-header">
            <span className="avatar history-avatar">{iniciais(item.clienteNome)}</span>
            <div className="history-detail-person">
              <span>CONVERSA ENCERRADA</span>
              <h2>{item.clienteNome}</h2>
              <p>{formatarTelefone(item.clienteTelefone)}</p>
            </div>
            <SeloDeOrigem origem={item.origem} />
          </header>

          <div className="history-facts">
            <div><span>Iniciada em</span><strong>{formatoDataHora.format(new Date(item.iniciadaEm))}</strong></div>
            <div><span>Encerrada em</span><strong>{formatoDataHora.format(new Date(item.encerradaEm))}</strong></div>
            <div><span>Tempo total</span><strong>{duracao(Date.parse(item.encerradaEm) - Date.parse(item.iniciadaEm))}</strong></div>
            <div><span>Atendente</span><strong>{item.atendenteNome ?? 'Não atribuído'}</strong></div>
          </div>

          <div className="history-context-grid">
            <div className="history-context"><span>MOTIVO DO CONTATO</span><p>{item.motivo || 'Motivo não informado.'}</p></div>
            <div className="history-context"><span>RESUMO DO ENCERRAMENTO</span><p>{item.resumoEncerramento || 'Nenhuma observação registrada.'}</p></div>
          </div>

          <div className="history-thread-heading">
            <h3>Conversa completa</h3>
            <span>{item.totalMensagens} {item.totalMensagens === 1 ? 'mensagem' : 'mensagens'}</span>
          </div>
          <div className="history-thread" role="log" aria-label={`Mensagens de ${item.clienteNome}`} tabIndex={0}>
            {item.mensagens.length === 0 && <p className="history-no-messages">Nenhuma mensagem registrada.</p>}
            {item.mensagens.map((mensagem: MensagemResumo, indice) => {
              const diaAtual = dia(mensagem.enviadaEm);
              const mostrarDia = indice === 0 || diaAtual !== dia(item.mensagens[indice - 1].enviadaEm);
              const classe = mensagem.tipoRemetente.toLowerCase();
              const autor = mensagem.tipoRemetente === 'ATENDENTE'
                ? item.atendenteNome ?? rotuloDoRemetente.ATENDENTE
                : rotuloDoRemetente[mensagem.tipoRemetente];
              return (
                <div key={`${id}-${mensagem.enviadaEm}-${indice}`}>
                  {mostrarDia && <div className="history-day"><span>{diaAtual}</span></div>}
                  <div className={`history-message history-message--${classe}`}>
                    <div className="history-bubble"><strong>{autor}</strong><span>{mensagem.conteudo}</span></div>
                    <time dateTime={mensagem.enviadaEm}>{hora(mensagem.enviadaEm)}</time>
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}
    </section>
  );
}

export function HistoryPage({ atendente }: { atendente: Atendente }) {
  const buscaRef = useRef<HTMLInputElement>(null);
  const [buscaDigitada, setBuscaDigitada] = useState('');
  const [busca, setBusca] = useState('');
  const [periodo, setPeriodo] = useState<Periodo>('30');
  const [atendenteId, setAtendenteId] = useState('');
  const [origem, setOrigem] = useState<OrigemConversa | ''>('');
  const [pagina, setPagina] = useState(0);
  const [versao, setVersao] = useState(0);
  const [resultado, setResultado] = useState<PaginaHistorico | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [atendentes, setAtendentes] = useState<Atendente[]>([]);
  const [selecionadaId, setSelecionadaId] = useState(idDaRota);
  const [detalhe, setDetalhe] = useState<HistoricoDetalhe | null>(null);
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(() => Boolean(idDaRota()));
  const [erroDetalhe, setErroDetalhe] = useState<string | null>(null);
  const [versaoDetalhe, setVersaoDetalhe] = useState(0);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const proxima = buscaDigitada.trim();
      if (proxima !== busca) {
        setResultado(null);
        setCarregando(true);
        setBusca(proxima);
      }
    }, 250);
    return () => window.clearTimeout(timer);
  }, [buscaDigitada, busca]);

  useEffect(() => {
    const controle = new AbortController();
    void listarAtendentes(controle.signal).then(setAtendentes, () => {});
    return () => controle.abort();
  }, []);

  useEffect(() => {
    const aoMudar = () => {
      const id = idDaRota();
      setSelecionadaId(id);
      setDetalhe(null);
      setErroDetalhe(null);
      setCarregandoDetalhe(Boolean(id));
    };
    window.addEventListener('hashchange', aoMudar);
    return () => window.removeEventListener('hashchange', aoMudar);
  }, []);

  useEffect(() => {
    const focarBusca = (evento: globalThis.KeyboardEvent) => {
      if (evento.key !== '/' || selecionadaId || evento.ctrlKey || evento.metaKey || evento.altKey) return;
      const alvo = evento.target;
      if (alvo instanceof HTMLElement && (alvo.isContentEditable || alvo.closest('input, textarea, select, [contenteditable]'))) return;
      evento.preventDefault();
      buscaRef.current?.focus();
    };
    window.addEventListener('keydown', focarBusca);
    return () => window.removeEventListener('keydown', focarBusca);
  }, [selecionadaId]);

  useEffect(() => {
    const controle = new AbortController();
    void listarHistorico({
      busca, dataInicial: inicioDoPeriodo(periodo),
      atendenteId: atendenteId || undefined,
      origem: origem || undefined,
      pagina, tamanho: TAMANHO_PAGINA,
    }, controle.signal).then(
      (paginaRecebida) => { if (!controle.signal.aborted) { setResultado(paginaRecebida); setErro(null); } },
      (falha) => { if (!controle.signal.aborted) setErro(mensagemDoErro(falha)); },
    ).finally(() => { if (!controle.signal.aborted) setCarregando(false); });
    return () => controle.abort();
  }, [busca, periodo, atendenteId, origem, pagina, versao]);

  useEffect(() => {
    if (!DEMO_ATIVO) return;
    const atualizar = () => { setCarregando(true); setVersao((valor) => valor + 1); };
    window.addEventListener('wxn:demo-conversations-changed', atualizar);
    return () => window.removeEventListener('wxn:demo-conversations-changed', atualizar);
  }, []);

  useEffect(() => {
    if (!selecionadaId) return;
    const controle = new AbortController();
    void detalharHistorico(selecionadaId, controle.signal).then(
      (registro) => { if (!controle.signal.aborted) setDetalhe(registro); },
      (falha) => { if (!controle.signal.aborted) setErroDetalhe(mensagemDoErro(falha)); },
    ).finally(() => { if (!controle.signal.aborted) setCarregandoDetalhe(false); });
    return () => controle.abort();
  }, [selecionadaId, versaoDetalhe]);

  const abrir = (id: string) => {
    window.location.hash = `#/historico/${encodeURIComponent(id)}`;
  };
  const voltar = () => {
    window.location.hash = '#/historico';
  };
  const tecladoDaLinha = (evento: KeyboardEvent<HTMLTableRowElement>, id: string) => {
    if (evento.key === 'Enter' || evento.key === ' ') { evento.preventDefault(); abrir(id); }
  };
  const mudarFiltro = (mudar: () => void) => { setResultado(null); setCarregando(true); setPagina(0); mudar(); };
  const itens = resultado?.itens ?? [];
  const total = resultado?.total ?? 0;
  const inicio = total ? pagina * TAMANHO_PAGINA + 1 : 0;
  const fim = Math.min((pagina + 1) * TAMANHO_PAGINA, total);
  const paginas = Math.max(1, Math.ceil(total / TAMANHO_PAGINA));

  if (selecionadaId) return (
    <Detalhe
      id={selecionadaId} item={detalhe} carregando={carregandoDetalhe} erro={erroDetalhe}
      onVoltar={voltar} onRecarregar={() => {
        setDetalhe(null);
        setErroDetalhe(null);
        setCarregandoDetalhe(true);
        setVersaoDetalhe((valor) => valor + 1);
      }}
    />
  );

  return (
    <section className="history-card" aria-label="Histórico de conversas">
      <div className="history-toolbar">
        <div className="history-title">
          <h2>Conversas encerradas <span>{total}</span></h2>
          <p>Busque por cliente, assunto ou mensagem e consulte a conversa completa.</p>
        </div>
        <button type="button" className="button secondary history-refresh" onClick={() => { setCarregando(true); setVersao((valor) => valor + 1); }} disabled={carregando}>
          <Icon name="refresh" /> Atualizar histórico
        </button>
      </div>

      <div className="history-filters">
        <label className="history-search">
          <Icon name="search" />
          <span className="sr-only">Buscar no histórico</span>
          <input
            ref={buscaRef}
            type="search" value={buscaDigitada}
            onChange={(evento) => { setPagina(0); setBuscaDigitada(evento.target.value); }}
            placeholder="Cliente ou mensagem…"
          />
          <kbd aria-hidden="true">/</kbd>
        </label>
        <label><span>Período</span><select value={periodo} onChange={(evento) => mudarFiltro(() => setPeriodo(evento.target.value as Periodo))}>
          <option value="hoje">Hoje</option>
          <option value="7">Últimos 7 dias</option>
          <option value="30">Últimos 30 dias</option>
          <option value="90">Últimos 90 dias</option>
          <option value="todos">Todo o período</option>
        </select></label>
        <label><span>Atendente</span><select value={atendenteId} onChange={(evento) => mudarFiltro(() => setAtendenteId(evento.target.value))}>
          <option value="">Todos</option>
          <option value={atendente.id}>Meus atendimentos</option>
          {atendentes.filter((pessoa) => pessoa.id !== atendente.id).map((pessoa) => (
            <option key={pessoa.id} value={pessoa.id}>{pessoa.nome}</option>
          ))}
        </select></label>
        <label><span>Canal</span><select value={origem} onChange={(evento) => mudarFiltro(() => setOrigem(evento.target.value as OrigemConversa | ''))}>
          <option value="">Todos</option>
          {ORIGENS.map((item) => <option key={item.valor} value={item.valor}>{item.rotulo}</option>)}
        </select></label>
      </div>

      {erro && <div className="history-error" role="alert">{erro} <button type="button" onClick={() => { setCarregando(true); setVersao((valor) => valor + 1); }}>Tentar novamente</button></div>}
      <div className="history-table-scroll" aria-busy={carregando}>
        <table className="history-table">
          <thead><tr><th>CLIENTE</th><th>MOTIVO</th><th>ATENDENTE</th><th>CANAL</th><th>ENCERRADA EM</th><th><span className="sr-only">Abrir</span></th></tr></thead>
          <tbody>
            {itens.map((item: HistoricoItem) => (
              <tr key={item.conversaId} tabIndex={0} onClick={() => abrir(item.conversaId)} onKeyDown={(evento) => tecladoDaLinha(evento, item.conversaId)}>
                <td><div className="history-client"><span className="avatar">{iniciais(item.clienteNome)}</span><span><strong>{item.clienteNome}</strong><small>{formatarTelefone(item.clienteTelefone)}</small></span></div></td>
                <td><span className="history-reason">{item.motivo || 'Motivo não informado'}</span></td>
                <td><span className="history-agent">{item.atendenteNome ?? 'Não atribuído'}</span></td>
                <td>{item.origem === 'DEMONSTRACAO' ? <span className="history-channel-muted">—</span> : <SeloDeOrigem origem={item.origem} />}</td>
                <td><time dateTime={item.encerradaEm} className="history-date">{formatoDataHora.format(new Date(item.encerradaEm))}</time></td>
                <td><button type="button" className="history-row-action" aria-label={`Abrir histórico de ${item.clienteNome}`} onClick={(evento) => { evento.stopPropagation(); abrir(item.conversaId); }}><Icon name="chevron-right" /></button></td>
              </tr>
            ))}
          </tbody>
        </table>
        {carregando && !resultado && <div className="history-empty" role="status"><Icon name="history" /><strong>Carregando histórico…</strong></div>}
        {!carregando && !erro && total === 0 && (
          <div className="history-empty"><Icon name={busca || periodo !== 'todos' || atendenteId || origem ? 'search' : 'history'} />
            <strong>Nenhuma conversa encontrada</strong>
            <p>{busca || atendenteId || origem || periodo !== 'todos' ? 'Ajuste os filtros ou escolha outro período.' : 'As conversas encerradas aparecerão aqui.'}</p>
          </div>
        )}
      </div>
      <footer className="history-footer">
        <span>{total ? `Mostrando ${inicio}–${fim} de ${total} ${total === 1 ? 'conversa' : 'conversas'}` : 'Nenhuma conversa'}</span>
        <div className="history-pagination">
          <button type="button" aria-label="Página anterior" disabled={pagina === 0 || carregando} onClick={() => { setResultado(null); setCarregando(true); setPagina(pagina - 1); }}><Icon name="chevron-left" /></button>
          <span>{pagina + 1} de {paginas}</span>
          <button type="button" aria-label="Próxima página" disabled={pagina + 1 >= paginas || carregando} onClick={() => { setResultado(null); setCarregando(true); setPagina(pagina + 1); }}><Icon name="chevron-right" /></button>
        </div>
      </footer>
    </section>
  );
}
