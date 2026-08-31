package br.com.wxnchatbot.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint simples usado para comprovar que o backend esta no ar e para o
 * frontend confirmar a conexao (ver frontend/src/hooks/useBackendHealth.ts).
 *
 * Nao consulta o banco de dados de proposito: um health check de liveness
 * deve responder mesmo que uma dependencia externa esteja instavel. A prova
 * de que o backend conseguiu conectar ao PostgreSQL e o proprio processo ter
 * subido sem erros (o Spring falha ao iniciar se o banco configurado em
 * application.properties estiver inacessivel).
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
