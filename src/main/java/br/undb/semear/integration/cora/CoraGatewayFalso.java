package br.undb.semear.integration.cora;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.undb.semear.config.SemearProperties;
import br.undb.semear.model.entity.financeiro.Cobranca;
import br.undb.semear.model.entity.financeiro.ReferenciaCora;

/**
 * Cora simulado (CORA_MODO=falso): gera id e link fictícios e aponta o pagamento para a tela
 * de boleto do próprio portal, onde dá para simular a quitação. O webhook funciona igual ao real
 * (assinatura HMAC-SHA256 com CORA_WEBHOOK_SEGREDO).
 */
@Component
@ConditionalOnProperty(prefix = "semear.cora", name = "modo", havingValue = "falso", matchIfMissing = true)
public class CoraGatewayFalso implements CoraGateway {

    private static final Logger LOG = LoggerFactory.getLogger(CoraGatewayFalso.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final String segredo;

    public CoraGatewayFalso(SemearProperties propriedades) {
        this.segredo = propriedades.cora() == null ? null : propriedades.cora().webhookSegredo();
    }

    @Override
    public ReferenciaCora emitir(Cobranca cobranca, String descricao) {
        byte[] bytes = new byte[10];
        ALEATORIO.nextBytes(bytes);
        String codigo = HexFormat.of().formatHex(bytes);
        LOG.info("Boleto simulado emitido para a cobrança {} ({})", cobranca.getId(), cobranca.getTipo());
        return new ReferenciaCora("cora_" + codigo, "https://pagamentos.example.com/boleto/" + codigo);
    }

    /** No modo falso o link leva à tela de boleto do portal (o example.com não existe). */
    @Override
    public String linkPagamento(ReferenciaCora boleto, UUID cobrancaId) {
        return "/portal/cobrancas/" + cobrancaId;
    }

    @Override
    public boolean assinaturaValida(byte[] corpo, String assinatura) {
        if (segredo == null || segredo.isBlank() || assinatura == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = mac.doFinal(corpo);
            byte[] recebido = HexFormat.of().parseHex(assinatura.trim().toLowerCase());
            return MessageDigest.isEqual(esperado, recebido);
        } catch (IllegalArgumentException e) {
            return false;
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }

    @Override
    public boolean simulado() {
        return true;
    }
}
