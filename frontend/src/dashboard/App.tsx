import { useCallback, useEffect, useRef, useState } from "react";
import { StatusBadge } from "../components/StatusBadge";
import { useAtendentes } from "../hooks/useAtendentes";
import { useBackendHealth } from "../hooks/useBackendHealth";
import { useFila } from "../hooks/useFila";
import {
  API_URL,
  DEMO_ATIVO,
  ErroApi,
  assumirConversa,
  encerrarConversa,
  mensagemDoErro,
} from "../services/api";
import type { DetalheConversaFila, EmAtendimento } from "../types/fila";
import { Icon } from "./icons";
import { esperaDesde, horaExata, iniciais, plural } from "./formato";
import { Sidebar } from "./components/Sidebar";
import { Metrics } from "./components/Metrics";
import { ConversationTable, type Aba } from "./components/ConversationTable";
import { ConversationDetail } from "./components/ConversationDetail";
import { Modal } from "./components/Modal";

type ModalType = "settings" | "notifications" | "resolve" | null;

export default function App() {
  const fila = useFila();
  const atendentes = useAtendentes();
  const backend = useBackendHealth();
  const [aba, setAba] = useState<Aba>("fila");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [atendimento, setAtendimento] = useState<EmAtendimento[]>([]);
  const [modal, setModal] = useState<ModalType>(null);
  const [ocupado, setOcupado] = useState(false);
  const [toast, setToast] = useState<{ text: string; erro: boolean } | null>(
    null,
  );
  const toastTimer = useRef<ReturnType<typeof setTimeout> | undefined>(
    undefined,
  );
  const painel = useRef<HTMLElement>(null);
  const abridor = useRef<HTMLElement | null>(null);
  const atendente = atendentes.atendente;
  const recarregarFila = fila.recarregar;
  const aguardando = fila.itens?.length;
  const emAtendimento = atendimento.find(
    (a) => a.detalhe.conversaId === selectedId,
  );
  const naFila = fila.itens?.find((c) => c.conversaId === selectedId);
  const aberta = emAtendimento ?? naFila;
  const maisAntiga = fila.itens?.[0];

  useEffect(() => () => clearTimeout(toastTimer.current), []);
  const notify = useCallback((text: string, erro = false) => {
    clearTimeout(toastTimer.current);
    setToast({ text, erro });
    toastTimer.current = setTimeout(() => setToast(null), erro ? 8000 : 5000);
  }, []);
  const abrir = (id: string) => {
    const ativo = document.activeElement;
    abridor.current =
      ativo instanceof HTMLElement && ativo !== document.body ? ativo : null;
    if (id === selectedId) painel.current?.focus();
    else setSelectedId(id);
  };
  const fecharPainel = useCallback(() => {
    setSelectedId(null);
    if (abridor.current?.isConnected) abridor.current.focus();
  }, []);
  const conversaSaiuDaFila = useCallback(() => {
    notify("Esta conversa não está mais na fila.");
    recarregarFila();
  }, [notify, recarregarFila]);

  if (selectedId && fila.itens && !aberta) setSelectedId(null);
  useEffect(() => {
    if (selectedId) painel.current?.focus();
  }, [selectedId]);
  useEffect(() => {
    if (!selectedId || modal) return;
    const aoTeclar = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !(e.target instanceof HTMLInputElement))
        fecharPainel();
    };
    window.addEventListener("keydown", aoTeclar);
    return () => window.removeEventListener("keydown", aoTeclar);
  }, [selectedId, modal, fecharPainel]);
  const openSettings = () => {
    atendentes.recarregar();
    setModal("settings");
  };
  const assume = async (detalhe: DetalheConversaFila) => {
    if (!atendente || ocupado) return;
    setOcupado(true);
    try {
      await assumirConversa(detalhe.conversaId, atendente.id);
      setAtendimento((lista) => [
        ...lista,
        { detalhe, atendente, assumidaEm: Date.now() },
      ]);
      setAba("atendimento");
      notify(`Você assumiu a conversa de ${detalhe.clienteNome}.`);
    } catch (erro) {
      notify(mensagemDoErro(erro), true);
    } finally {
      setOcupado(false);
      fila.recarregar();
    }
  };
  const resolveConversation = async () => {
    if (!emAtendimento || ocupado) return;
    const { conversaId, clienteNome } = emAtendimento.detalhe;
    const retirar = () =>
      setAtendimento((lista) =>
        lista.filter((a) => a.detalhe.conversaId !== conversaId),
      );
    setOcupado(true);
    try {
      await encerrarConversa(conversaId);
      retirar();
      notify(`Atendimento de ${clienteNome} encerrado.`);
    } catch (erro) {
      if (erro instanceof ErroApi && [404, 409].includes(erro.status))
        retirar();
      notify(mensagemDoErro(erro), true);
    } finally {
      setOcupado(false);
      setModal(null);
      fila.recarregar();
    }
  };
  const goHome = () => {
    setAba("fila");
    setSelectedId(null);
  };
  const dateLabel = new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    timeZone: "America/Sao_Paulo",
  }).format(new Date());
  const placeholder = atendentes.lista
    ? "Escolha quem está atendendo"
    : atendentes.erro
      ? "Lista indisponível"
      : "Carregando atendentes…";

  return (
    <>
      <div className="app-shell">
        <Sidebar
          count={aguardando}
          atendente={atendente}
          backend={backend}
          demo={DEMO_ATIVO}
          onHome={goHome}
          onSettings={openSettings}
        />
        <main className="main-content">
          <header className="page-header">
            <div>
              <div className="breadcrumb">
                Atendimento <span>/</span> <b>Visão geral</b>
              </div>
              <h1>
                Fila de conversas
                <span className="heading-dot" />
              </h1>
              <p>Cada conversa, um atendimento mais próximo.</p>
            </div>
            <div className="header-actions">
              <span className="today">{dateLabel}</span>
              <button
                className="icon-button notifications"
                onClick={() => setModal("notifications")}
                aria-label="Ver notificações"
              >
                <Icon name="bell" />
                {aguardando ? <i /> : null}
              </button>
              <button
                className="profile-button"
                onClick={openSettings}
                aria-label={
                  atendente
                    ? `Perfil de ${atendente.nome}`
                    : "Escolher o atendente"
                }
              >
                <span className="avatar profile-avatar header-avatar">
                  {atendente ? iniciais(atendente.nome) : "?"}
                </span>
              </button>
            </div>
          </header>
          {DEMO_ATIVO ? (
            <div className="demo-banner" role="status">
              <span><strong>Modo demonstração</strong> · Conversas fictícias para testar a tela.</span>
              <a href={window.location.pathname}>Voltar para a fila real</a>
            </div>
          ) : import.meta.env.DEV && fila.erro ? (
            <div className="demo-banner">
              <span>Servidor indisponível? Você pode explorar a tela com dados fictícios.</span>
              <a href="?demo=1">Abrir demonstração</a>
            </div>
          ) : null}
          <Metrics
            fila={fila.itens}
            minhas={atendimento.length}
            agora={fila.atualizadoEm}
          />
          <ConversationTable
            fila={fila.itens}
            atendimento={atendimento}
            aba={aba}
            onAba={setAba}
            selectedId={selectedId}
            onSelect={abrir}
            erro={fila.erro}
            carregando={fila.carregando}
            agora={fila.atualizadoEm}
            onRefresh={fila.recarregar}
          />
          <footer className="main-footer">
            <span>
              <i />
              {fila.itens
                ? `Fila atualizada às ${horaExata(fila.atualizadoEm)}`
                : fila.erro
                  ? "Fila indisponível"
                  : "Carregando a fila…"}
            </span>
            <span>
              WXN Tecnologia <b>·</b> Atualização automática a cada 15 s
            </span>
          </footer>
        </main>
        <aside
          ref={painel}
          tabIndex={-1}
          className={`conversation-panel ${aberta ? "" : "panel-closed"}`}
          aria-label="Detalhes da conversa"
        >
          <ConversationDetail
            key={selectedId}
            conversa={naFila}
            atendimento={emAtendimento}
            atendente={atendente}
            agora={fila.atualizadoEm}
            ocupado={ocupado}
            onClose={fecharPainel}
            onAssume={assume}
            onResolve={() => setModal("resolve")}
            onSettings={openSettings}
            onIndisponivel={conversaSaiuDaFila}
          />
        </aside>
      </div>
      <div className="toast-region">
        <div role="status" aria-live="polite">
          {toast && !toast.erro && <div className="toast">{toast.text}</div>}
        </div>
        <div role="alert">
          {toast?.erro && <div className="toast">{toast.text}</div>}
        </div>
      </div>
      {modal === "settings" && (
        <Modal title="Configurações" onClose={() => setModal(null)}>
          <div className="settings-profile">
            <span className="avatar profile-avatar">
              {atendente ? iniciais(atendente.nome) : "?"}
            </span>
            <div>
              <strong>{atendente?.nome ?? "Nenhum atendente escolhido"}</strong>
              <small>
                {atendente
                  ? `${atendente.email} · ${atendente.disponivel ? "Disponível" : "Indisponível"}`
                  : "Escolha abaixo quem está atendendo."}
              </small>
            </div>
          </div>
          <label>
            Atendendo como
            <select
              value={atendente?.id ?? ""}
              onChange={(e) => atendentes.escolher(e.target.value)}
              disabled={!atendentes.lista}
            >
              <option value="" disabled>
                {placeholder}
              </option>
              {atendentes.lista?.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.nome}
                  {a.disponivel ? "" : " (indisponível)"}
                </option>
              ))}
            </select>
          </label>
          {atendentes.erro && (
            <div className="error-banner" role="alert">
              <span>{atendentes.erro}</span>
              <button className="text-button" onClick={atendentes.recarregar}>
                Tentar novamente
              </button>
            </div>
          )}
          {atendentes.lista?.length === 0 && (
            <p>Nenhum atendente cadastrado no servidor.</p>
          )}
          <p>A escolha fica salva neste navegador.</p>
          <div className="setting-row">
            <span>
              {DEMO_ATIVO ? "Fonte dos dados" : "Servidor"}
              <small>{DEMO_ATIVO ? "Exemplos locais" : API_URL}</small>
            </span>
            {DEMO_ATIVO ? (
              <span className="status-badge status-badge--demo">Demonstração</span>
            ) : <StatusBadge status={backend} />}
          </div>
          <div className="setting-row">
            <span>
              Fila de conversas
              <small>
                {fila.itens
                  ? `Atualizada às ${horaExata(fila.atualizadoEm)}`
                  : "Ainda não carregada"}
              </small>
            </span>
            {aguardando !== undefined && (
              <span className="badge waiting">{aguardando} aguardando</span>
            )}
          </div>
          <div className="modal-actions">
            <button className="button" onClick={() => setModal(null)}>
              Concluir
            </button>
          </div>
        </Modal>
      )}
      {modal === "notifications" && (
        <Modal title="Central de notificações" onClose={() => setModal(null)}>
          {fila.erro && (
            <div className="notification-item">
              <strong>Não foi possível atualizar a fila</strong>
              <small>{fila.erro}</small>
            </div>
          )}
          <div className="notification-item">
            {aguardando === undefined ? (
              <strong>Fila ainda não carregada</strong>
            ) : (
              <>
                <strong>
                  {plural(
                    aguardando,
                    "conversa aguardando",
                    "conversas aguardando",
                  )}{" "}
                  atendimento
                </strong>
                <small>
                  {maisAntiga
                    ? `A mais antiga espera há ${esperaDesde(maisAntiga.abertoEm, fila.atualizadoEm)}.`
                    : "Nenhuma conversa na fila agora."}
                </small>
              </>
            )}
          </div>
          <div className="notification-item">
            <strong>
              {plural(
                atendimento.length,
                "conversa em atendimento",
                "conversas em atendimento",
              )}{" "}
              por você
            </strong>
            <small>
              {atendente
                ? `Atendendo como ${atendente.nome}.`
                : "Escolha o atendente nas configurações."}
            </small>
          </div>
          <div className="modal-actions">
            <button
              className="button"
              onClick={() => {
                setModal(null);
                goHome();
              }}
            >
              Ver fila de conversas
            </button>
          </div>
        </Modal>
      )}
      {modal === "resolve" && emAtendimento && (
        <Modal title="Encerrar atendimento?" onClose={() => setModal(null)}>
          <p>
            A conversa de {emAtendimento.detalhe.clienteNome} será encerrada e
            sairá da sua lista de atendimentos.
          </p>
          <div className="modal-actions">
            <button
              className="button secondary"
              disabled={ocupado}
              onClick={() => setModal(null)}
            >
              Continuar atendendo
            </button>
            <button
              className="button"
              disabled={ocupado}
              onClick={resolveConversation}
            >
              Encerrar atendimento
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}
