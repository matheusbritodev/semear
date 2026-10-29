package br.undb.semear.integration.notificacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.undb.semear.model.entity.financeiro.NotificacaoCobranca;

/**
 * Notificador de desenvolvimento: monta a mensagem e só registra no log que "enviaria"
 * (sem nome, e-mail ou telefone no log — LGPD). Um por canal.
 */
public class NotificadorLog implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorLog.class);

    private final String canal;

    public NotificadorLog(String canal) {
        this.canal = canal;
    }

    @Override
    public String canal() {
        return canal;
    }

    @Override
    public boolean enviar(NotificacaoCobranca n) {
        String mensagem = MensagemCobranca.para(n);
        LOG.info("[{}] aviso {} da cobrança {} preparado ({} caracteres) — envio simulado",
                canal, n.tipo(), n.cobrancaId(), mensagem.length());
        return true;
    }
}
