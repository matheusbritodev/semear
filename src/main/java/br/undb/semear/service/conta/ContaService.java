package br.undb.semear.service.conta;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.undb.semear.integration.ProvedorIdentidade;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Email;

/**
 * Conta de acesso do responsável no portal. A conta nasce só com e-mail e senha;
 * o perfil RESPONSAVEL é ligado depois, quando ele completa o cadastro (PortalService).
 */
@Service
public class ContaService {

    private static final Logger LOG = LoggerFactory.getLogger(ContaService.class);

    private final ProvedorIdentidade identidade;
    private final LimitadorTentativas limitador;

    public ContaService(ProvedorIdentidade identidade, Clock relogio) {
        this.identidade = identidade;
        this.limitador = new LimitadorTentativas(5, Duration.ofHours(1), relogio);
    }

    public UUID criar(String email, String senha, String origem) {
        limitador.registrar(origem);
        Email endereco = new Email(email);
        validarSenha(senha);
        UUID id = identidade.criarContaConfirmada(endereco, senha);
        LOG.info("Conta do portal criada (usuário {})", id);
        return id;
    }

    /** Mesma regra da View (mensagensAuth.esquemaSenha): 8 a 72 caracteres, com letra e número. */
    static void validarSenha(String senha) {
        if (senha == null || senha.length() < 8 || senha.length() > 72) {
            throw new DadoInvalidoException("senha", "Use de 8 a 72 caracteres");
        }
        if (!senha.matches(".*[A-Za-z].*") || !senha.matches(".*\\d.*")) {
            throw new DadoInvalidoException("senha", "Inclua ao menos uma letra e um número");
        }
    }
}
