import { Icon } from "../icons";
export function Metrics() {
  return (
    <section
      className="metrics"
      aria-label="Indicadores ilustrativos de demonstração"
    >
      <article className="metric">
        <div className="metric-top">
          <span>Volume de atendimentos</span>
          <span className="metric-icon">
            <Icon name="messages" />
          </span>
        </div>
        <div className="metric-value">
          1.284
          <svg className="sparkline" viewBox="0 0 90 32" aria-hidden="true">
            <path d="M1 27 14 21 25 24 37 12 49 16 61 8 73 12 89 2" />
          </svg>
        </div>
        <div className="metric-bottom">
          <span className="trend">↗ 8,2%</span>
          <span>vs. período anterior</span>
        </div>
      </article>
      <article className="metric">
        <div className="metric-top">
          <span>Tempo médio de resposta</span>
          <span className="metric-icon">
            <Icon name="clock" />
          </span>
        </div>
        <div className="metric-value">
          01m <span>42s</span>
        </div>
        <div className="metric-bottom">
          <span className="trend">↘ 12%</span>
          <span>mais rápido neste período</span>
        </div>
      </article>
      <article className="metric">
        <div className="metric-top">
          <span>Resolução pela IA</span>
          <span className="metric-icon">
            <Icon name="sparkles" />
          </span>
        </div>
        <div className="metric-value">
          72<span>%</span>
          <span className="metric-mini-badge">Lily</span>
        </div>
        <div className="metric-bottom">
          <span className="mini-progress">
            <i />
          </span>
          <span>924 resolvidos</span>
        </div>
      </article>
      <article className="metric">
        <div className="metric-top">
          <span>Transferência para humano</span>
          <span className="metric-icon">
            <Icon name="users" />
          </span>
        </div>
        <div className="metric-value">
          18<span>%</span>
        </div>
        <div className="metric-bottom">
          <span className="neutral-trend">231</span>
          <span>conversas transferidas</span>
        </div>
      </article>
    </section>
  );
}
