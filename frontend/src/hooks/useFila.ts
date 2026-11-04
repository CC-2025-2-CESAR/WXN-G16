import { useCallback, useEffect, useRef, useState } from 'react';
import { listarFila, mensagemDoErro } from '../services/api';
import type { ConversaNaFila } from '../types/fila';
import { useAtualizacaoAutomatica } from './useAtualizacaoAutomatica';

export function useFila() {
  const [itens, setItens] = useState<ConversaNaFila[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [atualizadoEm, setAtualizadoEm] = useState(() => Date.now());
  const [busca, setBusca] = useState(0);
  const emVoo = useRef(false);

  useEffect(() => {
    const controle = new AbortController();
    emVoo.current = true;

    listarFila(controle.signal)
      .then(
        (fila) => {
          if (controle.signal.aborted) return;
          setItens(fila);
          setErro(null);
          setAtualizadoEm(Date.now());
        },
        (e) => {
          if (!controle.signal.aborted) setErro(mensagemDoErro(e));
        },
      )
      .finally(() => {
        if (controle.signal.aborted) return;
        emVoo.current = false;
        setCarregando(false);
      });

    return () => controle.abort();
  }, [busca]);

  const recarregar = useCallback(() => {
    setCarregando(true);
    setBusca((n) => n + 1);
  }, []);

  const emSegundoPlano = useCallback(() => {
    if (!emVoo.current) setBusca((n) => n + 1);
  }, []);
  useAtualizacaoAutomatica(emSegundoPlano, 15_000);

  return { itens, erro, carregando, atualizadoEm, recarregar };
}
