package br.undb.semear.dto;

/** Resposta do endpoint de saúde: status da aplicação e do banco. */
public record SaudeResponse(String status, String banco) {
}
