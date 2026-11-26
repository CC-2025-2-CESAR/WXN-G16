import { Fragment, useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { SeloDeOrigem } from '../../components/SeloDeOrigem';
import { useMensagensDoAtendimento } from '../../hooks/useMensagensDoAtendimento';
import { DEMO_ATIVO, enviarMensagemDoAtendente, mensagemDoErro } from '../../services/api';
import type { EmAtendimento, MensagemResumo, TipoRemetente } from '../../types/fila';
import { dataHora, dia, formatarTelefone, hora, iniciais } from '../formato';
import { Icon } from '../icons';
import './active-conversation.css';

const LIMITE_MENSAGEM = 4000;

type Props = {
  atendimentos: EmAtendimento[];
  selectedId: string;
  agora: number;
  ocupado: boolean;
  onSelect: (id: string) => void;
  onBack: () => void;
  onResolve: () => void;
  onMessageSent: (conversaId: string, mensagem: MensagemResumo) => void;
};

const tipoMensagem: Record<TipoRemetente, string> = {
  CLIENTE: 'cliente',
  CHATBOT: 'assistente',
  ATENDENTE: 'atendente',
};

export function ActiveConversation({
  atendimentos,
  selectedId,
  agora,
  ocupado,
  onSelect,
  onBack,
  onResolve,
  onMessageSent,
}: Props) {
  const atendimento = atendimentos.find((item) => item.detalhe.conversaId === selectedId);
  const detalhe = atendimento?.detalhe;
  const aoVivo = useMensagensDoAtendimento(selectedId, !!atendimento, detalhe?.mensagens);
  const mensagens = aoVivo.mensagens;
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState<{ erro: boolean; texto: string } | null>(null);
  const rascunhos = useRef<Record<string, string>>({});
  const enviandoRef = useRef(false);
  const conversaRef = useRef<HTMLDivElement>(null);
  const campoRef = useRef<HTMLTextAreaElement>(null);
  const pertoDoFim = useRef(true);

  useEffect(() => {
    setTexto(rascunhos.current[selectedId] ?? '');
    setAviso(null);
    pertoDoFim.current = true;
    campoRef.current?.focus();
  }, [selectedId]);

  useEffect(() => {
    const area = conversaRef.current;
    if (area && pertoDoFim.current) area.scrollTop = area.scrollHeight;
  }, [selectedId, mensagens.length]);

  const aoRolar = () => {
    const area = conversaRef.current;
    if (area) pertoDoFim.current = area.scrollHeight - area.scrollTop - area.clientHeight < 100;
  };

  const enviar = async () => {
    const conteudo = texto.trim();
    if (!atendimento || !conteudo || enviandoRef.current || aoVivo.indisponivel) return;
    const conversaId = selectedId;
    enviandoRef.current = true;
    setEnviando(true);
    setAviso(null);
    try {
      const resposta = await enviarMensagemDoAtendente(conversaId, conteudo);
      aoVivo.adicionar(resposta.mensagem);
      onMessageSent(conversaId, resposta.mensagem);
      delete rascunhos.current[conversaId];
      setTexto('');
      pertoDoFim.current = true;
      if (!resposta.entregue && !DEMO_ATIVO) {
        setAviso({ erro: false, texto: resposta.aviso || 'A mensagem foi salva, mas não chegou ao cliente.' });
      }
    } catch (erro) {
      setAviso({ erro: true, texto: mensagemDoErro(erro) });
    } finally {
      enviandoRef.current = false;
      setEnviando(false);
      campoRef.current?.focus();
    }
  };

  const aoTeclar = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) return;
    event.preventDefault();
    void enviar();
  };

  if (!atendimento || !detalhe) return null;

  return (
    <section className="active-chat" aria-label={`Atendimento de ${detalhe.clienteNome}`}>
      <div className="active-chat-topline">
        <button className="active-chat-back" type="button" onClick={onBack} disabled={enviando}>
          <Icon name="chevron-left" /> Todas as conversas
        </button>
        <span><span className="active-chat-live-dot" /> Atendimento em andamento</span>
      </div>

      <div className="active-chat-workspace">
        <nav className="active-chat-list" aria-label="Suas conversas em atendimento">
          <div className="active-chat-list-heading">
            <span>MINHA CAIXA</span>
            <strong>{atendimentos.length}</strong>
          </div>
          {atendimentos.map((item) => {
            const c = item.detalhe;
            const ultima = c.mensagens.at(-1);
            const precisaResposta = ultima?.tipoRemetente === 'CLIENTE';
            return (
              <button
                type="button"
                key={c.conversaId}
                className={`active-chat-list-item ${selectedId === c.conversaId ? 'selected' : ''}`}
                aria-current={selectedId === c.conversaId ? 'page' : undefined}
                onClick={() => onSelect(c.conversaId)}
                disabled={enviando}
              >
                <span className="avatar">{iniciais(c.clienteNome)}</span>
                <span className="active-chat-list-item-copy">
                  <strong>{c.clienteNome}</strong>
                  <small>{ultima?.conteudo || c.motivo}</small>
                  <span>{precisaResposta ? 'Aguardando sua resposta' : 'Em andamento'}</span>
                </span>
                {precisaResposta && <i aria-label="Nova mensagem do cliente" />}
              </button>
            );
          })}
          <div className="active-chat-list-note">As mensagens são atualizadas automaticamente.</div>
        </nav>

        <div className="active-chat-conversation">
          <header className="active-chat-contact">
            <span className="avatar">{iniciais(detalhe.clienteNome)}</span>
            <div className="active-chat-contact-name">
              <span>CONVERSA COM</span>
              <h2>{detalhe.clienteNome}</h2>
              <small>{formatarTelefone(detalhe.clienteTelefone)} <b aria-hidden="true">·</b> Iniciada em {dataHora(detalhe.abertoEm)}</small>
            </div>
            <div className="active-chat-contact-actions">
              <SeloDeOrigem origem={detalhe.origem} />
              <button type="button" className="active-chat-finish" onClick={onResolve} disabled={ocupado || enviando || aoVivo.indisponivel}>
                <Icon name="check" /> Encerrar
              </button>
            </div>
          </header>

          <div
            className="active-chat-thread"
            ref={conversaRef}
            role="log"
            aria-label={`Mensagens de ${detalhe.clienteNome}`}
            onScroll={aoRolar}
          >
            <div className="active-chat-context">
              <span><Icon name="sparkles" /> CONTEXTO DO ATENDIMENTO</span>
              <p>{detalhe.motivo}</p>
            </div>
            {mensagens.length === 0 && <div className="active-chat-empty">A conversa ainda não tem mensagens.</div>}
            {mensagens.map((mensagem, indice) => {
              const remetente = tipoMensagem[mensagem.tipoRemetente];
              const diaDaMensagem = dia(mensagem.enviadaEm);
              return (
                <Fragment key={`${mensagem.enviadaEm}-${indice}`}>
                  {(indice === 0 || diaDaMensagem !== dia(mensagens[indice - 1].enviadaEm)) && (
                    <div className="active-chat-day"><span>{diaDaMensagem === dia(agora) ? 'Hoje' : diaDaMensagem}</span></div>
                  )}
                  <div className={`active-chat-message ${remetente}`}>
                    <div className="active-chat-message-bubble">
                      {remetente === 'assistente' && <strong>Lily · Assistente virtual</strong>}
                      {remetente === 'atendente' && <strong>Você · Atendente</strong>}
                      <span>{mensagem.conteudo}</span>
                    </div>
                    <small>{hora(mensagem.enviadaEm)}{remetente === 'cliente' ? ' · Cliente' : ''}</small>
                  </div>
                </Fragment>
              );
            })}
          </div>

          {aoVivo.indisponivel && <div className="active-chat-notice error" role="alert">Esta conversa não está mais em atendimento. Volte à lista para escolher outra.</div>}
          {aoVivo.semConexao && !aoVivo.indisponivel && (
            <div className="active-chat-notice" role="status">
              Não foi possível atualizar as mensagens.
              <button type="button" onClick={aoVivo.recarregar}>Tentar agora</button>
            </div>
          )}

          <form className="active-chat-composer" onSubmit={(event) => { event.preventDefault(); void enviar(); }}>
            <div className="active-chat-composer-heading"><span className="active-chat-live-dot" /> Respondendo como {atendimento.atendente.nome}</div>
            {aviso && <div className={`active-chat-notice ${aviso.erro ? 'error' : ''}`} role="alert">{aviso.texto}</div>}
            <label className="sr-only" htmlFor="active-chat-reply">Mensagem para {detalhe.clienteNome}</label>
            <textarea
              id="active-chat-reply"
              ref={campoRef}
              value={texto}
              rows={2}
              maxLength={LIMITE_MENSAGEM}
              placeholder={`Escreva sua resposta para ${detalhe.clienteNome}…`}
              disabled={enviando || aoVivo.indisponivel}
              onChange={(event) => {
                const valor = event.target.value;
                rascunhos.current[selectedId] = valor;
                setTexto(valor);
              }}
              onKeyDown={aoTeclar}
            />
            <div className="active-chat-composer-bottom">
              <span>Enter envia · Shift+Enter quebra a linha {texto.length > LIMITE_MENSAGEM - 500 && `· ${texto.length}/${LIMITE_MENSAGEM}`}</span>
              <button type="submit" disabled={enviando || aoVivo.indisponivel || !texto.trim()}>
                {enviando ? 'Enviando…' : 'Enviar mensagem'} <Icon name="send" />
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>
  );
}
