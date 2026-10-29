package br.undb.semear.repository.financeiro;

import java.util.function.Function;

import br.undb.semear.model.entity.financeiro.NotificacaoCobranca;

public interface NotificacaoRepository {

    /**
     * Pega até {@code limite} notificações PENDENTE (FOR UPDATE SKIP LOCKED, para duas instâncias não enviarem
     * a mesma), entrega cada uma ao {@code envio} e grava ENVIADA ou FALHA, tudo na mesma transação.
     * Devolve quantas foram enviadas.
     */
    int processarPendentes(int limite, Function<NotificacaoCobranca, Boolean> envio);

    /** fn_rotina_diaria: marca vencidas, gera notificações e atualiza obrigações (idempotente). */
    void rotinaDiaria();
}
