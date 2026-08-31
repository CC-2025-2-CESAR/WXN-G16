import { useEffect, useState } from 'react';
import { getHealth } from '../services/api';
import type { HealthResponse } from '../types/health';

export type BackendStatus = 'loading' | 'online' | 'offline';

interface UseBackendHealthResult {
  status: BackendStatus;
  data: HealthResponse | null;
}

/**
 * Consulta GET /api/health uma vez, ao montar o componente, e resume o
 * resultado em um status simples para a UI (StatusBadge). Existe só para
 * comprovar visualmente que React -> HTTP -> Spring Boot está funcionando;
 * não há retry nem polling automático nesta etapa.
 */
export function useBackendHealth(): UseBackendHealthResult {
  const [status, setStatus] = useState<BackendStatus>('loading');
  const [data, setData] = useState<HealthResponse | null>(null);

  useEffect(() => {
    let cancelled = false;

    getHealth()
      .then((result) => {
        if (cancelled) return;
        setData(result);
        setStatus('online');
      })
      .catch(() => {
        if (cancelled) return;
        setStatus('offline');
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return { status, data };
}
