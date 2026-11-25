import { StatusBadge } from "../../components/StatusBadge";
import type { BackendStatus } from "../../hooks/useBackendHealth";
import type { Atendente } from "../../types/fila";
import type { Aba } from "./ConversationTable";
import { iniciais } from "../formato";
import { Icon } from "../icons";

type Props = {
  count: number | undefined;
  activeCount: number;
  activePage: Aba;
  atendente: Atendente;
  backend: BackendStatus;
  demo: boolean;
  onHome: () => void;
  onAtendimento: () => void;
  onSettings: () => void;
  onLogout: () => void;
};
export function Sidebar({
  count,
  activeCount,
  activePage,
  atendente,
  backend,
  demo,
  onHome,
  onAtendimento,
  onSettings,
  onLogout,
}: Props) {
  return (
    <aside className="sidebar" aria-label="Navegação principal">
      <a
        href="#"
        className="brand"
        aria-label="WXN Tecnologia, início"
        onClick={(e) => {
          e.preventDefault();
          onHome();
        }}
      >
        <span className="brand-lockup" aria-hidden="true">
          <img className="brand-logo" src="/wxn-logo.png" alt="" width="140" height="64" />
          <span className="brand-tagline">TECNOLOGIA</span>
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
          className={`nav-item ${activePage === "fila" ? "active" : ""}`}
          aria-current={activePage === "fila" ? "page" : undefined}
          onClick={onHome}
        >
          <Icon name="inbox" />
          <span>Fila de conversas</span>
          <b>{count ?? "—"}</b>
        </button>
        <button
          title="Simulador de WhatsApp"
          className="nav-item"
          onClick={() => {
            window.location.hash = "#/simulador";
          }}
        >
          <Icon name="messages" />
          <span>Simulador de chat</span>
        </button>
        <button
          title="Suas conversas em atendimento"
          className={`nav-item ${activePage === "atendimento" ? "active" : ""}`}
          aria-current={activePage === "atendimento" ? "page" : undefined}
          onClick={onAtendimento}
        >
          <Icon name="users" />
          <span>Em atendimento</span>
          <b>{activeCount}</b>
        </button>
        <button title="Histórico (em breve)" className="nav-item" disabled>
          <Icon name="history" />
          <span>Histórico</span>
          <b>Em breve</b>
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
        </div>
        {!demo && (
          <div className="environment">
            <StatusBadge status={backend} />
          </div>
        )}
        <button
          className="sidebar-profile"
          onClick={onSettings}
          aria-label={`Configurações de ${atendente.nome}`}
        >
          <span className="avatar profile-avatar">
            {iniciais(atendente.nome)}
          </span>
          <span>
            {atendente.nome}
            <small>
              {atendente.disponivel ? "Disponível" : "Indisponível"}
            </small>
          </span>
          <Icon name="chevron-right" />
        </button>
        <button className="sidebar-signout" onClick={onLogout}>
          <Icon name="sign-out" /> Sair do workspace
        </button>
      </div>
    </aside>
  );
}
