import { StatusBadge } from "../../components/StatusBadge";
import type { BackendStatus } from "../../hooks/useBackendHealth";
import type { Atendente } from "../../types/fila";
import { iniciais } from "../formato";
import { Icon } from "../icons";

type Props = {
  count: number | undefined;
  atendente: Atendente | null;
  backend: BackendStatus;
  onHome: () => void;
  onSettings: () => void;
};
export function Sidebar({
  count,
  atendente,
  backend,
  onHome,
  onSettings,
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
          className="nav-item active"
          aria-current="page"
          onClick={onHome}
        >
          <Icon name="inbox" />
          <span>Fila de conversas</span>
          <b>{count ?? "—"}</b>
        </button>
        <button
          title="Simulador de WhatsApp (teste da Lily)"
          className="nav-item"
          onClick={() => {
            window.location.hash = "#/simulador";
          }}
        >
          <Icon name="messages" />
          <span>Simulador de chat</span>
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
        <div className="environment">
          <StatusBadge status={backend} />
        </div>
        <button
          className="sidebar-profile"
          onClick={onSettings}
          aria-label={
            atendente
              ? `Configurações de ${atendente.nome}`
              : "Escolher o atendente"
          }
        >
          <span className="avatar profile-avatar">
            {atendente ? iniciais(atendente.nome) : "?"}
          </span>
          <span>
            {atendente?.nome ?? "Atendente"}
            <small>
              {atendente
                ? atendente.disponivel
                  ? "Disponível"
                  : "Indisponível"
                : "Não escolhido"}
            </small>
          </span>
          <Icon name="chevron-right" />
        </button>
      </div>
    </aside>
  );
}
