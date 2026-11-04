import { useEffect } from 'react';

export function useAtualizacaoAutomatica(atualizar: () => void, intervaloMs: number) {
  useEffect(() => {
    const quandoVisivel = () => {
      if (document.visibilityState === 'visible') atualizar();
    };
    const temporizador = setInterval(quandoVisivel, intervaloMs);
    document.addEventListener('visibilitychange', quandoVisivel);

    return () => {
      clearInterval(temporizador);
      document.removeEventListener('visibilitychange', quandoVisivel);
    };
  }, [atualizar, intervaloMs]);
}
