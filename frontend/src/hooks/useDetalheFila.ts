import { useEffect, useState } from 'react';
import { ErroApi, abrirConversa, mensagemDoErro } from '../services/api';
import type { DetalheConversaFila } from '../types/fila';

interface Resultado {
  conversaId: string;
  detalhe: DetalheConversaFila | null;
  erro: string | null;
  indisponivel: boolean;
}

export function useDetalheFila(conversaId: string | null) {
  const [resultado, setResultado] = useState<Resultado | null>(null);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    if (!conversaId) return;
    const controle = new AbortController();

    abrirConversa(conversaId, controle.signal).then(
      (detalhe) => {
        if (!controle.signal.aborted) setResultado({ conversaId, detalhe, erro: null, indisponivel: false });
      },
      (e) => {
        if (controle.signal.aborted) return;
        const indisponivel = e instanceof ErroApi && [404, 409].includes(e.status);
        setResultado({
          conversaId,
          detalhe: null,
          erro: indisponivel ? 'Esta conversa não está mais na fila.' : mensagemDoErro(e),
          indisponivel,
        });
      },
    );

    return () => controle.abort();
  }, [conversaId, tentativa]);

  const atual = resultado?.conversaId === conversaId ? resultado : null;

  const recarregar = () => {
    setResultado(null);
    setTentativa((t) => t + 1);
  };

  return {
    detalhe: atual?.detalhe ?? null,
    erro: atual?.erro ?? null,
    indisponivel: atual?.indisponivel ?? false,
    carregando: conversaId !== null && atual === null,
    recarregar,
  };
}
