import { useEffect, useState } from 'react'
import DashboardApp from './dashboard/App'
import SimuladorApp from './simulador/SimuladorApp'
import './dashboard/styles.css'

// Duas telas, escolhidas pelo hash da URL: "#/simulador" abre o simulador de WhatsApp e qualquer outro valor, a fila.
const rotaAtual = () => (window.location.hash.startsWith('#/simulador') ? 'simulador' : 'fila')

export default function App() {
  const [rota, setRota] = useState(rotaAtual)

  useEffect(() => {
    const aoMudar = () => setRota(rotaAtual())
    window.addEventListener('hashchange', aoMudar)
    return () => window.removeEventListener('hashchange', aoMudar)
  }, [])

  // A fila continua montada (só escondida) para não perder a lista "Em atendimento", que só existe na memória da página.
  return (
    <>
      <div hidden={rota !== 'fila'}>
        <DashboardApp />
      </div>
      {rota === 'simulador' && <SimuladorApp />}
    </>
  )
}
