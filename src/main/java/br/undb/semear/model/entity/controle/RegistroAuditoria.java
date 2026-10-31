package br.undb.semear.model.entity.controle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Uma linha da trilha de auditoria, como gravada pelo trigger fn_auditar.
 * Os textos (data em UTC e jsonb) vêm do banco exatamente como entraram no hash.
 */
public record RegistroAuditoria(long id, String ocorridoEmUtc, UUID usuarioId, String papelBanco, String tabela,
                                String operacao, String registroId, String dadosAntes, String dadosDepois,
                                String hashAnterior, String hash) {

    /** Mesmo texto da função fn_auditoria_conteudo do banco. */
    public String conteudo() {
        return String.join("|", String.valueOf(id), ocorridoEmUtc, usuarioId == null ? "" : usuarioId.toString(),
                papelBanco, tabela, operacao, nulo(registroId), nulo(dadosAntes), nulo(dadosDepois), hashAnterior);
    }

    /** SHA-256 recalculado aqui, sem confiar no hash gravado. */
    public String hashCalculado() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(conteudo().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String nulo(String texto) {
        return texto == null ? "" : texto;
    }
}
