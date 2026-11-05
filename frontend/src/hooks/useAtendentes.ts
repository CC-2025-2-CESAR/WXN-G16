import { useEffect, useState } from 'react';
import { listarAtendentes, mensagemDoErro } from '../services/api';
import type { Atendente } from '../types/fila';

const CHAVE_ESCOLHA = 'wxn-atendente-escolhido';

function lerEscolha() {
  try {
    return localStorage.getItem(CHAVE_ESCOLHA);
  } catch {
    return null;
  }
}

export function useAtendentes() {
  const [lista, setLista] = useState<Atendente[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [escolhidoId, setEscolhidoId] = useState(lerEscolha);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    const controle = new AbortController();

    listarAtendentes(controle.signal).then(
      (atendentes) => {
        if (controle.signal.aborted) return;
        setLista(atendentes);
        setErro(null);
      },
      (e) => {
        if (!controle.signal.aborted) setErro(mensagemDoErro(e));
      },
    );

    return () => controle.abort();
  }, [tentativa]);

  const escolher = (id: string) => {
    setEscolhidoId(id);
    try {
      localStorage.setItem(CHAVE_ESCOLHA, id);
    } catch {
      // o armazenamento é opcional: sem ele a escolha vale só até recarregar
    }
  };

  const recarregar = () => {
    setErro(null);
    setTentativa((t) => t + 1);
  };

  return {
    lista,
    erro,
    atendente: lista?.find((a) => a.id === escolhidoId) ?? null,
    escolher,
    recarregar,
  };
}
