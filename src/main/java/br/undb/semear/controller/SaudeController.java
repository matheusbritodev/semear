package br.undb.semear.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.SaudeResponse;
import br.undb.semear.service.SaudeService;

/** Endpoint público para verificar se a aplicação e o banco estão no ar. */
@RestController
@RequestMapping("/api/saude")
public class SaudeController {

    private final SaudeService saudeService;

    public SaudeController(SaudeService saudeService) {
        this.saudeService = saudeService;
    }

    @GetMapping
    public ResponseEntity<SaudeResponse> verificar() {
        boolean bancoOk = saudeService.bancoDisponivel();
        SaudeResponse resposta = new SaudeResponse(bancoOk ? "UP" : "DEGRADADO", bancoOk ? "OK" : "INDISPONIVEL",
                bancoOk ? saudeService.fusoBanco() : null);
        return ResponseEntity.status(bancoOk ? 200 : 503).body(resposta);
    }
}
