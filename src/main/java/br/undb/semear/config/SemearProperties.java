package br.undb.semear.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações próprias da aplicação (prefixo "semear" no application.yml).
 * Os valores vêm do .env da raiz ou de variáveis de ambiente.
 */
@ConfigurationProperties(prefix = "semear")
public record SemearProperties(Supabase supabase, Cors cors, Cora cora) {

    public record Supabase(String url, String secretKey, String jwtIssuer, String jwtAudience) {

        public String jwksUri() {
            return url + "/auth/v1/.well-known/jwks.json";
        }
    }

    public record Cors(List<String> origens) {
    }

    public record Cora(String modo, String webhookSegredo) {
    }
}
