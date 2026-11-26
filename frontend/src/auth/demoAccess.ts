/** Contas públicas da prévia local; não autenticam no backend. */
const contasDeTeste = [
  { email: 'ana.martins@demo.wxn.example', senha: 'Demo@2026' },
  { email: 'paulo.mendes@demo.wxn.example', senha: 'Paulo@2026' },
] as const

export function emailDaContaDeTeste(email: string, senha: string): string | null {
  const normalizado = email.trim().toLowerCase()
  return contasDeTeste.find((conta) => conta.email === normalizado && conta.senha === senha)?.email ?? null
}
