import { useEffect, useRef, useState } from "react";
import { Icon } from "./icons";
import {
  createInitialData,
  loadConversations,
  STORAGE_KEY,
  type Conversation,
  type Message,
} from "./data";
import { Sidebar, type View } from "./components/Sidebar";
import { Metrics } from "./components/Metrics";
import { ConversationTable } from "./components/ConversationTable";
import { ConversationDetail } from "./components/ConversationDetail";
import { Modal } from "./components/Modal";

type ModalType =
  | "settings"
  | "notifications"
  | "transfer"
  | "resolve"
  | "reset"
  | null;
const now = () =>
  new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "America/Sao_Paulo",
  }).format(new Date());
function systemMessage(text: string): Message {
  return { id: crypto.randomUUID(), kind: "system", text, time: now() };
}

export default function App() {
  const [conversations, setConversations] = useState(loadConversations);
  const [selectedId, setSelectedId] = useState<number | null>(() =>
    window.matchMedia("(max-width: 860px)").matches ? null : 4521,
  );
  const [view, setView] = useState<View>("queue");
  const [modal, setModal] = useState<ModalType>(null);
  const [targetAgent, setTargetAgent] = useState("Paulo M.");
  const [toast, setToast] = useState("");
  const [tableKey, setTableKey] = useState(0);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | undefined>(
    undefined,
  );
  const selected = conversations.find((c) => c.id === selectedId);
  const waiting = conversations.filter((c) => c.status === "waiting").length;
  const mine = conversations.filter(
    (c) => c.status === "active" && c.agent === "Ana Martins",
  ).length;

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations));
    } catch {
      /* O armazenamento é opcional. */
    }
  }, [conversations]);
  useEffect(() => () => clearTimeout(toastTimer.current), []);
  const notify = (text: string) => {
    clearTimeout(toastTimer.current);
    setToast(text);
    toastTimer.current = setTimeout(() => setToast(""), 4000);
  };
  const updateSelected = (fn: (c: Conversation) => Conversation) => {
    setConversations((all) =>
      all.map((c) => (c.id === selectedId ? fn(c) : c)),
    );
  };
  const assume = () => {
    if (!selected || selected.status === "resolved") return;
    updateSelected((c) => ({
      ...c,
      status: "active",
      agent: "Ana Martins",
      messages: [
        ...c.messages,
        systemMessage("Ana Martins assumiu a conversa"),
      ],
    }));
    notify(`Você assumiu a conversa #${selected.id}.`);
  };
  const send = (text: string) => {
    if (
      !selected ||
      selected.status !== "active" ||
      selected.agent !== "Ana Martins" ||
      !text.trim()
    )
      return;
    const message: Message = {
      id: crypto.randomUUID(),
      text: text.trim().slice(0, 2000),
      kind: "agent",
      time: now(),
    };
    updateSelected((c) => ({
      ...c,
      preview: message.text,
      ago: "agora",
      messages: [...c.messages, message],
    }));
    notify("Resposta adicionada à demonstração.");
  };
  const resolveConversation = () => {
    updateSelected((c) => ({
      ...c,
      status: "resolved",
      messages: [
        ...c.messages,
        systemMessage("Atendimento encerrado por Ana Martins"),
      ],
    }));
    setModal(null);
    notify("Conversa encerrada. Ela está disponível no histórico.");
  };
  const transfer = () => {
    const toQueue = targetAgent === "queue";
    updateSelected((c) => ({
      ...c,
      status: toQueue ? "waiting" : targetAgent === "Lily" ? "ai" : "active",
      agent: toQueue ? "" : targetAgent,
      messages: [
        ...c.messages,
        systemMessage(
          toQueue
            ? "Conversa devolvida à fila de atendimento"
            : `Conversa transferida para ${targetAgent}`,
        ),
      ],
    }));
    setModal(null);
    notify(
      toQueue
        ? "Conversa devolvida à fila."
        : `Conversa transferida para ${targetAgent}.`,
    );
  };
  const navigate = (next: View) => {
    setView(next);
    setTableKey((k) => k + 1);
    if (window.matchMedia("(max-width: 860px)").matches) setSelectedId(null);
    else if (next === "history")
      setSelectedId(
        conversations.find((c) => c.status === "resolved" && c.daysAgo === 0)
          ?.id ?? null,
      );
    else setSelectedId(4521);
  };
  const reset = () => {
    setConversations(createInitialData());
    setSelectedId(4521);
    setView("queue");
    setTableKey((k) => k + 1);
    setModal(null);
    notify("Demonstração restaurada para a apresentação.");
  };
  const dateLabel = new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    timeZone: "America/Sao_Paulo",
  }).format(new Date());

  return (
    <>
      <div className="app-shell">
        <Sidebar
          view={view}
          count={conversations.filter((c) => c.daysAgo === 0).length}
          onNavigate={navigate}
          onSettings={() => setModal("settings")}
        />
        <main className="main-content">
          <header className="page-header">
            <div>
              <div className="breadcrumb">
                Atendimento <span>/</span>{" "}
                <b>{view === "queue" ? "Visão geral" : "Histórico"}</b>
              </div>
              <h1>
                {view === "queue"
                  ? "Fila de conversas"
                  : "Histórico de conversas"}
                <span className="heading-dot" />
              </h1>
              <p>
                {view === "queue"
                  ? "Cada conversa, um atendimento mais próximo."
                  : "Bons atendimentos ficam na memória."}
              </p>
            </div>
            <div className="header-actions">
              <span className="today">{dateLabel}</span>
              <button
                className="icon-button notifications"
                onClick={() => setModal("notifications")}
                aria-label="Ver notificações"
              >
                <Icon name="bell" />
                {waiting > 0 && <i />}
              </button>
              <button
                className="profile-button"
                onClick={() => setModal("settings")}
                aria-label="Perfil de Ana Martins"
              >
                <span className="avatar profile-avatar header-avatar">AM</span>
              </button>
            </div>
          </header>
          <Metrics />
          <ConversationTable
            key={tableKey}
            conversations={conversations}
            selectedId={selectedId}
            onSelect={setSelectedId}
            view={view}
            onRefresh={() =>
              notify("Fila atualizada com os dados locais da demonstração.")
            }
          />
          <footer className="main-footer">
            <span>
              <i />
              Tudo pronto para um bom atendimento
            </span>
            <span>
              WXN Tecnologia <b>·</b> Indicadores ilustrativos
            </span>
          </footer>
        </main>
        <aside
          className={`conversation-panel ${selectedId === null ? "panel-closed" : ""}`}
          aria-label="Detalhes da conversa"
        >
          <ConversationDetail
            key={selectedId}
            conversation={selected}
            onClose={() => setSelectedId(null)}
            onAssume={assume}
            onResolve={() => setModal("resolve")}
            onTransfer={() => {
              setTargetAgent("Paulo M.");
              setModal("transfer");
            }}
            onSend={send}
          />
        </aside>
      </div>
      {toast && (
        <div className="toast" role="status" aria-live="polite">
          {toast}
        </div>
      )}
      {modal === "settings" && (
        <Modal
          title="Configurações da demonstração"
          onClose={() => setModal(null)}
        >
          <div className="settings-profile">
            <span className="avatar profile-avatar">AM</span>
            <div>
              <strong>Ana Martins</strong>
              <small>Supervisora · Workspace WXN</small>
            </div>
          </div>
          <p>
            As interações são simuladas neste navegador. O backend, a IA e o
            WhatsApp não estão conectados nesta versão.
          </p>
          <div className="setting-row">
            <span>
              Memória local
              <small>Alterações salvas automaticamente neste navegador.</small>
            </span>
            <span className="badge resolved">Ativa</span>
          </div>
          <div className="setting-row">
            <span>
              Indicadores
              <small>Valores ilustrativos para apresentar o dashboard.</small>
            </span>
            <Icon name="messages" />
          </div>
          <div className="modal-actions">
            <button
              className="button secondary"
              onClick={() => setModal("reset")}
            >
              Restaurar demonstração
            </button>
            <button className="button" onClick={() => setModal(null)}>
              Concluir
            </button>
          </div>
        </Modal>
      )}
      {modal === "notifications" && (
        <Modal title="Central de notificações" onClose={() => setModal(null)}>
          <div className="notification-item">
            <strong>{waiting} conversas aguardando atendimento</strong>
            <small>A fila precisa de atenção da equipe.</small>
          </div>
          <div className="notification-item">
            <strong>{mine} conversas atribuídas a você</strong>
            <small>Continue o atendimento como Ana Martins.</small>
          </div>
          <div className="notification-item">
            <strong>Lily cuida das primeiras respostas</strong>
            <small>
              {conversations.filter((c) => c.status === "ai").length} conversas
              com a assistente virtual na simulação.
            </small>
          </div>
          <div className="modal-actions">
            <button
              className="button"
              onClick={() => {
                setModal(null);
                navigate("queue");
              }}
            >
              Ver fila de conversas
            </button>
          </div>
        </Modal>
      )}
      {modal === "transfer" && selected && (
        <Modal
          title={`Transferir conversa #${selected.id}`}
          onClose={() => setModal(null)}
        >
          <p>
            Escolha quem vai dar continuidade ao atendimento de {selected.name}.
            O histórico será mantido na demonstração.
          </p>
          <label>
            Transferir para
            <select
              value={targetAgent}
              onChange={(e) => setTargetAgent(e.target.value)}
            >
              <option>Paulo M.</option>
              <option>Ana Martins</option>
              <option value="Lily">Lily · Assistente virtual</option>
              <option value="queue">Fila de atendimento</option>
            </select>
          </label>
          <div className="modal-actions">
            <button className="button secondary" onClick={() => setModal(null)}>
              Cancelar
            </button>
            <button className="button" onClick={transfer}>
              Confirmar transferência
            </button>
          </div>
        </Modal>
      )}
      {modal === "resolve" && (
        <Modal title="Encerrar atendimento?" onClose={() => setModal(null)}>
          <p>
            A conversa será marcada como resolvida e continuará disponível no
            histórico.
          </p>
          <div className="modal-actions">
            <button className="button secondary" onClick={() => setModal(null)}>
              Continuar atendendo
            </button>
            <button className="button" onClick={resolveConversation}>
              Encerrar atendimento
            </button>
          </div>
        </Modal>
      )}
      {modal === "reset" && (
        <Modal title="Restaurar a demonstração?" onClose={() => setModal(null)}>
          <p>
            As respostas e mudanças de status desta simulação serão removidas.
            Os clientes e as conversas voltarão ao estado inicial.
          </p>
          <div className="modal-actions">
            <button
              className="button secondary"
              onClick={() => setModal("settings")}
            >
              Cancelar
            </button>
            <button className="button" onClick={reset}>
              Restaurar agora
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}
