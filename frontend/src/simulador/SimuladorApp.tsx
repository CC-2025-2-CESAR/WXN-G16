import { useEffect, useState, type FormEvent } from 'react';
import { StatusBadge } from '../components/StatusBadge';
import { useBackendHealth } from '../hooks/useBackendHealth';
import { gravarArmazenado, lerArmazenado } from './armazenamento';
import { Celular } from './Celular';
import type { Cliente, StatusConversa } from './tipos';
import { useConversaSimulada } from './useConversaSimulada';
import './simulador.css';

const CHAVE_DO_CLIENTE = 'wxn.simulador.v1.cliente';
const CLIENTE_PADRAO: Cliente = { telefone: '81999990707', nome: 'Maria Souza' };

const EXEMPLOS = [
  'Oi, tudo bem?',
  'Como funciona o atendimento?',
  'Qual o status da WXN-2026-0001?',
  'Quero abrir um chamado para aquele problema do relatório',
  'Meu sistema caiu e a empresa está parada!',
  'Quero falar com um atendente',
];

const ROTULO_DO_STATUS: Record<StatusConversa, string> = {
  COM_CHATBOT: 'Com a Lily',
  AGUARDANDO_ATENDENTE: 'Aguardando atendente',
  COM_ATENDENTE: 'Com atendente',
  ENCERRADA: 'Encerrada',
};

function clienteSalvo(): Cliente {
  const salvo = lerArmazenado<Partial<Cliente>>(CHAVE_DO_CLIENTE);
  if (salvo && typeof salvo.telefone === 'string' && /^\d{10,11}$/.test(salvo.telefone)) {
    return { telefone: salvo.telefone, nome: typeof salvo.nome === 'string' ? salvo.nome : '' };
  }
  return CLIENTE_PADRAO;
}

/** Só os dígitos, sem o 55: o servidor trata com e sem o país como o mesmo cliente, e a tela deve fazer o mesmo. */
function normalizarTelefone(texto: string) {
  const digitos = texto.replace(/\D/g, '');
  const comPais = digitos.startsWith('55') && (digitos.length === 12 || digitos.length === 13);
  return comPais ? digitos.slice(2) : digitos;
}
const telefoneValido = (digitos: string) => digitos.length >= 10 && digitos.length <= 11;

function telefoneAleatorio() {
  const final = String(Math.floor(Math.random() * 1e8)).padStart(8, '0');
  return `819${final}`;
}

type FormularioProps = { cliente: Cliente; onUsar: (cliente: Cliente) => void };

function FormularioDoCliente({ cliente, onUsar }: FormularioProps) {
  const [telefone, setTelefone] = useState(cliente.telefone);
  const [nome, setNome] = useState(cliente.nome);
  const [erro, setErro] = useState<string | null>(null);

  const usar = (e: FormEvent) => {
    e.preventDefault();
    const digitos = normalizarTelefone(telefone);
    if (!telefoneValido(digitos)) {
      setErro('Informe o DDD e o número (10 ou 11 dígitos, com ou sem o 55).');
      return;
    }
    setErro(null);
    setTelefone(digitos);
    onUsar({ telefone: digitos, nome: nome.trim() });
  };

  const novoCliente = () => {
    const aleatorio = telefoneAleatorio();
    setTelefone(aleatorio);
    setNome('');
    setErro(null);
    onUsar({ telefone: aleatorio, nome: '' });
  };

  return (
    <form className="sim-cliente" onSubmit={usar}>
      <label>
        Telefone do cliente
        <input
          type="tel"
          inputMode="tel"
          value={telefone}
          onChange={(e) => setTelefone(e.target.value)}
          aria-invalid={erro ? true : undefined}
          aria-describedby={erro ? 'sim-erro-telefone' : undefined}
        />
      </label>
      <label>
        Nome (opcional, só vale no primeiro contato)
        <input type="text" value={nome} maxLength={255} onChange={(e) => setNome(e.target.value)} />
      </label>
      <button type="submit" className="sim-botao">
        Usar este cliente
      </button>
      <button type="button" className="sim-botao sim-botao--suave" onClick={novoCliente}>
        Cliente novo
      </button>
      {erro && (
        <p id="sim-erro-telefone" className="sim-erro-campo" role="alert">
          {erro}
        </p>
      )}
    </form>
  );
}

function Simulacao({ cliente }: { cliente: Cliente }) {
  const conversa = useConversaSimulada(cliente);
  const { status, conversaId, enviando, enviar, limparTela } = conversa;

  return (
    <div className="sim-grade">
      <Celular cliente={cliente} conversa={conversa} />
      <aside className="sim-lateral" aria-label="Apoio ao teste">
        <section>
          <h2>Experimente</h2>
          <p>Clique para a Lily receber a frase, ou escreva a sua no celular.</p>
          <ul className="sim-exemplos">
            {EXEMPLOS.map((exemplo) => (
              <li key={exemplo}>
                <button type="button" disabled={enviando} onClick={() => void enviar(exemplo)}>
                  {exemplo}
                </button>
              </li>
            ))}
          </ul>
        </section>
        <section>
          <h2>Conversa no servidor</h2>
          <p className="sim-estado">
            <span className={`sim-pilula sim-pilula--${status ?? 'NENHUMA'}`}>
              {status ? ROTULO_DO_STATUS[status] : 'Ainda sem conversa'}
            </span>
            {conversaId && <small>id {conversaId.slice(0, 8)}</small>}
          </p>
          {status === 'AGUARDANDO_ATENDENTE' && (
            <p>
              A conversa já está na fila: <a href="#/">abra a fila de atendimento</a> para assumir.
            </p>
          )}
          <p>
            O servidor reconhece o cliente pelo telefone. Para recomeçar do zero, use <b>Cliente novo</b>; limpar a
            tela não apaga a conversa.
          </p>
          <button type="button" className="sim-botao sim-botao--suave" onClick={limparTela}>
            Limpar a tela
          </button>
        </section>
      </aside>
    </div>
  );
}

export default function SimuladorApp() {
  const backend = useBackendHealth();
  const [cliente, setCliente] = useState<Cliente>(clienteSalvo);

  useEffect(() => {
    gravarArmazenado(CHAVE_DO_CLIENTE, cliente);
  }, [cliente]);

  useEffect(() => {
    const anterior = document.title;
    document.title = 'Simulador de WhatsApp — Lily';
    return () => {
      document.title = anterior;
    };
  }, []);

  return (
    <div className="sim-pagina">
      <header className="sim-cabecalho">
        <div>
          <a className="sim-voltar" href="#/">
            ← Fila de atendimento
          </a>
          <h1>Simulador de WhatsApp</h1>
          <p>Converse com a Lily como um cliente faria pelo WhatsApp, sem depender da Meta.</p>
        </div>
        <StatusBadge status={backend} />
      </header>
      <FormularioDoCliente cliente={cliente} onUsar={setCliente} />
      <Simulacao key={cliente.telefone} cliente={cliente} />
    </div>
  );
}
