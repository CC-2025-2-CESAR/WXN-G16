import type { Atendente, EmAtendimento } from '../../types/fila';
import { iniciais } from '../formato';
import { Icon } from '../icons';

type Props = {
  atendente: Atendente;
  atendimento: EmAtendimento[];
  onSelect: (id: string) => void;
  onQueue: () => void;
};

export function ActiveOverview({ atendente, atendimento, onSelect, onQueue }: Props) {
  const precisamResposta = atendimento.filter((item) => item.detalhe.mensagens.at(-1)?.tipoRemetente === 'CLIENTE');
  const foco = precisamResposta[0] ?? atendimento[0];
  const primeiroNome = atendente.nome.split(' ')[0];

  return (
    <section className="active-overview" aria-label="Resumo dos seus atendimentos">
      <div className="active-overview-main">
        <span className="active-overview-kicker"><span /> SEU ESPAÇO DE ATENDIMENTO</span>
        <h2>{atendimento.length ? `Tudo em mãos, ${primeiroNome}.` : `Tudo pronto, ${primeiroNome}.`}</h2>
        <p>{atendimento.length
          ? 'As conversas que você assumiu ficam aqui. Abra uma para acompanhar as mensagens e responder ao cliente.'
          : 'Quando assumir uma conversa na fila, ela aparecerá aqui para você continuar o atendimento.'}</p>
        <div className="active-overview-footer">
          <div className="active-overview-stat"><strong>{atendimento.length}</strong><span>com você agora</span></div>
          <span className="active-overview-divider" aria-hidden="true" />
          <div className="active-overview-stat"><strong>{precisamResposta.length}</strong><span>com última mensagem do cliente</span></div>
        </div>
      </div>
      <div className="active-overview-focus">
        <span className="active-overview-focus-label"><Icon name="sparkles" /> {foco ? 'EM FOCO AGORA' : 'PRÓXIMO PASSO'}</span>
        {foco ? (
          <>
            <div className="active-overview-person">
              <span className="avatar">{iniciais(foco.detalhe.clienteNome)}</span>
              <div><strong>{foco.detalhe.clienteNome}</strong><small>Conversa em andamento</small></div>
            </div>
            <p>{foco.detalhe.motivo}</p>
            <button className="active-overview-link" onClick={() => onSelect(foco.detalhe.conversaId)}>
              Continuar conversa <Icon name="chevron-right" />
            </button>
          </>
        ) : (
          <>
            <div className="active-overview-empty-icon"><Icon name="messages" /></div>
            <p>A fila está esperando por você. Escolha uma conversa para começar.</p>
            <button className="active-overview-link" onClick={onQueue}>
              Explorar a fila <Icon name="chevron-right" />
            </button>
          </>
        )}
      </div>
    </section>
  );
}
