package br.undb.semear.integration.notificacao;

import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Factory: devolve o notificador de cada canal. Por enquanto todos registram em log;
 * para enviar e-mail de verdade, basta registrar aqui um NotificadorEmail para "EMAIL".
 */
@Component
public class NotificadorFactory {

    private final Map<String, Notificador> porCanal = Map.of(
            "EMAIL", new NotificadorLog("EMAIL"),
            "SMS", new NotificadorLog("SMS"),
            "WHATSAPP", new NotificadorLog("WHATSAPP"));

    public Notificador para(String canal) {
        Notificador notificador = porCanal.get(canal);
        if (notificador == null) {
            throw new IllegalArgumentException("Canal de notificação desconhecido: " + canal);
        }
        return notificador;
    }
}
