package br.undb.semear.controller.financeiro;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.undb.semear.integration.cora.CoraGateway;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.service.financeiro.CobrancaService;

/**
 * Aviso de pagamento do Cora (rota pública, protegida pela assinatura HMAC no cabeçalho X-Cora-Assinatura).
 * Corpo: {"evento": "boleto.pago", "id": "cora_..."}. Reentrega de boleto já pago responde 200.
 */
@RestController
@RequestMapping("/api/webhooks")
public class CoraWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(CoraWebhookController.class);

    private final CoraGateway cora;
    private final CobrancaService cobrancas;
    private final ObjectMapper json;

    public CoraWebhookController(CoraGateway cora, CobrancaService cobrancas, ObjectMapper json) {
        this.cora = cora;
        this.cobrancas = cobrancas;
        this.json = json;
    }

    @PostMapping("/cora")
    public ResponseEntity<Map<String, Object>> receber(@RequestBody byte[] corpo,
                                                       @RequestHeader(name = "X-Cora-Assinatura", required = false) String assinatura) {
        if (!cora.assinaturaValida(corpo, assinatura)) {
            LOG.warn("Webhook do Cora recusado: assinatura inválida ou segredo não configurado");
            return ResponseEntity.status(401).body(Map.of("codigo", "ASSINATURA_INVALIDA"));
        }
        JsonNode evento;
        try {
            evento = json.readTree(corpo);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("codigo", "JSON_INVALIDO"));
        }
        if (!"boleto.pago".equals(evento.path("evento").asText()) || evento.path("id").asText().isBlank()) {
            return ResponseEntity.accepted().body(Map.of("codigo", "EVENTO_IGNORADO"));
        }
        try {
            boolean baixou = cobrancas.confirmarPagamentoSite(evento.path("id").asText());
            return ResponseEntity.ok(Map.of("codigo", baixou ? "PAGAMENTO_REGISTRADO" : "JA_ESTAVA_PAGA"));
        } catch (RecursoNaoEncontradoException e) {
            return ResponseEntity.status(404).body(Map.of("codigo", "BOLETO_DESCONHECIDO"));
        }
    }
}
