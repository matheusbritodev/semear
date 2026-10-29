package br.undb.semear.service.conta;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import br.undb.semear.model.exception.LimiteTentativasException;

/**
 * Janela deslizante em memória: no máximo N tentativas por chave (ex.: IP) a cada período.
 * Suficiente para uma instância; com várias instâncias, trocar por um contador compartilhado.
 */
public class LimitadorTentativas {

    private final int maximo;
    private final Duration janela;
    private final Clock relogio;
    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();

    public LimitadorTentativas(int maximo, Duration janela, Clock relogio) {
        this.maximo = maximo;
        this.janela = janela;
        this.relogio = relogio;
    }

    /** Registra a tentativa ou lança LimiteTentativasException se a chave estourou o limite. */
    public void registrar(String chave) {
        Instant agora = relogio.instant();
        Instant corte = agora.minus(janela);
        Deque<Instant> fila = tentativas.computeIfAbsent(chave, c -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && !fila.peekFirst().isAfter(corte)) {
                fila.pollFirst();
            }
            if (fila.size() >= maximo) {
                throw new LimiteTentativasException("Muitas tentativas em pouco tempo. Aguarde e tente de novo.");
            }
            fila.addLast(agora);
        }
        if (tentativas.size() > 10_000) {
            tentativas.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
    }
}
