package br.undb.semear.integration.notificacao;

import br.undb.semear.model.entity.financeiro.NotificacaoCobranca;

/** Envia um aviso de cobrança por um canal (RF05). Devolve false se não conseguiu (vira FALHA na fila). */
public interface Notificador {

    /** Canal atendido, igual ao enum canal_notificacao (EMAIL, SMS, WHATSAPP). */
    String canal();

    boolean enviar(NotificacaoCobranca notificacao);
}
