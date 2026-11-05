import { useEffect, useRef, useState } from "react";
import type { ConversaNaFila, EmAtendimento } from "../../types/fila";
import { esperaDesde, formatarTelefone, hora, iniciais } from "../formato";
import { Icon, type IconName } from "../icons";

export type Aba = "fila" | "atendimento";
type Props = {
  fila: ConversaNaFila[] | null;
  atendimento: EmAtendimento[];
  aba: Aba;
  onAba: (aba: Aba) => void;
  selectedId: string | null;
  onSelect: (id: string) => void;
  erro: string | null;
  carregando: boolean;
  agora: number;
  onRefresh: () => void;
};
type Linha = {
  id: string;
  nome: string;
  telefone: string;
  motivo: string;
  status: "waiting" | "active";
  rotulo: string;
  tempo: string;
  atendente: string | null;
};
const PAGE_SIZE = 6;
const normalize = (value: string) =>
  value.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase();
function estadoVazio(
  aba: Aba,
  fila: ConversaNaFila[] | null,
  erro: string | null,
  total: number,
  search: string,
): { icon: IconName; title: string; text: string } | null {
  if (aba === "fila" && fila === null)
    return erro
      ? null
      : {
          icon: "refresh",
          title: "Carregando a fila…",
          text: "Buscando as conversas no servidor.",
        };
  if (total) return null;
  if (search)
    return {
      icon: "search",
      title: "Nenhuma conversa encontrada",
      text: "Tente outro nome, telefone ou motivo.",
    };
  if (aba === "fila")
    return {
      icon: "inbox",
      title: "Nenhuma conversa aguardando atendimento",
      text: "Quando um cliente pedir atendimento humano, ele aparece aqui.",
    };
  return {
    icon: "users",
    title: "Nenhuma conversa em atendimento",
    text: "As conversas que você assumir aparecem aqui até a página ser recarregada.",
  };
}
export function ConversationTable({
  fila,
  atendimento,
  aba,
  onAba,
  selectedId,
  onSelect,
  erro,
  carregando,
  agora,
  onRefresh,
}: Props) {
  const [search, setSearch] = useState("");
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

  const bate = (c: {
    clienteNome: string;
    clienteTelefone: string;
    motivo: string;
  }) =>
    normalize(
      `${c.clienteNome} ${c.clienteTelefone} ${formatarTelefone(c.clienteTelefone)} ${c.motivo}`,
    ).includes(normalize(search));
  const aguardando = (fila ?? []).filter(bate);
  const ativas = atendimento.filter((a) => bate(a.detalhe));
  const linhas: Linha[] =
    aba === "fila"
      ? aguardando.map((c) => ({
          id: c.conversaId,
          nome: c.clienteNome,
          telefone: c.clienteTelefone,
          motivo: c.motivo,
          status: "waiting",
          rotulo: `${c.posicao}º na fila`,
          tempo: `Aguardando há ${esperaDesde(c.abertoEm, agora)}`,
          atendente: null,
        }))
      : ativas.map((a) => ({
          id: a.detalhe.conversaId,
          nome: a.detalhe.clienteNome,
          telefone: a.detalhe.clienteTelefone,
          motivo: a.detalhe.motivo,
          status: "active",
          rotulo: "Em atendimento",
          tempo: `Assumida às ${hora(a.assumidaEm)}`,
          atendente: a.atendente.nome,
        }));
  const maxPages = Math.max(1, Math.ceil(linhas.length / PAGE_SIZE));
  const currentPage = Math.min(page, maxPages);
  const visible = linhas.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );
  const tabs: Array<[Aba, string, number]> = [
    ["fila", "Aguardando", aguardando.length],
    ["atendimento", "Em atendimento (nesta sessão)", ativas.length],
  ];
  const vazio = estadoVazio(aba, fila, erro, linhas.length, search);

  return (
    <section className="queue-panel" aria-label="Fila de atendimento">
      <div className="queue-toolbar">
        <div className="queue-title">
          <h2>
            {aba === "fila"
              ? "Conversas aguardando"
              : "Em atendimento nesta sessão"}{" "}
            <span>{linhas.length}</span>
          </h2>
          <p>
            {aba === "fila"
              ? "Quem pediu atendimento primeiro aparece primeiro."
              : "Conversas que você assumiu desde que abriu esta página."}
          </p>
        </div>
        <button
          className="button secondary small"
          onClick={onRefresh}
          disabled={carregando}
        >
          <Icon name="refresh" />
          Atualizar fila
        </button>
      </div>
      {erro && (
        <div className="error-banner" role="alert">
          <span>
            {erro}
            {fila && " Exibindo a última atualização."}
          </span>
          <button
            className="text-button"
            onClick={onRefresh}
            disabled={carregando}
          >
            Tentar novamente
          </button>
        </div>
      )}
      <div className="filters">
        <label className="search-field">
          <Icon name="search" />
          <input
            ref={searchRef}
            type="search"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(1);
            }}
            placeholder="Buscar nome, telefone ou motivo..."
            aria-label="Buscar conversas"
          />
          <kbd>/</kbd>
        </label>
      </div>
      <div className="status-tabs" aria-label="Lista de conversas">
        {tabs.map(([value, label, total]) => (
          <button
            key={value}
            aria-pressed={aba === value}
            className={`status-tab ${aba === value ? "active" : ""}`}
            onClick={() => {
              onAba(value);
              setPage(1);
            }}
          >
            {label}
            <span>{total}</span>
          </button>
        ))}
      </div>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>CLIENTE</th>
              <th>STATUS</th>
              <th>MOTIVO</th>
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
                    <span className="avatar">{iniciais(c.nome)}</span>
                    <div className="client-info">
                      <strong>{c.nome}</strong>
                      <small>{formatarTelefone(c.telefone)}</small>
                    </div>
                  </div>
                </td>
                <td>
                  <span className={`badge ${c.status}`}>{c.rotulo}</span>
                </td>
                <td>
                  <span className="message-preview" title={c.motivo}>
                    {c.motivo}
                  </span>
                  <span className="message-time">{c.tempo}</span>
                </td>
                <td className="agent-cell">
                  {c.atendente && <span className="agent-dot" />}
                  {c.atendente ?? "Não atribuído"}
                </td>
                <td>
                  <button
                    className="row-open"
                    aria-label={`Abrir conversa de ${c.nome}`}
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
      {vazio && (
        <div className="empty-state">
          <Icon name={vazio.icon} />
          <h3>{vazio.title}</h3>
          <p>{vazio.text}</p>
          {search && (
            <button className="button secondary" onClick={() => setSearch("")}>
              Limpar busca
            </button>
          )}
        </div>
      )}
      <footer className="table-footer">
        <span>
          {linhas.length
            ? `Mostrando ${(currentPage - 1) * PAGE_SIZE + 1}–${Math.min(currentPage * PAGE_SIZE, linhas.length)} de ${linhas.length} conversas`
            : "Nenhuma conversa"}
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
