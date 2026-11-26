import { useState, type FormEvent } from 'react'
import './login.css'

type Props = {
  loading: boolean
  error: string | null
  onLogin: (email: string, password: string) => void
}

const CURRENT_YEAR = new Date().getFullYear()

function ArrowIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M4 12h15m-6-6 6 6-6 6" />
    </svg>
  )
}

function EyeIcon({ visible }: { visible: boolean }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {visible ? <><path d="M3 3l18 18M10.6 10.6a2 2 0 0 0 2.8 2.8" /><path d="M8.6 5.3A11 11 0 0 1 12 4c5.2 0 9 5.4 9 8a10.9 10.9 0 0 1-3.1 4.5M6.1 6.1C4.1 7.7 3 10.1 3 12c0 2.6 3.8 8 9 8 1.3 0 2.5-.3 3.6-.9" /></> : <><path d="M3 12s3.5-8 9-8 9 8 9 8-3.5 8-9 8-9-8-9-8Z" /><circle cx="12" cy="12" r="3" /></>}
    </svg>
  )
}

export function LoginScreen({ loading, error, onLogin }: Props) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!loading && email.trim() && password) onLogin(email.trim(), password)
  }

  return (
    <main className="wxn-login-page">
      <section className="wxn-login-story" aria-label="WXN Tecnologia">
        <div className="wxn-login-story-inner">
          <div className="wxn-login-brand">
            <img src="/wxn-logo.png" alt="WXN Tecnologia" />
            <span>Central de atendimento</span>
          </div>

          <div className="wxn-login-story-copy">
            <span className="wxn-login-kicker"><span /> Pessoas no centro de cada conversa</span>
            <h1>O melhor atendimento começa <em>por aqui.</em></h1>
            <p>Um espaço para acompanhar a fila, entender cada história e responder com a atenção que o cliente merece.</p>
          </div>

          <div className="wxn-login-story-footer">
            <div className="wxn-login-story-line" />
            <span>WXN Tecnologia</span>
            <span>Feito para quem cuida de pessoas</span>
          </div>
        </div>
        <div className="wxn-login-orbit wxn-login-orbit-one" aria-hidden="true" />
        <div className="wxn-login-orbit wxn-login-orbit-two" aria-hidden="true" />
      </section>

      <section className="wxn-login-access" aria-labelledby="wxn-login-title">
        <div className="wxn-login-access-inner">
          <div className="wxn-login-access-top">
            <span className="wxn-login-access-symbol" aria-hidden="true">W<span>.</span></span>
            <span className="wxn-login-access-label">Workspace WXN</span>
          </div>

          <div className="wxn-login-heading">
            <span className="wxn-login-eyebrow">ACESSO DA EQUIPE</span>
            <h2 id="wxn-login-title">Bem-vindo de volta</h2>
            <p>Entre com seu e-mail e senha para continuar seu trabalho.</p>
          </div>
          <form className="wxn-login-form" onSubmit={submit}>
            <label htmlFor="wxn-login-email">E-mail corporativo</label>
            <input
              id="wxn-login-email"
              type="email"
              inputMode="email"
              autoComplete="username"
              placeholder="seu.nome@wxntecnologia.com.br"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={loading}
              required
            />
            <div className="wxn-login-password-label"><label htmlFor="wxn-login-password">Senha</label></div>
            <div className="wxn-login-password-field">
              <input
                id="wxn-login-password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="Digite sua senha"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                disabled={loading}
                required
              />
              <button type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'} aria-pressed={showPassword} disabled={loading}>
                <EyeIcon visible={showPassword} />
              </button>
            </div>
            {error && <div className="wxn-login-error" role="alert">{error}</div>}
            <button className="wxn-login-submit" type="submit" disabled={loading}>
              <span>{loading ? 'Entrando…' : 'Entrar no workspace'}</span><ArrowIcon />
            </button>
          </form>
          <p className="wxn-login-help">Precisa de acesso? Fale com o responsável pela central na WXN.</p>
          <div className="wxn-login-access-bottom"><span>Área da equipe WXN</span><span>© {CURRENT_YEAR} WXN Tecnologia</span></div>
        </div>
      </section>
    </main>
  )
}
