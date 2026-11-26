import {
  Fragment,
  useEffect,
  useRef,
  useState,
  type KeyboardEvent,
} from "react";
import { SeloDeOrigem } from "../../components/SeloDeOrigem";
import { useDetalheFila } from "../../hooks/useDetalheFila";
import { useMensagensDoAtendimento } from "../../hooks/useMensagensDoAtendimento";
import { DEMO_ATIVO, enviarMensagemDoAtendente, mensagemDoErro } from "../../services/api";
import type {
  Atendente,
  ConversaNaFila,
  DetalheConversaFila,
  EmAtendimento,
  MensagemResumo,
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
const LIMITE_MENSAGEM = 4000;
// Distância (em px) do fim da conversa em que ainda contamos o atendente como "acompanhando".
const MARGEM_DO_FIM = 80;
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
  onMessageSent: (conversaId: string, mensagem: MensagemResumo) => void;
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
  onMessageSent,
}: Props) {
  const emAtendimento = atendimento !== undefined;
  const busca = useDetalheFila(
    conversa && !atendimento ? conversa.conversaId : null,
  );
  // Na conversa assumida o histórico se atualiza sozinho; até a primeira resposta do servidor, vale o retrato de quando foi assumida.
  const aoVivo = useMensagensDoAtendimento(
    atendimento?.detalhe.conversaId ?? null,
    emAtendimento,
    atendimento?.detalhe.mensagens,
  );
  const mensagens = atendimento ? aoVivo.mensagens : busca.detalhe?.mensagens;
  const total = mensagens?.length;
  const threadRef = useRef<HTMLDivElement>(null);
  const campoRef = useRef<HTMLTextAreaElement>(null);
  const pertoDoFim = useRef(true);
  const enviandoRef = useRef(false);
  const devolverFoco = useRef(false);
  const estavaEmAtendimento = useRef(emAtendimento);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [faixa, setFaixa] = useState<{ erro: boolean; texto: string } | null>(
    null,
  );

  const aoRolar = () => {
    const thread = threadRef.current;
    if (!thread) return;
    pertoDoFim.current =
      thread.scrollHeight - thread.scrollTop - thread.clientHeight <
      MARGEM_DO_FIM;
  };
  // Só acompanha as mensagens novas quem já estava perto do fim; quem está lendo o começo não perde o lugar.
  useEffect(() => {
    if (pertoDoFim.current)
      threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight });
  }, [total, emAtendimento]);
  useEffect(() => {
    if (busca.indisponivel) onIndisponivel();
  }, [busca.indisponivel, onIndisponivel]);
  // Ao assumir a conversa o botão "Assumir" some; o foco vai direto para o campo de resposta.
  useEffect(() => {
    if (emAtendimento && !estavaEmAtendimento.current)
      campoRef.current?.focus();
    estavaEmAtendimento.current = emAtendimento;
  }, [emAtendimento]);
  // O campo fica desabilitado durante o envio e só pode receber o foco de volta depois de habilitado.
  useEffect(() => {
    if (!enviando && devolverFoco.current) {
      devolverFoco.current = false;
      campoRef.current?.focus();
    }
  }, [enviando]);

  const enviar = async () => {
    const conversaId = atendimento?.detalhe.conversaId;
    const conteudo = texto.trim();
    if (!conversaId || !conteudo || enviandoRef.current) return;
    enviandoRef.current = true;
    setEnviando(true);
    setFaixa(null);
    try {
      const resposta = await enviarMensagemDoAtendente(conversaId, conteudo);
      pertoDoFim.current = true;
      aoVivo.adicionar(resposta.mensagem);
      onMessageSent(conversaId, resposta.mensagem);
      setTexto("");
      if (!resposta.entregue && !DEMO_ATIVO)
        setFaixa({
          erro: false,
          texto:
            resposta.aviso ||
            "A mensagem foi salva, mas não chegou ao cliente.",
        });
    } catch (erro) {
      setFaixa({ erro: true, texto: mensagemDoErro(erro) });
    } finally {
      enviandoRef.current = false;
      devolverFoco.current = true;
      setEnviando(false);
    }
  };
  const aoTeclar = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key !== "Enter" || e.shiftKey || e.nativeEvent.isComposing) return;
    e.preventDefault();
    void enviar();
  };

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
  const bloqueado = aoVivo.indisponivel;
  const metadados = [
    ["Na fila desde", dataHora(c.abertoEm)],
    atendimento
      ? ["Assumida às", atendimento.assumidaEm ? hora(atendimento.assumidaEm) : "Horário indisponível"]
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
        onScroll={aoRolar}
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
                className={`chat-message ${kind === "client" ? "outbound" : ""} ${kind === "agent" ? "agent" : ""}`}
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
      </div>
      {atendimento && bloqueado && (
        <div className="error-banner thread-status" role="alert">
          <span>Esta conversa não está mais em atendimento.</span>
        </div>
      )}
      {atendimento && aoVivo.semConexao && (
        <div className="thread-status offline" role="status">
          <span>Não foi possível atualizar as mensagens. Tentando de novo…</span>
          <button className="text-button" onClick={aoVivo.recarregar}>
            Tentar agora
          </button>
        </div>
      )}
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
        {atendimento ? (
          <form
            className="reply-form"
            onSubmit={(e) => {
              e.preventDefault();
              void enviar();
            }}
          >
            {faixa && (
              <div
                className={`composer-notice ${faixa.erro ? "error" : "warning"}`}
                role="alert"
              >
                {faixa.texto}
              </div>
            )}
            <textarea
              ref={campoRef}
              className="reply-input"
              aria-label="Mensagem para o cliente"
              aria-describedby="dica-resposta"
              placeholder="Escreva a resposta ao cliente…"
              maxLength={LIMITE_MENSAGEM}
              rows={2}
              value={texto}
              disabled={enviando || bloqueado}
              onChange={(e) => setTexto(e.target.value)}
              onKeyDown={aoTeclar}
            />
            <div className="reply-hint" id="dica-resposta">
              <span>Enter envia · Shift+Enter quebra a linha</span>
              {texto.length >= LIMITE_MENSAGEM - 500 && (
                <span>
                  {texto.length}/{LIMITE_MENSAGEM}
                </span>
              )}
            </div>
            <div className="detail-actions">
              <button
                type="button"
                className="button secondary"
                disabled={ocupado}
                onClick={onResolve}
              >
                <Icon name="check" />
                Encerrar conversa
              </button>
              <button
                type="submit"
                className="button send-button"
                disabled={enviando || bloqueado || texto.trim() === ""}
              >
                {enviando ? (
                  "Enviando…"
                ) : (
                  <>
                    Enviar
                    <Icon name="send" />
                  </>
                )}
              </button>
            </div>
          </form>
        ) : (
          <>
            <p className="reply-hint-text">
              Assuma a conversa para responder ao cliente.
            </p>
            <div className="detail-actions">
              <button
                className="button"
                disabled={ocupado || !busca.detalhe || !atendente}
                onClick={() => busca.detalhe && onAssume(busca.detalhe)}
              >
                Assumir conversa
                <Icon name="chevron-right" />
              </button>
            </div>
          </>
        )}
      </footer>
    </>
  );
}
