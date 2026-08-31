import { useBackendHealth } from '../hooks/useBackendHealth';
import { API_URL } from '../services/api';
import { StatusBadge } from '../components/StatusBadge';
import './Dashboard.css';

/**
 * Página única desta etapa. Não é o dashboard de atendimento (fila de
 * conversas, indicadores etc. — ver docs/modelagem-inicial.md); serve
 * apenas para comprovar que o frontend consegue falar com o backend.
 */
export function Dashboard() {
  const { status, data } = useBackendHealth();

  return (
    <main className="dashboard">
      <h1>WXN Chatbot</h1>
      <p className="dashboard__subtitle">
        Fundação do projeto — Semana 05, Projeto 3 (CESAR School)
      </p>

      <section className="dashboard__card">
        <h2>Conexão com o backend</h2>
        <StatusBadge status={status} />
        <p className="dashboard__detail">
          <code>GET {API_URL}/api/health</code>
          {data && <> → <code>{JSON.stringify(data)}</code></>}
        </p>
      </section>

      <p className="dashboard__note">
        Esta página existe só para validar o ambiente (React → HTTP → Spring
        Boot → PostgreSQL). O dashboard real de atendimento será construído
        nas próximas semanas.
      </p>
    </main>
  );
}
