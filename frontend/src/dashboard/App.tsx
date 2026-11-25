import { useCallback, useEffect, useRef, useState } from "react";
import { StatusBadge } from "../components/StatusBadge";
import { useAtendimentos } from "../hooks/useAtendimentos";
import { useBackendHealth } from "../hooks/useBackendHealth";
import { useFila } from "../hooks/useFila";
import {
  DEMO_ATIVO,
  ErroApi,
  assumirConversa,
  encerrarConversa,
  mensagemDoErro,
} from "../services/api";
import type { Atendente, DetalheConversaFila } from "../types/fila";
import { Icon } from "./icons";
import { esperaDesde, horaExata, iniciais, plural } from "./formato";
import { Sidebar } from "./components/Sidebar";
import { Metrics } from "./components/Metrics";
import { ConversationTable, type Aba } from "./components/ConversationTable";
import { ConversationDetail } from "./components/ConversationDetail";
import { Modal } from "./components/Modal";
import { ActiveOverview } from "./components/ActiveOverview";
import { ActiveConversation } from "./components/ActiveConversation";

type ModalType = "settings" | "notifications" | "resolve" | null;

type Props = { atendente: Atendente; onLogout: () => void; onReady?: () => void };

export default function App({ atendente, onLogout, onReady }: Props) {
  const fila = useFila();
  const meusAtendimentos = useAtendimentos(atendente.id);
  const backend = useBackendHealth();
  const [aba, setAba] = useState<Aba>(() => window.location.hash.startsWith("#/atendimento") ? "atendimento" : "fila");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const atendimento = meusAtendimentos.itens;
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
  const recarregarFila = fila.recarregar;
  const aguardando = fila.itens?.length;
  const emAtendimento = atendimento.find(
    (a) => a.detalhe.conversaId === selectedId,
  );
  const naFila = fila.itens?.find((c) => c.conversaId === selectedId);
  const aberta = emAtendimento ?? naFila;
  // A fila vem ordenada por urgência, então o primeiro item não é necessariamente o mais antigo.
  const maisAntiga = fila.itens?.length
    ? fila.itens.reduce((a, b) => (Date.parse(b.abertoEm) < Date.parse(a.abertoEm) ? b : a))
    : undefined;

  useEffect(() => () => clearTimeout(toastTimer.current), []);
  useEffect(() => {
    if (!fila.carregando && !meusAtendimentos.carregandoInicial) onReady?.();
  }, [fila.carregando, meusAtendimentos.carregandoInicial, onReady]);
  useEffect(() => {
    const sincronizarRota = () => {
      if (!window.location.hash.startsWith("#/simulador"))
        setAba(window.location.hash.startsWith("#/atendimento") ? "atendimento" : "fila");
    };
    window.addEventListener("hashchange", sincronizarRota);
    return () => window.removeEventListener("hashchange", sincronizarRota);
  }, []);
  const notify = useCallback((text: string, erro = false) => {
    clearTimeout(toastTimer.current);
    setToast({ text, erro });
    toastTimer.current = setTimeout(() => setToast(null), erro ? 8000 : 5000);
  }, []);
  const abrir = (id: string) => {
    const ativo = document.activeElement;
    abridor.current =
      ativo instanceof HTMLElement && ativo !== document.body ? ativo : null;
    if (id === selectedId && aba === "fila") painel.current?.focus();
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
    if (selectedId && aba === "fila") painel.current?.focus();
  }, [selectedId, aba]);
  useEffect(() => {
    if (!selectedId || aba !== "fila" || modal) return;
    const aoTeclar = (e: KeyboardEvent) => {
      // No campo de resposta, Esc não pode fechar o painel e jogar fora o texto digitado.
      const emCampo =
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement;
      if (e.key === "Escape" && !emCampo) fecharPainel();
    };
    window.addEventListener("keydown", aoTeclar);
    return () => window.removeEventListener("keydown", aoTeclar);
  }, [selectedId, aba, modal, fecharPainel]);
  const openSettings = () => {
    setModal("settings");
  };
  const assume = async (detalhe: DetalheConversaFila) => {
    if (!atendente || ocupado) return;
    setOcupado(true);
    try {
      await assumirConversa(detalhe.conversaId, atendente.id);
      meusAtendimentos.incluir({ detalhe, atendente, assumidaEm: Date.now() });
      setAba("atendimento");
      window.location.hash = "#/atendimento";
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
    const retirar = () => meusAtendimentos.retirar(conversaId);
    setOcupado(true);
    try {
      await encerrarConversa(conversaId);
      retirar();
      setSelectedId(null);
      notify(`Atendimento de ${clienteNome} encerrado.`);
    } catch (erro) {
      if (erro instanceof ErroApi && [404, 409].includes(erro.status)) {
        retirar();
        setSelectedId(null);
      }
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
    window.location.hash = "#/";
  };
  const goAtendimento = () => {
    setAba("atendimento");
    setSelectedId(null);
    window.location.hash = "#/atendimento";
  };
  const dateLabel = new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    timeZone: "America/Sao_Paulo",
  }).format(new Date(fila.atualizadoEm));

  return (
    <>
      <div className={`app-shell ${aba === "atendimento" ? "active-chat-open" : ""}`}>
        <Sidebar
          count={aguardando}
          activeCount={atendimento.length}
          activePage={aba}
          atendente={atendente}
          backend={backend}
          demo={DEMO_ATIVO}
          onHome={goHome}
          onAtendimento={goAtendimento}
          onSettings={openSettings}
          onLogout={onLogout}
        />
        <main className="main-content">
          <header className="page-header">
            <div>
              <div className="breadcrumb">
                Atendimento <span>/</span> <b>{aba === "fila" ? "Visão geral" : "Meu espaço"}</b>
              </div>
              <h1>
                {aba === "fila" ? "Fila de conversas" : "Em atendimento"}
                <span className="heading-dot" />
              </h1>
              <p>{aba === "fila" ? "Cada conversa, um atendimento mais próximo." : "Suas conversas reunidas para seguir de onde você parou."}</p>
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
                aria-label={`Perfil de ${atendente.nome}`}
              >
                <span className="avatar profile-avatar header-avatar">
                  {iniciais(atendente.nome)}
                </span>
              </button>
            </div>
          </header>
          {aba === "atendimento" && meusAtendimentos.erro && (
            <div className="error-banner" role="alert">
              <span>Não foi possível atualizar seus atendimentos: {meusAtendimentos.erro}</span>
              <button className="text-button" onClick={meusAtendimentos.recarregar}>Tentar novamente</button>
            </div>
          )}
          {aba === "atendimento" && emAtendimento ? (
            <ActiveConversation
              atendimentos={atendimento}
              selectedId={emAtendimento.detalhe.conversaId}
              agora={fila.atualizadoEm}
              ocupado={ocupado}
              onSelect={abrir}
              onBack={fecharPainel}
              onResolve={() => setModal("resolve")}
              onMessageSent={meusAtendimentos.adicionarMensagem}
            />
          ) : (
            <>
              {aba === "fila" ? (
                <Metrics fila={fila.itens} minhas={atendimento.length} agora={fila.atualizadoEm} />
              ) : (
                <ActiveOverview
                  atendente={atendente}
                  atendimento={atendimento}
                  onSelect={abrir}
                  onQueue={goHome}
                />
              )}
              <ConversationTable
                fila={fila.itens}
                atendimento={atendimento}
                aba={aba}
                onAba={abaSelecionada => abaSelecionada === "fila" ? goHome() : goAtendimento()}
                selectedId={selectedId}
                onSelect={abrir}
                erro={fila.erro}
                carregando={fila.carregando}
                agora={fila.atualizadoEm}
                onRefresh={() => { fila.recarregar(); meusAtendimentos.recarregar(); }}
              />
              <footer className="main-footer">
                <span>
                  <i />
                  {aba === "atendimento"
                    ? `${plural(atendimento.length, "conversa em atendimento", "conversas em atendimento")} por você`
                    : fila.itens
                      ? `Fila atualizada às ${horaExata(fila.atualizadoEm)}`
                      : fila.erro
                        ? "Fila indisponível"
                        : "Carregando a fila…"}
                </span>
                <span>
                  WXN Tecnologia <b>·</b> Atualização automática a cada 15 s
                </span>
              </footer>
            </>
          )}
        </main>
        {aba === "fila" && <aside
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
            onMessageSent={meusAtendimentos.adicionarMensagem}
          />
        </aside>}
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
              {iniciais(atendente.nome)}
            </span>
            <div>
              <strong>{atendente.nome}</strong>
              <small>{atendente.email} · {atendente.disponivel ? "Disponível" : "Indisponível"}</small>
            </div>
          </div>
          <p>Sua conta identifica as conversas assumidas e as respostas enviadas por você.</p>
          {!DEMO_ATIVO && (
            <div className="setting-row">
              <span>
                Servidor
                <small>Central de atendimento WXN</small>
              </span>
              <StatusBadge status={backend} />
            </div>
          )}
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
            <button className="button secondary" onClick={() => { setModal(null); onLogout(); }}>
              Sair da conta
            </button>
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
            <small>Atendendo como {atendente.nome}.</small>
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
