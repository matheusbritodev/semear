package br.undb.semear.integration.cora;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import br.undb.semear.config.SemearProperties;

class CoraGatewayFalsoTest {

    private static final String SEGREDO = "segredo-de-teste";
    private final byte[] corpo = "{\"evento\":\"boleto.pago\",\"id\":\"cora_abc\"}".getBytes(StandardCharsets.UTF_8);

    private CoraGatewayFalso gateway(String segredo) {
        return new CoraGatewayFalso(new SemearProperties(null, new SemearProperties.Cors(List.of()),
                new SemearProperties.Cora("falso", segredo)));
    }

    private static String assinar(byte[] corpo, String segredo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(corpo));
    }

    @Test
    void aceitaSoAssinaturaFeitaComOSegredo() throws Exception {
        CoraGatewayFalso cora = gateway(SEGREDO);
        assertThat(cora.assinaturaValida(corpo, assinar(corpo, SEGREDO))).isTrue();
        assertThat(cora.assinaturaValida(corpo, assinar(corpo, "outro"))).isFalse();
        assertThat(cora.assinaturaValida(corpo, "nao-e-hex")).isFalse();
        assertThat(cora.assinaturaValida(corpo, null)).isFalse();
    }

    @Test
    void corpoAlteradoInvalidaAAssinatura() throws Exception {
        String assinatura = assinar(corpo, SEGREDO);
        byte[] adulterado = "{\"evento\":\"boleto.pago\",\"id\":\"cora_xyz\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(gateway(SEGREDO).assinaturaValida(adulterado, assinatura)).isFalse();
    }

    @Test
    void semSegredoConfiguradoRecusaTudo() throws Exception {
        String assinatura = assinar(corpo, SEGREDO);
        assertThat(gateway("").assinaturaValida(corpo, assinatura)).isFalse();
        assertThat(gateway(null).assinaturaValida(corpo, assinatura)).isFalse();
    }
}
