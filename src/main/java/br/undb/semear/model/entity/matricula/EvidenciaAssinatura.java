package br.undb.semear.model.entity.matricula;

import java.util.Objects;
import java.util.UUID;

/**
 * Como a assinatura aconteceu: aceite no portal (com IP e navegador) ou registro feito na secretaria
 * (com o funcionário que registrou).
 */
public record EvidenciaAssinatura(Canal canal, String ip, String navegador, UUID registradaPor) {

    public enum Canal {
        PORTAL,
        PRESENCIAL
    }

    public EvidenciaAssinatura {
        Objects.requireNonNull(canal, "canal");
        ip = cortar(ip, 45);
        navegador = cortar(navegador, 255);
    }

    public static EvidenciaAssinatura portal(String ip, String navegador) {
        return new EvidenciaAssinatura(Canal.PORTAL, ip, navegador, null);
    }

    public static EvidenciaAssinatura presencial(UUID funcionarioId) {
        return new EvidenciaAssinatura(Canal.PRESENCIAL, null, null, funcionarioId);
    }

    private static String cortar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
