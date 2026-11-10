import { useCallback, useEffect, useRef, useState } from 'react';
import { mensagemDoErro } from '../services/api';
import { gravarArmazenado, lerArmazenado } from './armazenamento';
import { enviarParaLily } from './servico';
import type { Cliente, EstadoDoEnvio, MensagemSimulada, RespostaChat, StatusConversa } from './tipos';

const LIMITE_GUARDADO = 200;
const LENTO_APOS_MS = 4_000;

type Conversa = {
  mensagens: MensagemSimulada[];
  status: StatusConversa | null;
  conversaId: string | null;
};

const CONVERSA_VAZIA: Conversa = { mensagens: [], status: null, conversaId: null };

let sequencia = 0;
function novoId() {
  sequencia += 1;
  return `${Date.now()}-${sequencia}`;
}

const chaveDaConversa = (telefone: string) => `wxn.simulador.v1.conversa.${telefone}`;

function carregar(telefone: string): Conversa {
  const guardada = lerArmazenado<Conversa>(chaveDaConversa(telefone));
  if (!guardada || !Array.isArray(guardada.mensagens)) return CONVERSA_VAZIA;
  return {
    ...CONVERSA_VAZIA,
    ...guardada,
    // Um envio que a página interrompeu não tem como saber se chegou: volta como falha para o cliente decidir.
    mensagens: guardada.mensagens.map((m) => (m.estado === 'enviando' ? { ...m, estado: 'falhou' } : m)),
  };
}

function marcar(conversa: Conversa, id: string, estado: EstadoDoEnvio): Conversa {
  return { ...conversa, mensagens: conversa.mensagens.map((m) => (m.id === id ? { ...m, estado } : m)) };
}

function avisoDoStatus(anterior: StatusConversa | null, resposta: RespostaChat): string | null {
  const mudou = anterior !== resposta.status;
  switch (resposta.status) {
    case 'AGUARDANDO_ATENDENTE':
      // O servidor só devolve texto da Lily no turno em que ela encaminha; depois disso ela fica calada.
      return resposta.resposta
        ? 'A Lily encaminhou a conversa para um atendente humano.'
        : 'Mensagem registrada. Um atendente vai responder assim que puder.';
    case 'COM_ATENDENTE':
      return mudou
        ? 'Um atendente assumiu a conversa. Neste simulador, as respostas dele ainda não chegam aqui.'
        : 'Mensagem registrada para o atendente.';
    case 'ENCERRADA':
      return 'Conversa encerrada.';
    default:
      return null;
  }
}

function aplicarResposta(conversa: Conversa, id: string, resposta: RespostaChat): Conversa {
  const novas: MensagemSimulada[] = [];
  if (resposta.resposta) {
    novas.push({ id: novoId(), de: 'lily', texto: resposta.resposta, em: Date.now() });
  }
  const aviso = avisoDoStatus(conversa.status, resposta);
  if (aviso) novas.push({ id: novoId(), de: 'sistema', texto: aviso, em: Date.now() });
  return {
    conversaId: resposta.conversaId,
    status: resposta.status,
    mensagens: [...marcar(conversa, id, 'lida').mensagens, ...novas],
  };
}

/**
 * A conversa de um cliente simulado com a Lily. O servidor identifica o cliente pelo telefone, então a tela só guarda
 * as mensagens para mostrar de novo ao recarregar; quem manda no estado da conversa é o backend.
 */
export function useConversaSimulada(cliente: Cliente) {
  const chave = chaveDaConversa(cliente.telefone);
  const [conversa, setConversa] = useState<Conversa>(() => carregar(cliente.telefone));
  const [enviando, setEnviando] = useState(false);
  const [lento, setLento] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const ocupado = useRef(false);
  const cancelar = useRef<AbortController | null>(null);

  useEffect(() => {
    gravarArmazenado(chave, { ...conversa, mensagens: conversa.mensagens.slice(-LIMITE_GUARDADO) });
  }, [chave, conversa]);

  // Sair da tela com um envio em andamento não cancela a requisição: o servidor processa o turno de qualquer jeito.
  const vivo = useRef(true);
  useEffect(() => {
    vivo.current = true;
    return () => {
      vivo.current = false;
    };
  }, []);

  const enviar = useCallback(
    async (texto: string, reenviarId?: string) => {
      const limpo = texto.trim();
      if (!limpo || ocupado.current) return;
      ocupado.current = true;
      setEnviando(true);
      setErro(null);

      const id = reenviarId ?? novoId();
      setConversa((atual) =>
        reenviarId
          ? marcar(atual, id, 'enviando')
          : {
              ...atual,
              mensagens: [...atual.mensagens, { id, de: 'cliente', texto: limpo, em: Date.now(), estado: 'enviando' }],
            },
      );

      const controle = new AbortController();
      cancelar.current = controle;
      const aviso = setTimeout(() => setLento(true), LENTO_APOS_MS);
      try {
        const resposta = await enviarParaLily(
          { telefone: cliente.telefone, nome: cliente.nome, texto: limpo },
          controle.signal,
        );
        if (vivo.current) {
          setConversa((atual) => aplicarResposta(atual, id, resposta));
        } else {
          // A tela foi fechada (ou o cliente trocado) durante o envio: grava direto para a resposta aparecer na volta.
          gravarArmazenado(
            chaveDaConversa(cliente.telefone),
            aplicarResposta(carregar(cliente.telefone), id, resposta),
          );
        }
      } catch (falha) {
        if (!controle.signal.aborted) {
          setConversa((atual) => marcar(atual, id, 'falhou'));
          setErro(mensagemDoErro(falha));
        }
      } finally {
        clearTimeout(aviso);
        ocupado.current = false;
        if (!controle.signal.aborted) {
          setEnviando(false);
          setLento(false);
        }
      }
    },
    [cliente.telefone, cliente.nome],
  );

  const reenviar = useCallback(
    (id: string) => {
      const mensagem = conversa.mensagens.find((m) => m.id === id);
      if (mensagem) void enviar(mensagem.texto, id);
    },
    [conversa.mensagens, enviar],
  );

  /**
   * Limpa só as mensagens da tela: a conversa no servidor continua (e o status dela também), e só outro telefone
   * começa do zero.
   */
  const limparTela = useCallback(() => {
    cancelar.current?.abort();
    ocupado.current = false;
    setEnviando(false);
    setLento(false);
    setErro(null);
    setConversa((atual) => ({ ...atual, mensagens: [] }));
  }, []);

  return {
    mensagens: conversa.mensagens,
    status: conversa.status,
    conversaId: conversa.conversaId,
    enviando,
    lento,
    erro,
    enviar,
    reenviar,
    limparTela,
  };
}

export type ConversaSimulada = ReturnType<typeof useConversaSimulada>;
