import { Fragment, useEffect, useRef, useState, type ChangeEvent, type FormEvent, type KeyboardEvent } from 'react';
import { dia, formatarTelefone, hora } from '../dashboard/formato';
import type { Cliente, EstadoDoEnvio, MensagemSimulada, StatusConversa } from './tipos';
import type { ConversaSimulada } from './useConversaSimulada';

const LIMITE_DO_TEXTO = 4000;
const ALTURA_MAXIMA_DO_CAMPO = 120;

/** Com a conversa na mão de um atendente, a Lily fica calada: não faz sentido mostrar que ela está digitando. */
const lilyVaiResponder = (status: StatusConversa | null) =>
  status !== 'AGUARDANDO_ATENDENTE' && status !== 'COM_ATENDENTE';

function subtituloDoContato(status: StatusConversa | null, enviando: boolean, lento: boolean) {
  if (enviando && lento) return 'acordando o servidor…';
  if (enviando && lilyVaiResponder(status)) return 'digitando…';
  if (status === 'AGUARDANDO_ATENDENTE') return 'aguardando atendente';
  if (status === 'COM_ATENDENTE') return 'com atendente';
  return 'online';
}

const mesmoDia = (a: number, b: number) => new Date(a).toDateString() === new Date(b).toDateString();
const rotuloDoDia = (instante: number) => (mesmoDia(instante, Date.now()) ? 'HOJE' : dia(instante));

function Selo({ estado }: { estado: EstadoDoEnvio }) {
  if (estado === 'enviando') {
    return (
      <svg className="sim-selo" viewBox="0 0 14 14" role="img" aria-label="Enviando">
        <circle cx="7" cy="7" r="5.2" />
        <path d="M7 4.2V7l1.9 1.2" />
      </svg>
    );
  }
  if (estado === 'falhou') {
    return (
      <svg className="sim-selo sim-selo--falhou" viewBox="0 0 14 14" role="img" aria-label="Não enviada">
        <circle cx="7" cy="7" r="5.2" />
        <path d="M7 4v3.6M7 10v.1" />
      </svg>
    );
  }
  return (
    <svg className="sim-selo sim-selo--lida" viewBox="0 0 16 11" role="img" aria-label="Lida">
      <path d="M1 6l3 3 6.2-7.6M6.4 8.6l.9.9 6.6-8" />
    </svg>
  );
}

type BolhaProps = { mensagem: MensagemSimulada; onReenviar: (id: string) => void; semEnviar: boolean };

function Bolha({ mensagem, onReenviar, semEnviar }: BolhaProps) {
  if (mensagem.de === 'sistema') {
    return <div className="sim-aviso">{mensagem.texto}</div>;
  }
  const doCliente = mensagem.de === 'cliente';
  return (
    <div className={`sim-msg sim-msg--${mensagem.de}`}>
      <span className="sim-sr">{doCliente ? 'Você: ' : 'Lily: '}</span>
      <span className="sim-texto">{mensagem.texto}</span>
      <span className="sim-meta">
        <time dateTime={new Date(mensagem.em).toISOString()}>{hora(mensagem.em)}</time>
        {doCliente && <Selo estado={mensagem.estado ?? 'lida'} />}
      </span>
      {mensagem.estado === 'falhou' && (
        <button type="button" className="sim-reenviar" disabled={semEnviar} onClick={() => onReenviar(mensagem.id)}>
          Não enviada. Tentar de novo
        </button>
      )}
    </div>
  );
}

type Props = { cliente: Cliente; conversa: ConversaSimulada };

/** A conversa desenhada como um celular com WhatsApp: o cliente escreve, a Lily responde. */
export function Celular({ cliente, conversa }: Props) {
  const { mensagens, enviando, lento, erro, status, enviar, reenviar } = conversa;
  const [rascunho, setRascunho] = useState('');
  const lista = useRef<HTMLDivElement>(null);
  const noFim = useRef(true);

  // Mensagem nova leva ao fim da conversa, como no WhatsApp.
  useEffect(() => {
    const el = lista.current;
    if (el) el.scrollTop = el.scrollHeight;
    noFim.current = true;
  }, [mensagens, enviando]);

  // O aviso de servidor lento e o campo que cresce encolhem a lista: quem estava no fim continua no fim.
  useEffect(() => {
    const el = lista.current;
    if (!el) return;
    const observador = new ResizeObserver(() => {
      if (noFim.current) el.scrollTop = el.scrollHeight;
    });
    observador.observe(el);
    return () => observador.disconnect();
  }, []);

  const aoRolar = () => {
    const el = lista.current;
    if (el) noFim.current = el.scrollHeight - el.scrollTop - el.clientHeight < 40;
  };

  const enviarRascunho = (campo: HTMLTextAreaElement | null) => {
    if (enviando || !rascunho.trim()) return;
    void enviar(rascunho);
    setRascunho('');
    if (campo) campo.style.height = 'auto';
  };

  const aoDigitar = (e: ChangeEvent<HTMLTextAreaElement>) => {
    setRascunho(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = `${Math.min(e.target.scrollHeight, ALTURA_MAXIMA_DO_CAMPO)}px`;
  };

  const aoTeclar = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      enviarRascunho(e.currentTarget);
    }
  };

  const aoEnviar = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const campo = e.currentTarget.querySelector('textarea');
    enviarRascunho(campo);
    // Clicar em enviar não pode tirar o foco: no celular, isso fecharia o teclado.
    campo?.focus();
  };

  return (
    <section className="sim-celular" aria-label="Celular do cliente simulado">
      <header className="sim-topo">
        <span className="sim-avatar" aria-hidden="true">
          L
        </span>
        <div className="sim-contato">
          <strong>Lily · WXN</strong>
          <small>{subtituloDoContato(status, enviando, lento)}</small>
        </div>
        <span className="sim-quem">
          {cliente.nome || 'Cliente'}
          <small>{formatarTelefone(cliente.telefone)}</small>
        </span>
      </header>

      <div
        className="sim-conversa"
        ref={lista}
        role="log"
        aria-label="Mensagens da conversa"
        tabIndex={0}
        onScroll={aoRolar}
      >
        <div className="sim-aviso sim-aviso--simulador">
          Simulador: as mensagens vão direto para a API da Lily, sem passar pelo WhatsApp.
        </div>
        {mensagens.map((mensagem, i) => (
          <Fragment key={mensagem.id}>
            {(i === 0 || !mesmoDia(mensagens[i - 1].em, mensagem.em)) && (
              <div className="sim-dia">{rotuloDoDia(mensagem.em)}</div>
            )}
            <Bolha mensagem={mensagem} onReenviar={reenviar} semEnviar={enviando} />
          </Fragment>
        ))}
        {enviando && lilyVaiResponder(status) && (
          <div className="sim-msg sim-msg--lily sim-digitando" role="status">
            <span className="sim-sr">A Lily está digitando</span>
            <i aria-hidden="true" />
            <i aria-hidden="true" />
            <i aria-hidden="true" />
          </div>
        )}
      </div>

      {lento && (
        <p className="sim-lento" role="status">
          O servidor está acordando. A primeira resposta pode levar até 30 segundos.
        </p>
      )}
      {erro && (
        <p className="sim-erro" role="alert">
          {erro}
        </p>
      )}

      <form className="sim-entrada" onSubmit={aoEnviar}>
        <textarea
          rows={1}
          value={rascunho}
          maxLength={LIMITE_DO_TEXTO}
          placeholder="Mensagem"
          aria-label="Mensagem para a Lily"
          onChange={aoDigitar}
          onKeyDown={aoTeclar}
        />
        <button type="submit" className="sim-enviar" aria-label="Enviar" aria-disabled={enviando || !rascunho.trim()}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M2.01 21 23 12 2.01 3 2 10l15 2-15 2z" />
          </svg>
        </button>
      </form>
    </section>
  );
}
