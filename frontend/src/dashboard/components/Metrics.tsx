import type { ConversaNaFila } from "../../types/fila";
import { duracao } from "../formato";
import { Icon, type IconName } from "../icons";

type Props = {
  fila: ConversaNaFila[] | null;
  minhas: number;
  agora: number;
};
type Card = { titulo: string; valor: string; nota: string; icone: IconName };
export function Metrics({ fila, minhas, agora }: Props) {
  const esperas = (fila ?? []).map((c) => agora - Date.parse(c.abertoEm));
  const maior = esperas.length ? duracao(Math.max(...esperas)) : "—";
  const media = esperas.length
    ? duracao(esperas.reduce((soma, e) => soma + e, 0) / esperas.length)
    : "—";
  const cards: Card[] = [
    {
      titulo: "Aguardando agora",
      valor: fila ? String(fila.length) : "—",
      nota: "conversas na fila",
      icone: "inbox",
    },
    {
      titulo: "Maior espera",
      valor: maior,
      nota: "conversa mais antiga da fila",
      icone: "clock",
    },
    {
      titulo: "Espera média",
      valor: media,
      nota: "entre as conversas da fila",
      icone: "clock",
    },
    {
      titulo: "Em atendimento por você",
      valor: String(minhas),
      nota: "nesta sessão",
      icone: "users",
    },
  ];
  return (
    <section className="metrics" aria-label="Indicadores da fila">
      {cards.map((c) => (
        <article className="metric" key={c.titulo}>
          <div className="metric-top">
            <span>{c.titulo}</span>
            <span className="metric-icon">
              <Icon name={c.icone} />
            </span>
          </div>
          <div className="metric-value">{c.valor}</div>
          <div className="metric-bottom">
            <span>{c.nota}</span>
          </div>
        </article>
      ))}
    </section>
  );
}
