package br.undb.semear.integration.supabase;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.undb.semear.config.SemearProperties;
import br.undb.semear.integration.ProvedorIdentidade;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.IntegracaoException;
import br.undb.semear.model.valueobject.Email;

/**
 * API administrativa do Supabase Auth (chave secreta, só no servidor).
 * Cria a conta já confirmada para não depender de servidor de e-mail (decisão D10, projeto acadêmico).
 */
@Component
public class SupabaseAuthAdmin implements ProvedorIdentidade {

    private static final Logger LOG = LoggerFactory.getLogger(SupabaseAuthAdmin.class);

    private final RestClient http;

    public SupabaseAuthAdmin(SemearProperties propriedades, RestClient.Builder construtor) {
        this.http = construtor
                .baseUrl(propriedades.supabase().url() + "/auth/v1")
                .defaultHeader("apikey", propriedades.supabase().secretKey())
                .build();
    }

    @Override
    public UUID criarContaConfirmada(Email email, String senha) {
        try {
            Map<?, ?> resposta = http.post().uri("/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email.getEndereco(), "password", senha, "email_confirm", true))
                    .retrieve()
                    .body(Map.class);
            Object id = resposta == null ? null : resposta.get("id");
            if (id == null) {
                throw new IntegracaoException("O serviço de contas não devolveu o usuário criado", null);
            }
            return UUID.fromString(id.toString());
        } catch (HttpClientErrorException e) {
            String corpo = e.getResponseBodyAsString();
            if (corpo.contains("email_exists") || corpo.contains("already been registered")) {
                throw new ConflitoException("EMAIL_JA_CADASTRADO", "Este e-mail já tem conta. Entre ou recupere a senha.");
            }
            if (corpo.contains("weak_password")) {
                throw new DadoInvalidoException("senha", "A senha não atende aos requisitos mínimos");
            }
            LOG.error("Supabase Auth recusou a criação de conta (HTTP {})", e.getStatusCode().value());
            throw new IntegracaoException("Não foi possível criar a conta agora. Tente novamente.", e);
        } catch (RestClientException e) {
            LOG.error("Falha ao falar com o Supabase Auth", e);
            throw new IntegracaoException("Não foi possível criar a conta agora. Tente novamente.", e);
        }
    }

    @Override
    public void definirSenha(UUID usuarioId, String senha) {
        try {
            http.put().uri("/admin/users/" + usuarioId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("password", senha))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            LOG.error("Supabase Auth recusou a troca de senha do usuário {}", usuarioId);
            throw new IntegracaoException("Não foi possível trocar a senha agora. Tente novamente.", e);
        }
    }

    @Override
    public void remover(UUID usuarioId) {
        try {
            http.delete().uri("/admin/users/" + usuarioId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            LOG.warn("Conta {} não removida do Supabase Auth: ficará sem cadastro na escola", usuarioId, e);
        }
    }
}
