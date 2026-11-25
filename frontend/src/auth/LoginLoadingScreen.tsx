import './login-loading.css'

type Props = {
  nome?: string
}

export function LoginLoadingScreen({ nome }: Props) {
  const primeiroNome = nome?.trim().split(/\s+/)[0]

  return (
    <main className="wxn-loading-screen" aria-busy="true">
      <header className="wxn-loading-header">
        <img src="/wxn-logo.png" alt="WXN Tecnologia" />
        <span>Central de atendimento</span>
      </header>

      <section className="wxn-loading-content" role="status" aria-live="polite">
        <div className="wxn-loading-orbit" aria-hidden="true">
          <span className="wxn-loading-orbit-ring" />
          <span className="wxn-loading-orbit-core">W<span>.</span></span>
          <span className="wxn-loading-orbit-dot" />
        </div>
        <span className="wxn-loading-eyebrow">ACESSO CONFIRMADO</span>
        <h1>Preparando seu espaço{primeiroNome ? `, ${primeiroNome}` : ''}<span>.</span></h1>
        <p>Estamos reunindo suas conversas e organizando a fila de atendimento.</p>
        <div className="wxn-loading-progress" aria-hidden="true"><span /></div>
      </section>

      <footer className="wxn-loading-footer">
        <span>WXN Tecnologia</span>
        <span>Cada conversa importa.</span>
      </footer>
    </main>
  )
}
