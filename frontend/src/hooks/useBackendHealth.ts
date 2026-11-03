import { useCallback, useEffect, useRef, useState } from 'react';
import { getHealth } from '../services/api';
import { useAtualizacaoAutomatica } from './useAtualizacaoAutomatica';

export type BackendStatus = 'loading' | 'online' | 'offline';

export function useBackendHealth(): BackendStatus {
  const [status, setStatus] = useState<BackendStatus>('loading');
  const [busca, setBusca] = useState(0);
  const emVoo = useRef(false);

  useEffect(() => {
    const controle = new AbortController();
    emVoo.current = true;

    getHealth(controle.signal)
      .then(
        () => {
          if (!controle.signal.aborted) setStatus('online');
        },
        () => {
          if (!controle.signal.aborted) setStatus('offline');
        },
      )
      .finally(() => {
        if (!controle.signal.aborted) emVoo.current = false;
      });

    return () => controle.abort();
  }, [busca]);

  const verificar = useCallback(() => {
    if (!emVoo.current) setBusca((n) => n + 1);
  }, []);
  useAtualizacaoAutomatica(verificar, 15_000);

  return status;
}
