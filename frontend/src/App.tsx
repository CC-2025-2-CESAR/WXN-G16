import { useCallback, useEffect, useRef, useState } from 'react'
import DashboardApp from './dashboard/App'
import SimuladorApp from './simulador/SimuladorApp'
import { LoginScreen } from './auth/LoginScreen'
import { LoginLoadingScreen } from './auth/LoginLoadingScreen'
import { emailDaContaDeTeste } from './auth/demoAccess'
import { DEMO_ATIVO, ErroApi, entrar, listarAtendentes, mensagemDoErro, sair, sessaoAtual } from './services/api'
import type { Atendente } from './types/fila'
import './dashboard/styles.css'

const CHAVE_DEMO = 'wxn-demo-atendente'
const rotaAtual = () => (window.location.hash.startsWith('#/simulador') ? 'simulador' : 'fila')

export default function App() {
  const [rota, setRota] = useState(rotaAtual)
  const [atendente, setAtendente] = useState<Atendente | null>(null)
  const [demoAtendentes, setDemoAtendentes] = useState<Atendente[]>([])
  const [iniciando, setIniciando] = useState(true)
  const [ocupado, setOcupado] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [emTransicao, setEmTransicao] = useState(false)
  const painelPronto = useRef(false)
  const tempoMinimo = useRef(false)

  useEffect(() => {
    if (!emTransicao) return
    const timer = window.setTimeout(() => {
      tempoMinimo.current = true
      if (painelPronto.current) setEmTransicao(false)
    }, 900)
    return () => window.clearTimeout(timer)
  }, [emTransicao])

  const painelCarregado = useCallback(() => {
    painelPronto.current = true
    if (tempoMinimo.current) setEmTransicao(false)
  }, [])
  const iniciarEntrada = (perfil: Atendente) => {
    painelPronto.current = false
    tempoMinimo.current = false
    setEmTransicao(true)
    setAtendente(perfil)
  }

  useEffect(() => {
    const aoMudar = () => setRota(rotaAtual())
    window.addEventListener('hashchange', aoMudar)
    return () => window.removeEventListener('hashchange', aoMudar)
  }, [])

  useEffect(() => {
    let ativo = true
    const iniciar = async () => {
      try {
        if (DEMO_ATIVO) {
          const lista = await listarAtendentes()
          if (!ativo) return
          setDemoAtendentes(lista)
          const id = sessionStorage.getItem(CHAVE_DEMO)
          setAtendente(lista.find((item) => item.id === id) ?? null)
        } else {
          const perfil = await sessaoAtual()
          if (ativo) setAtendente(perfil)
        }
      } catch (falha) {
        if (!ativo) return
        if (!(falha instanceof ErroApi && falha.status === 401))
          setErro('Não foi possível conectar à central. Verifique a conexão e tente novamente.')
      } finally {
        if (ativo) setIniciando(false)
      }
    }
    void iniciar()
    return () => { ativo = false }
  }, [])

  useEffect(() => {
    if (DEMO_ATIVO) return
    const expirada = () => {
      setEmTransicao(false)
      setAtendente(null)
      setErro('Sua sessão terminou. Entre novamente para continuar.')
    }
    window.addEventListener('wxn:session-expired', expirada)
    return () => window.removeEventListener('wxn:session-expired', expirada)
  }, [])

  const entrarComSenha = async (email: string, senha: string) => {
    if (ocupado) return
    setOcupado(true)
    setErro(null)
    try {
      iniciarEntrada(await entrar(email.trim(), senha))
    } catch (falha) {
      setErro(falha instanceof ErroApi && falha.status === 401
        ? 'E-mail ou senha incorretos.'
        : mensagemDoErro(falha))
    } finally {
      setOcupado(false)
    }
  }

  const entrarNaDemo = (email: string, senha: string) => {
    const emailDaConta = emailDaContaDeTeste(email, senha)
    if (!emailDaConta) {
      setErro('E-mail ou senha incorretos.')
      return
    }
    const escolhido = demoAtendentes.find((item) => item.email.toLowerCase() === emailDaConta)
    if (!escolhido || !escolhido.disponivel) {
      setErro('Esta conta está indisponível no momento.')
      return
    }
    sessionStorage.setItem(CHAVE_DEMO, escolhido.id)
    iniciarEntrada(escolhido)
    setErro(null)
  }

  const sairDoPainel = async () => {
    if (ocupado) return
    setOcupado(true)
    setErro(null)
    try {
      if (DEMO_ATIVO) sessionStorage.removeItem(CHAVE_DEMO)
      else await sair()
      setEmTransicao(false)
      setAtendente(null)
      window.location.hash = '#/'
    } catch (falha) {
      setErro(mensagemDoErro(falha))
    } finally {
      setOcupado(false)
    }
  }

  if (iniciando) return <div className="auth-loading" role="status">Preparando a central WXN…</div>
  if (!atendente) return (
    <LoginScreen
      loading={ocupado}
      error={erro}
      onLogin={DEMO_ATIVO ? entrarNaDemo : entrarComSenha}
    />
  )

  return (
    <>
      {erro && <div className="session-error" role="alert">{erro}</div>}
      <div hidden={rota !== 'fila' || emTransicao}>
        <DashboardApp atendente={atendente} onLogout={sairDoPainel} onReady={painelCarregado} />
      </div>
      {rota === 'simulador' && !emTransicao && <SimuladorApp />}
      {emTransicao && <LoginLoadingScreen nome={atendente.nome} />}
    </>
  )
}
