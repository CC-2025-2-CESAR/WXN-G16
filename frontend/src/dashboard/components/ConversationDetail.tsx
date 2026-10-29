import { useEffect, useRef, useState } from "react";
import { statuses, type Conversation } from "../data";
import { Icon } from "../icons";

type Props = {
  conversation: Conversation | undefined;
  onClose: () => void;
  onAssume: () => void;
  onResolve: () => void;
  onTransfer: () => void;
  onSend: (text: string) => void;
};
export function ConversationDetail({
  conversation: c,
  onClose,
  onAssume,
  onResolve,
  onTransfer,
  onSend,
}: Props) {
  const [draft, setDraft] = useState("");
  const threadRef = useRef<HTMLDivElement>(null);
  const lastCount = useRef(c?.messages.length || 0);
  useEffect(() => {
    if (threadRef.current && c && c.messages.length > lastCount.current)
      threadRef.current.scrollTo({
        top: threadRef.current.scrollHeight,
        behavior: "smooth",
      });
    lastCount.current = c?.messages.length || 0;
  }, [c?.messages.length]);
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
  const isMine = c.status === "active" && c.agent === "Ana Martins";
  const canSend = isMine && draft.trim().length > 0;
  const send = () => {
    if (canSend) {
      onSend(draft.trim());
      setDraft("");
    }
  };
  return (
    <>
      <header className="detail-header">
        <div>
          <div className="eyebrow">CONVERSA SELECIONADA</div>
          <h2>
            #{c.id}
            <span className="detail-channel">· {c.channel}</span>
          </h2>
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
          <span className={`avatar color-${c.color}`}>{c.initials}</span>
          <div>
            <h3 className="contact-name">{c.name}</h3>
            <div className="contact-phone">{c.phone}</div>
          </div>
          <span className={`badge ${c.status}`}>{statuses[c.status]}</span>
        </div>
        <div className="contact-metadata">
          <div>
            <div className="metadata-label">Empresa</div>
            <div className="metadata-value">{c.company}</div>
          </div>
          <div>
            <div className="metadata-label">Canal</div>
            <div className="metadata-value">
              <Icon name={c.channel === "WhatsApp" ? "whatsapp" : "monitor"} />
              {c.channel}
            </div>
          </div>
          <div>
            <div className="metadata-label">Assunto</div>
            <div className="metadata-value">{c.subject}</div>
          </div>
          <div>
            <div className="metadata-label">Prioridade</div>
            <div className="metadata-value">
              <i
                className={`priority-dot ${c.priority === "Alta" ? "high" : c.priority === "Baixa" ? "low" : ""}`}
              />
              {c.priority}
            </div>
          </div>
        </div>
      </section>
      <section
        className="ai-summary"
        aria-label="Resumo ilustrativo da conversa"
      >
        <div className="summary-title">
          <Icon name="sparkles" />
          RESUMO DA LILY<span>DEMO</span>
        </div>
        <p>{c.summary}</p>
      </section>
      <div
        className="chat-thread"
        ref={threadRef}
        role="log"
        aria-label={`Histórico de ${c.name}`}
      >
        <div className="day-separator">
          {c.daysAgo === 0 ? "Hoje" : `Há ${c.daysAgo} dias`} · 09:38
        </div>
        {c.messages.map((m) =>
          m.kind === "system" ? (
            <div className="system-message" key={m.id}>
              <Icon name="check" />
              {m.text}
            </div>
          ) : (
            <div
              className={`chat-message ${m.kind === "client" ? "outbound" : ""}`}
              key={m.id}
            >
              <div className="bubble">
                {m.kind === "bot" && (
                  <div className="bubble-sender">Lily · Assistente virtual</div>
                )}
                {m.kind === "agent" && (
                  <div className="bubble-sender">Ana Martins · Atendente</div>
                )}
                {m.text}
              </div>
              <div className="message-meta">
                {m.time}
                {m.kind === "client" ? " · Cliente" : " · ✓✓"}
              </div>
            </div>
          ),
        )}
      </div>
      <footer className="composer">
        <div className="composer-top">
          <strong>
            {c.status === "resolved"
              ? "Conversa finalizada"
              : isMine
                ? "Você está no atendimento"
                : c.status === "active"
                  ? `${c.agent} está no atendimento`
                  : c.status === "ai"
                    ? "Lily está no atendimento"
                    : "Aguardando um atendente"}
          </strong>
          <span>Simulação local</span>
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            send();
          }}
        >
          <div className="compose-input">
            <textarea
              aria-label="Sua resposta"
              placeholder={
                c.status === "resolved"
                  ? "Este atendimento foi encerrado."
                  : isMine
                    ? "Digite sua resposta..."
                    : "Assuma a conversa para responder..."
              }
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              disabled={!isMine}
              maxLength={2000}
              onKeyDown={(e) => {
                if (
                  e.key === "Enter" &&
                  !e.shiftKey &&
                  !e.nativeEvent.isComposing
                ) {
                  e.preventDefault();
                  send();
                }
              }}
            />
            <div className="compose-tools">
              <span className="compose-hint">
                Enter para enviar · Shift + Enter para nova linha
              </span>
              <button
                type="submit"
                className="send-button"
                disabled={!canSend}
                aria-label="Enviar resposta"
              >
                <Icon name="send" />
              </button>
            </div>
          </div>
        </form>
        <div className="detail-actions">
          <button
            className="button secondary"
            disabled={c.status === "resolved"}
            onClick={onTransfer}
          >
            <Icon name="transfer" />
            Transferir
          </button>
          {c.status === "resolved" ? (
            <button className="button" disabled>
              <Icon name="check" />
              Finalizada
            </button>
          ) : isMine ? (
            <button className="button" onClick={onResolve}>
              <Icon name="check" />
              Encerrar conversa
            </button>
          ) : (
            <button className="button" onClick={onAssume}>
              Assumir conversa
              <Icon name="chevron-right" />
            </button>
          )}
        </div>
      </footer>
    </>
  );
}
