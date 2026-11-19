import { useCallback, useEffect, useRef, useState } from 'react';
import { ErroApi, listarMensagensDoAtendimento } from '../services/api';
import type { MensagemResumo } from '../types/fila';
import { useAtualizacaoAutomatica } from './useAtualizacaoAutomatica';

const INTERVALO_MS = 3_000;

interface Resultado {
  conversaId: string;
  mensagens: MensagemResumo[] | null;
  semConexao: boolean;
  indisponivel: boolean;
}

function mesmasMensagens(a: MensagemResumo[] | null, b: MensagemResumo[]) {
  return (
    a !== null &&
    a.length === b.length &&
    a.every(
      (m, i) => m.enviadaEm === b[i].enviadaEm && m.conteudo === b[i].conteudo && m.tipoRemetente === b[i].tipoRemetente,
    )
  );
}

/**
 * Mantém o histórico de uma conversa em atendimento atualizado: busca ao montar e depois a cada 3 s,
 * enquanto `ativo` e a aba do navegador estiver visível.
 *
 * - `inicial` é o que aparece até a primeira resposta do servidor chegar (o histórico de quando a conversa foi assumida).
 * - Falha de rede mantém a última lista e liga `semConexao`; a próxima consulta tenta de novo sozinha.
 * - 404/409 (a conversa saiu do atendimento) liga `indisponivel` e para de consultar.
 * - `adicionar` coloca na lista, na hora, uma mensagem que o próprio atendente acabou de enviar.
 */
export function useMensagensDoAtendimento(conversaId: string | null, ativo: boolean, inicial?: MensagemResumo[]) {
  const [resultado, setResultado] = useState<Resultado | null>(null);
  const [busca, setBusca] = useState(0);
  const emVoo = useRef(false);
  // Sobe a cada mensagem adicionada à mão; uma consulta que começou antes dela não pode apagá-la ao terminar.
  const versao = useRef(0);

  const atual = conversaId !== null && resultado?.conversaId === conversaId ? resultado : null;
  const indisponivel = atual?.indisponivel ?? false;
  const habilitado = ativo && conversaId !== null && !indisponivel;

  useEffect(() => {
    if (!conversaId || !habilitado) return;
    const controle = new AbortController();
    const versaoInicial = versao.current;
    emVoo.current = true;

    listarMensagensDoAtendimento(conversaId, controle.signal)
      .then(
        (mensagens) => {
          if (controle.signal.aborted || versaoInicial !== versao.current) return;
          // Sem mudança, devolve o mesmo estado para o painel não redesenhar a cada consulta.
          setResultado((anterior) =>
            anterior?.conversaId === conversaId &&
            !anterior.semConexao &&
            !anterior.indisponivel &&
            mesmasMensagens(anterior.mensagens, mensagens)
              ? anterior
              : { conversaId, mensagens, semConexao: false, indisponivel: false },
          );
        },
        (e) => {
          if (controle.signal.aborted) return;
          const saiu = e instanceof ErroApi && [404, 409].includes(e.status);
          setResultado((anterior) => ({
            conversaId,
            mensagens: anterior?.conversaId === conversaId ? anterior.mensagens : null,
            semConexao: !saiu,
            indisponivel: saiu,
          }));
        },
      )
      .finally(() => {
        if (!controle.signal.aborted) emVoo.current = false;
      });

    return () => {
      controle.abort();
      emVoo.current = false;
    };
  }, [conversaId, habilitado, busca]);

  const emSegundoPlano = useCallback(() => {
    if (habilitado && !emVoo.current) setBusca((n) => n + 1);
  }, [habilitado]);
  useAtualizacaoAutomatica(emSegundoPlano, INTERVALO_MS);

  const recarregar = useCallback(() => {
    setResultado((anterior) => (anterior ? { ...anterior, semConexao: false, indisponivel: false } : anterior));
    setBusca((n) => n + 1);
  }, []);

  const adicionar = useCallback(
    (mensagem: MensagemResumo) => {
      versao.current += 1;
      setResultado((anterior) => {
        if (!conversaId) return anterior;
        const base = anterior?.conversaId === conversaId ? anterior : null;
        return {
          conversaId,
          mensagens: [...(base?.mensagens ?? inicial ?? []), mensagem],
          semConexao: base?.semConexao ?? false,
          indisponivel: base?.indisponivel ?? false,
        };
      });
    },
    [conversaId, inicial],
  );

  return {
    mensagens: atual?.mensagens ?? inicial ?? [],
    semConexao: atual?.semConexao ?? false,
    indisponivel,
    recarregar,
    adicionar,
  };
}
