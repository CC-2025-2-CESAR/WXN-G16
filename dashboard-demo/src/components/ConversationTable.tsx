import { useEffect, useMemo, useRef, useState } from "react";
import { Icon } from "../icons";
import { statuses, type Conversation, type Status } from "../data";
import type { View } from "./Sidebar";

type Props = {
  conversations: Conversation[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  view: View;
  onRefresh: () => void;
};
const PAGE_SIZE = 6;
const normalize = (value: string) =>
  value.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase();
export function ConversationTable({
  conversations,
  selectedId,
  onSelect,
  view,
  onRefresh,
}: Props) {
  const [search, setSearch] = useState("");
  const [company, setCompany] = useState("");
  const [agent, setAgent] = useState("");
  const [period, setPeriod] = useState("today");
  const [channel, setChannel] = useState("");
  const [priority, setPriority] = useState("");
  const [status, setStatus] = useState<Status | "all">("all");
  const [extra, setExtra] = useState(false);
  const [page, setPage] = useState(1);
  const searchRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const shortcut = (e: KeyboardEvent) => {
      if (
        e.key === "/" &&
        !(
          e.target instanceof HTMLInputElement ||
          e.target instanceof HTMLTextAreaElement ||
          e.target instanceof HTMLSelectElement
        )
      ) {
        e.preventDefault();
        searchRef.current?.focus();
      }
    };
    window.addEventListener("keydown", shortcut);
    return () => window.removeEventListener("keydown", shortcut);
  }, []);
  useEffect(() => {
    setPage(1);
  }, [search, company, agent, period, channel, priority, status, view]);

  const filteredBase = useMemo(
    () =>
      conversations.filter((c) => {
        if (view === "history" && c.status !== "resolved") return false;
        if (
          (period === "today" && c.daysAgo !== 0) ||
          (period === "week" && c.daysAgo > 6)
        )
          return false;
        if (
          (company && c.company !== company) ||
          (channel && c.channel !== channel) ||
          (priority && c.priority !== priority)
        )
          return false;
        if (agent === "none" ? Boolean(c.agent) : agent && c.agent !== agent)
          return false;
        return normalize(
          `${c.name} ${c.company} ${c.id} ${c.preview}`,
        ).includes(normalize(search));
      }),
    [conversations, search, company, agent, period, channel, priority, view],
  );
  const filtered = filteredBase.filter(
    (c) => status === "all" || c.status === status,
  );
  const maxPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, maxPages);
  const visible = filtered.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );
  const clear = () => {
    setSearch("");
    setCompany("");
    setAgent("");
    setPeriod("today");
    setChannel("");
    setPriority("");
    setStatus("all");
    setPage(1);
  };
  const tabs: Array<[Status | "all", string]> =
    view === "history"
      ? [["all", "Finalizadas"]]
      : [
          ["all", "Todas"],
          ["waiting", "Aguardando"],
          ["active", "Em atendimento"],
          ["ai", "Com a Lily"],
        ];

  return (
    <section
      className="queue-panel"
      aria-label={
        view === "history" ? "Histórico de atendimento" : "Fila de atendimento"
      }
    >
      <div className="queue-toolbar">
        <div className="queue-title">
          <h2>
            {view === "history"
              ? "Conversas finalizadas"
              : "Todas as conversas"}{" "}
            <span>{filtered.length}</span>
          </h2>
          <p>
            {view === "history"
              ? "Consulte o histórico dos atendimentos concluídos."
              : "Acompanhe e gerencie seus atendimentos."}
          </p>
        </div>
        <button className="button secondary small" onClick={onRefresh}>
          <Icon name="refresh" />
          Atualizar fila
        </button>
      </div>
      <div className="filters">
        <label className="search-field">
          <Icon name="search" />
          <input
            ref={searchRef}
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar nome, mensagem ou protocolo..."
            aria-label="Buscar conversas"
          />
          <kbd>/</kbd>
        </label>
        <div className="select-filters">
          <label>
            <span className="sr-only">Empresa</span>
            <select
              value={company}
              onChange={(e) => setCompany(e.target.value)}
            >
              <option value="">Todas as empresas</option>
              {[
                "Alfa Comércio",
                "Grupo Norte",
                "Vértice Ltda.",
                "Beta Serviços",
              ].map((v) => (
                <option key={v}>{v}</option>
              ))}
            </select>
          </label>
          <label>
            <span className="sr-only">Atendente</span>
            <select value={agent} onChange={(e) => setAgent(e.target.value)}>
              <option value="">Atendente</option>
              <option value="none">Não atribuído</option>
              {["Ana Martins", "Paulo M.", "Lily"].map((v) => (
                <option key={v}>{v}</option>
              ))}
            </select>
          </label>
          <label>
            <span className="sr-only">Período</span>
            <select value={period} onChange={(e) => setPeriod(e.target.value)}>
              <option value="today">Hoje</option>
              <option value="week">Últimos 7 dias</option>
              <option value="all">Todo o período</option>
            </select>
          </label>
          <button
            className={`button secondary icon-only ${extra ? "filter-active" : ""}`}
            onClick={() => setExtra(!extra)}
            aria-label="Mais filtros"
            aria-expanded={extra}
          >
            <Icon name="sliders" />
          </button>
        </div>
      </div>
      {extra && (
        <div className="extra-filters">
          <label>
            Canal
            <select
              value={channel}
              onChange={(e) => setChannel(e.target.value)}
            >
              <option value="">Todos os canais</option>
              <option>WhatsApp</option>
              <option>Webchat</option>
            </select>
          </label>
          <label>
            Prioridade
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
            >
              <option value="">Todas</option>
              <option>Alta</option>
              <option>Média</option>
              <option>Baixa</option>
            </select>
          </label>
          <button onClick={clear} className="text-button">
            Limpar filtros
          </button>
        </div>
      )}
      <div className="status-tabs" aria-label="Status da conversa">
        {tabs.map(([value, label]) => (
          <button
            key={value}
            aria-pressed={status === value}
            className={`status-tab ${status === value ? "active" : ""}`}
            onClick={() => setStatus(value)}
          >
            {label}
            <span>
              {
                filteredBase.filter(
                  (c) => value === "all" || c.status === value,
                ).length
              }
            </span>
          </button>
        ))}
      </div>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>CLIENTE</th>
              <th>STATUS</th>
              <th>ÚLTIMA MENSAGEM</th>
              <th>ATENDENTE</th>
              <th>
                <span className="sr-only">Abrir conversa</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {visible.map((c) => (
              <tr
                key={c.id}
                className={selectedId === c.id ? "selected" : ""}
                onClick={() => onSelect(c.id)}
              >
                <td>
                  <div className="client-cell">
                    <span className={`avatar color-${c.color}`}>
                      {c.initials}
                    </span>
                    <div className="client-info">
                      <strong>{c.name}</strong>
                      <small>{c.company}</small>
                    </div>
                  </div>
                </td>
                <td>
                  <span className={`badge ${c.status}`}>
                    {statuses[c.status]}
                  </span>
                </td>
                <td>
                  <span className="message-preview" title={c.preview}>
                    {c.preview}
                  </span>
                  <span className="message-time">
                    {c.daysAgo ? `há ${c.daysAgo} dias` : c.ago}{" "}
                    <span className="table-channel">· {c.channel}</span>
                  </span>
                </td>
                <td className="agent-cell">
                  {c.agent && <span className="agent-dot" />}
                  {c.agent || "Não atribuído"}
                </td>
                <td>
                  <button
                    className="row-open"
                    aria-label={`Abrir conversa de ${c.name}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelect(c.id);
                    }}
                  >
                    <Icon name="chevron-right" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {filtered.length === 0 && (
        <div className="empty-state">
          <Icon name="search" />
          <h3>Nenhuma conversa por aqui</h3>
          <p>Tente outro nome ou ajuste os filtros.</p>
          <button className="button secondary" onClick={clear}>
            Limpar filtros
          </button>
        </div>
      )}
      <footer className="table-footer">
        <span>
          {filtered.length
            ? `Mostrando ${(currentPage - 1) * PAGE_SIZE + 1}–${Math.min(currentPage * PAGE_SIZE, filtered.length)} de ${filtered.length} conversas`
            : "Nenhuma conversa encontrada"}
        </span>
        <div className="pagination">
          <button
            className="icon-button"
            disabled={currentPage === 1}
            onClick={() => setPage(currentPage - 1)}
            aria-label="Página anterior"
          >
            <Icon name="chevron-left" />
          </button>
          <span id="page-number">{currentPage}</span>
          <button
            className="icon-button"
            disabled={currentPage >= maxPages}
            onClick={() => setPage(currentPage + 1)}
            aria-label="Próxima página"
          >
            <Icon name="chevron-right" />
          </button>
        </div>
      </footer>
    </section>
  );
}
