package br.undb.semear.service.financeiro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.undb.semear.integration.notificacao.NotificadorFactory;
import br.undb.semear.repository.financeiro.NotificacaoRepository;

/**
 * Tarefas automáticas do financeiro (docs/04 §9). Desligue com SEMEAR_JOBS_ATIVOS=false.
 * A rotina diária repete o pg_cron (é idempotente), para funcionar mesmo sem ele configurado.
 */
@Service
@EnableScheduling
@ConditionalOnProperty(prefix = "semear.jobs", name = "ativos", havingValue = "true", matchIfMissing = true)
public class AgendamentoFinanceiro {

    private static final Logger LOG = LoggerFactory.getLogger(AgendamentoFinanceiro.class);

    private final CobrancaService cobrancas;
    private final NotificacaoRepository notificacoes;
    private final NotificadorFactory notificadores;

    public AgendamentoFinanceiro(CobrancaService cobrancas, NotificacaoRepository notificacoes,
                                 NotificadorFactory notificadores) {
        this.cobrancas = cobrancas;
        this.notificacoes = notificacoes;
        this.notificadores = notificadores;
    }

    /** Boletos no Cora para cobranças em aberto ainda sem boleto. */
    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT15M")
    public void emitirBoletos() {
        executar("emissão de boletos", () -> {
            int emitidos = cobrancas.emitirBoletosPendentes(200);
            if (emitidos > 0) {
                LOG.info("{} boletos emitidos", emitidos);
            }
        });
    }

    /** Envia a fila de avisos (vencimento próximo e inadimplência). */
    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT10M")
    public void enviarNotificacoes() {
        executar("envio de notificações", () -> {
            int enviadas = notificacoes.processarPendentes(100, n -> notificadores.para(n.canal()).enviar(n));
            if (enviadas > 0) {
                LOG.info("{} notificações de cobrança enviadas", enviadas);
            }
        });
    }

    /** Dia 25, 05h: cobranças do mês seguinte (idempotente: RN01 não deixa duplicar). */
    @Scheduled(cron = "0 0 5 25 * *", zone = "America/Fortaleza")
    public void gerarCobrancasDoMesSeguinte() {
        executar("geração mensal de cobranças", () -> {
            var geracao = cobrancas.gerarMesSeguinte();
            if (geracao.criadas() > 0) {
                LOG.info("{} cobranças do mês seguinte geradas", geracao.criadas());
            }
        });
    }

    /** 06h de Fortaleza: marca vencidas, gera avisos e atualiza obrigações. */
    @Scheduled(cron = "0 0 6 * * *", zone = "America/Fortaleza")
    public void rotinaDiaria() {
        executar("rotina diária", notificacoes::rotinaDiaria);
    }

    /** Uma tarefa que falha não derruba as outras nem para o agendador. */
    private static void executar(String nome, Runnable tarefa) {
        try {
            tarefa.run();
        } catch (RuntimeException e) {
            LOG.error("Falha na tarefa agendada: {}", nome, e);
        }
    }
}
