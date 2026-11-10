import { Fragment, useEffect, useRef } from "react";
import { SeloDeOrigem } from "../../components/SeloDeOrigem";
import { useDetalheFila } from "../../hooks/useDetalheFila";
import type {
  Atendente,
  ConversaNaFila,
  DetalheConversaFila,
  EmAtendimento,
  TipoRemetente,
} from "../../types/fila";
import {
  dataHora,
  dia,
  esperaDesde,
  formatarTelefone,
  hora,
  iniciais,
} from "../formato";
import { Icon } from "../icons";

const kinds = {
  CLIENTE: "client",
  CHATBOT: "bot",
  ATENDENTE: "agent",
} satisfies Record<TipoRemetente, string>;
type Props = {
  conversa: ConversaNaFila | undefined;
  atendimento: EmAtendimento | undefined;
  atendente: Atendente | null;
  agora: number;
  ocupado: boolean;
  onClose: () => void;
  onAssume: (detalhe: DetalheConversaFila) => void;
  onResolve: () => void;
  onSettings: () => void;
  onIndisponivel: () => void;
};
export function ConversationDetail({
  conversa,
  atendimento,
  atendente,
  agora,
  ocupado,
  onClose,
  onAssume,
  onResolve,
  onSettings,
  onIndisponivel,
}: Props) {
  const busca = useDetalheFila(
    conversa && !atendimento ? conversa.conversaId : null,
  );
  const mensagens = atendimento?.detalhe.mensagens ?? busca.detalhe?.mensagens;
  const total = mensagens?.length;
  const threadRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight });
  }, [total, atendimento]);
  useEffect(() => {
    if (busca.indisponivel) onIndisponivel();
  }, [busca.indisponivel, onIndisponivel]);
  const c = atendimento?.detalhe ?? conversa;
  if (!c)
    return (
      <div className="empty-detail">
        <Icon name="messages" />
        <h2>Uma conversa de cada vez</h2>
        <p>
          Selecione um cliente na fila para visualizar o histórico e iniciar o
          atendimento.
        </p>
      </div>
    );
  const responsavel = atendimento?.atendente ?? atendente;
  const ultima = mensagens?.at(-1);
  const metadados = [
    ["Na fila desde", dataHora(c.abertoEm)],
    atendimento
      ? ["Assumida às", hora(atendimento.assumidaEm)]
      : ["Aguardando há", esperaDesde(c.abertoEm, agora)],
    ["Mensagens", String(total ?? "—")],
    ["Última mensagem", ultima ? hora(ultima.enviadaEm) : "—"],
  ];
  return (
    <>
      <header className="detail-header">
        <div>
          <div className="eyebrow">CONVERSA SELECIONADA</div>
          <h2>{atendimento ? "Em atendimento" : "Aguardando atendente"}</h2>
        </div>
        <button
          className="icon-button"
          onClick={onClose}
          aria-label="Fechar conversa"
        >
          <Icon name="close" />
        </button>
      </header>
      <section className="contact-summary" aria-label="Dados do cliente">
        <div className="contact-top">
          <span className="avatar">{iniciais(c.clienteNome)}</span>
          <div>
            <h3 className="contact-name">{c.clienteNome}</h3>
            <div className="contact-phone">
              {formatarTelefone(c.clienteTelefone)}
            </div>
            <SeloDeOrigem origem={c.origem} />
          </div>
          <span className={`badge ${atendimento ? "active" : "waiting"}`}>
            {atendimento ? "Em atendimento" : "Aguardando humano"}
          </span>
        </div>
        <div className="contact-metadata">
          {metadados.map(([label, value]) => (
            <div key={label}>
              <div className="metadata-label">{label}</div>
              <div className="metadata-value">{value}</div>
            </div>
          ))}
        </div>
      </section>
      <section className="reason" aria-label="Motivo da solicitação">
        <div className="summary-title">
          <Icon name="users" />
          MOTIVO DA SOLICITAÇÃO
        </div>
        <p>{c.motivo}</p>
      </section>
      <div
        className="chat-thread"
        ref={threadRef}
        role="log"
        aria-label={`Histórico de ${c.clienteNome}`}
        aria-busy={busca.carregando}
      >
        {busca.carregando && (
          <div className="system-message">Carregando mensagens…</div>
        )}
        {busca.erro && (
          <div className="error-banner" role="alert">
            <span>{busca.erro}</span>
            {!busca.indisponivel && (
              <button className="text-button" onClick={busca.recarregar}>
                Tentar novamente
              </button>
            )}
          </div>
        )}
        {mensagens?.length === 0 && (
          <div className="system-message">Nenhuma mensagem nesta conversa.</div>
        )}
        {mensagens?.map((m, i) => {
          const kind = kinds[m.tipoRemetente];
          const dayLabel = dia(m.enviadaEm);
          return (
            <Fragment key={`${m.enviadaEm}-${i}`}>
              {(i === 0 || dayLabel !== dia(mensagens[i - 1].enviadaEm)) && (
                <div className="day-separator">
                  {dayLabel === dia(agora) ? "Hoje" : dayLabel}
                </div>
              )}
              <div
                className={`chat-message ${kind === "client" ? "outbound" : ""}`}
              >
                <div className="bubble">
                  {kind === "bot" && (
                    <div className="bubble-sender">
                      Lily · Assistente virtual
                    </div>
                  )}
                  {kind === "agent" && (
                    <div className="bubble-sender">Atendente</div>
                  )}
                  {m.conteudo}
                </div>
                <div className="message-meta">
                  {hora(m.enviadaEm)}
                  {kind === "client" && " · Cliente"}
                </div>
              </div>
            </Fragment>
          );
        })}
        {atendimento && (
          <div className="system-message">
            Histórico até o momento em que você assumiu a conversa.
          </div>
        )}
      </div>
      <footer className="composer">
        <div className="composer-top">
          <strong>
            {atendimento
              ? "Você está no atendimento"
              : "Aguardando um atendente"}
          </strong>
          {responsavel ? (
            <span>
              Como {responsavel.nome}
              {!responsavel.disponivel && " (indisponível)"}
            </span>
          ) : (
            <button className="text-button" onClick={onSettings}>
              Escolher atendente
            </button>
          )}
        </div>
        <div className="detail-actions">
          {atendimento ? (
            <button className="button" disabled={ocupado} onClick={onResolve}>
              <Icon name="check" />
              Encerrar conversa
            </button>
          ) : (
            <button
              className="button"
              disabled={ocupado || !busca.detalhe || !atendente}
              onClick={() => busca.detalhe && onAssume(busca.detalhe)}
            >
              Assumir conversa
              <Icon name="chevron-right" />
            </button>
          )}
        </div>
      </footer>
    </>
  );
}
