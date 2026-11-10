// O localStorage pode estar bloqueado ou cheio (janela anônima, por exemplo): o simulador continua funcionando sem ele.

export function lerArmazenado<T>(chave: string): T | null {
  try {
    const bruto = localStorage.getItem(chave);
    return bruto ? (JSON.parse(bruto) as T) : null;
  } catch {
    return null;
  }
}

export function gravarArmazenado(chave: string, valor: unknown) {
  try {
    localStorage.setItem(chave, JSON.stringify(valor));
  } catch {
    // Sem armazenamento: a conversa só não sobrevive a recarregar a página.
  }
}
