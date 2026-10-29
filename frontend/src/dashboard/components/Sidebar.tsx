import { Icon } from "../icons";
export type View = "queue" | "history";
type Props = {
  view: View;
  count: number;
  onNavigate: (view: View) => void;
  onSettings: () => void;
};
export function Sidebar({ view, count, onNavigate, onSettings }: Props) {
  return (
    <aside className="sidebar" aria-label="Navegação principal">
      <a
        href="#"
        className="brand"
        aria-label="WXN Tecnologia, início"
        onClick={(e) => {
          e.preventDefault();
          onNavigate("queue");
        }}
      >
        <span className="brand-mark">
          w<span>↗</span>
        </span>
        <span className="brand-name">
          WXN<small>TECNOLOGIA</small>
        </span>
      </a>
      <div className="workspace">
        <span className="workspace-icon">W</span>
        <span>
          Workspace WXN<small>Central de atendimento</small>
        </span>
        <span className="workspace-chevron">⌄</span>
      </div>
      <div className="nav-label">PRINCIPAL</div>
      <nav>
        <button
          title="Fila de conversas"
          className={`nav-item ${view === "queue" ? "active" : ""}`}
          onClick={() => onNavigate("queue")}
        >
          <Icon name="inbox" />
          <span>Fila de conversas</span>
          <b>{count}</b>
        </button>
        <button
          title="Histórico"
          className={`nav-item ${view === "history" ? "active" : ""}`}
          onClick={() => onNavigate("history")}
        >
          <Icon name="history" />
          <span>Histórico</span>
        </button>
        <button title="Configurações" className="nav-item" onClick={onSettings}>
          <Icon name="settings" />
          <span>Configurações</span>
        </button>
      </nav>
      <div className="sidebar-bottom">
        <div className="assistant-card">
          <span className="assistant-spark">
            <Icon name="sparkles" />
          </span>
          <strong>Lily está por aqui</strong>
          <p>Sua assistente virtual cuida das primeiras respostas.</p>
          <span className="online-label">
            <i />
            Assistente ativa na demo
          </span>
        </div>
        <div className="environment">
          <i />
          <span>
            Ambiente de demonstração<small>Dados fictícios · Grupo 16</small>
          </span>
        </div>
        <button
          className="sidebar-profile"
          onClick={onSettings}
          aria-label="Configurações de Ana Martins"
        >
          <span className="avatar profile-avatar">AM</span>
          <span>
            Ana Martins<small>Supervisora</small>
          </span>
          <Icon name="chevron-right" />
        </button>
      </div>
    </aside>
  );
}
