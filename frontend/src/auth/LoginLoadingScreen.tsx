import './login-loading.css'

export function LoginLoadingScreen() {
  return (
    <main className="wxn-loading-screen" role="status" aria-live="polite">
      <img src="/wxn-logo.png" alt="WXN Tecnologia" />
      <span className="wxn-loading-spinner" aria-hidden="true" />
      <p>Carregando painel...</p>
    </main>
  )
}
