import type { BackendStatus } from '../hooks/useBackendHealth';
import './StatusBadge.css';

const LABELS: Record<BackendStatus, string> = {
  loading: 'Verificando conexão…',
  online: 'Backend conectado',
  offline: 'Backend indisponível',
};

export function StatusBadge({ status }: { status: BackendStatus }) {
  return (
    <span className={`status-badge status-badge--${status}`}>
      <span className="status-badge__dot" aria-hidden="true" />
      {LABELS[status]}
    </span>
  );
}
