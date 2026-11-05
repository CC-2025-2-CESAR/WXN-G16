const fuso = "America/Sao_Paulo";
const formatoHora = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
  timeZone: fuso,
});
const formatoHoraExata = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  timeZone: fuso,
});
const formatoDia = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  timeZone: fuso,
});

type Instante = string | number;

export const hora = (instante: Instante) =>
  formatoHora.format(new Date(instante));
export const horaExata = (instante: Instante) =>
  formatoHoraExata.format(new Date(instante));
export const dia = (instante: Instante) =>
  formatoDia.format(new Date(instante));
export const dataHora = (instante: Instante) =>
  `${dia(instante)}, ${hora(instante)}`;

export const plural = (n: number, singular: string, varios: string) =>
  `${n} ${n === 1 ? singular : varios}`;

export const iniciais = (nome: string) =>
  nome
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("");

export function formatarTelefone(telefone: string) {
  const digitos = telefone.replace(/\D/g, "");
  const comPais =
    (digitos.length === 12 || digitos.length === 13) &&
    digitos.startsWith("55");
  const nacional = comPais ? digitos.slice(2) : digitos;
  if (nacional.length !== 10 && nacional.length !== 11) return telefone;
  return `${comPais ? "+55 " : ""}(${nacional.slice(0, 2)}) ${nacional.slice(2, -4)}-${nacional.slice(-4)}`;
}

export function duracao(ms: number) {
  const minutos = Math.max(0, Math.floor(ms / 60_000));
  if (minutos < 1) return "< 1 min";
  if (minutos < 60) return `${minutos} min`;
  const horas = Math.floor(minutos / 60);
  if (horas < 24)
    return `${horas} h ${String(minutos % 60).padStart(2, "0")} min`;
  return `${Math.floor(horas / 24)} d`;
}

export const esperaDesde = (abertoEm: string, agora: number) =>
  duracao(agora - Date.parse(abertoEm));
