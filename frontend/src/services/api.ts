import type { HealthResponse } from '../types/health';

/**
 * URL base do backend. Vem da variável de ambiente VITE_API_URL (ver
 * frontend/.env.example) e cai para localhost:8080 em desenvolvimento se a
 * variável não estiver definida.
 */
export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

/**
 * Consulta GET /api/health no backend. Lança um erro se a resposta não for
 * bem-sucedida ou se o backend estiver inacessível (usado pelo
 * hook useBackendHealth para decidir entre "conectado" e "indisponível").
 */
export async function getHealth(): Promise<HealthResponse> {
  const response = await fetch(`${API_URL}/api/health`);

  if (!response.ok) {
    throw new Error(`Backend respondeu com status ${response.status}`);
  }

  return response.json() as Promise<HealthResponse>;
}
