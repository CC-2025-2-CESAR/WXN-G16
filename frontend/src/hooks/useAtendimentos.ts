import { useCallback, useEffect, useRef, useState } from 'react';
import { listarAtendimentosAtivos, mensagemDoErro } from '../services/api';
import type { EmAtendimento, MensagemResumo } from '../types/fila';
import { useAtualizacaoAutomatica } from './useAtualizacaoAutomatica';

export function useAtendimentos(atendenteId: string) {
  const [itens, setItens] = useState<EmAtendimento[]>([]);
  const [erro, setErro] = useState<string | null>(null);
  const [carregandoInicial, setCarregandoInicial] = useState(true);
  const [busca, setBusca] = useState(0);
  const versao = useRef(0);

  useEffect(() => {
    const controle = new AbortController();
    const versaoInicial = versao.current;
    listarAtendimentosAtivos(atendenteId, controle.signal).then(
      (lista) => {
        if (controle.signal.aborted || versaoInicial !== versao.current) return;
        setItens(lista);
        setErro(null);
      },
      (falha) => {
        if (!controle.signal.aborted) setErro(mensagemDoErro(falha));
      },
    ).finally(() => {
      if (!controle.signal.aborted) setCarregandoInicial(false);
    });
    return () => controle.abort();
  }, [atendenteId, busca]);

  const recarregar = useCallback(() => setBusca((valor) => valor + 1), []);
  useAtualizacaoAutomatica(recarregar, 15_000);

  const incluir = useCallback((novo: EmAtendimento) => {
    versao.current += 1;
    setItens((lista) => [novo, ...lista.filter((item) => item.detalhe.conversaId !== novo.detalhe.conversaId)]);
  }, []);
  const retirar = useCallback((conversaId: string) => {
    versao.current += 1;
    setItens((lista) => lista.filter((item) => item.detalhe.conversaId !== conversaId));
  }, []);

  const adicionarMensagem = useCallback((conversaId: string, mensagem: MensagemResumo) => {
    versao.current += 1;
    setItens((lista) => lista.map((item) => item.detalhe.conversaId === conversaId
      ? { ...item, detalhe: { ...item.detalhe, mensagens: [...item.detalhe.mensagens, mensagem] } }
      : item));
  }, []);

  return { itens, erro, carregandoInicial, recarregar, incluir, retirar, adicionarMensagem };
}
