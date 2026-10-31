package br.undb.semear.model.entity.controle;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.StatusSolicitacao;
import br.undb.semear.model.enums.TipoSolicitacao;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;

/**
 * Pedido que só vale depois que uma segunda pessoa aprova (maker-checker, SoD).
 * <p>
 * Template Method: {@link #aprovar} e {@link #rejeitar} são finais e aplicam as mesmas regras a todo tipo
 * (está pendente, quem decide pode aprovar, não é quem pediu, não é da família do aluno). Cada subclasse
 * diz o que está sendo pedido ({@link #dados()}, {@link #chave()}) e qual efeito aplicar ({@link #efetivar}).
 */
public abstract class SolicitacaoAprovacao extends Entidade {

    /** Situação de decisão, para reconstruir o pedido a partir do banco. */
    public record Decisao(StatusSolicitacao status, UUID decididaPor, OffsetDateTime decididaEm, String motivo) {

        public static final Decisao PENDENTE = new Decisao(StatusSolicitacao.PENDENTE, null, null, null);
    }

    private final TipoSolicitacao tipo;
    private final String resumo;
    private final UUID solicitadaPor;
    private final OffsetDateTime solicitadaEm;
    private Decisao decisao;

    protected SolicitacaoAprovacao(UUID id, TipoSolicitacao tipo, String resumo, UUID solicitadaPor,
                                   OffsetDateTime solicitadaEm, Decisao decisao) {
        super(id);
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        if (resumo == null || resumo.isBlank()) {
            throw new DadoInvalidoException("resumo", "Descreva o pedido");
        }
        this.resumo = resumo.length() > 300 ? resumo.substring(0, 300) : resumo;
        this.solicitadaPor = Objects.requireNonNull(solicitadaPor, "solicitadaPor");
        this.solicitadaEm = solicitadaEm;
        this.decisao = decisao == null ? Decisao.PENDENTE : decisao;
    }

    /* ---- Template Method ---- */

    /**
     * Aprova o pedido. Quem chama aplica o efeito em seguida ({@link #efetivar}), na mesma transação.
     *
     * @param cpfsDaFamilia CPFs dos responsáveis do aluno envolvido (vazio se o pedido não envolve aluno)
     */
    public final void aprovar(Decisor decisor, Collection<String> cpfsDaFamilia, OffsetDateTime agora) {
        exigirDecidivel(decisor, cpfsDaFamilia);
        decisao = new Decisao(StatusSolicitacao.APROVADA, decisor.usuarioId(), agora, null);
    }

    public final void rejeitar(Decisor decisor, Collection<String> cpfsDaFamilia, String motivo, OffsetDateTime agora) {
        if (motivo == null || motivo.isBlank()) {
            throw new DadoInvalidoException("motivo", "Explique por que o pedido foi recusado");
        }
        exigirDecidivel(decisor, cpfsDaFamilia);
        decisao = new Decisao(StatusSolicitacao.REJEITADA, decisor.usuarioId(), agora, motivo.trim());
    }

    /** Só quem pediu desiste do próprio pedido, enquanto ninguém decidiu. */
    public final void cancelar(UUID usuarioId, OffsetDateTime agora) {
        exigirPendente();
        if (!solicitadaPor.equals(usuarioId)) {
            throw new AcessoNegadoException("SO_O_SOLICITANTE", "Só quem fez o pedido pode cancelá-lo");
        }
        decisao = new Decisao(StatusSolicitacao.CANCELADA, usuarioId, agora, null);
    }

    private void exigirDecidivel(Decisor decisor, Collection<String> cpfsDaFamilia) {
        exigirPendente();
        if (!decisor.podeAprovar()) {
            throw new AcessoNegadoException("SEM_PERMISSAO_APROVAR", "Seu perfil não pode decidir pedidos de aprovação");
        }
        if (decisor.usuarioId().equals(solicitadaPor)) {
            throw new AcessoNegadoException("AUTOAPROVACAO",
                    "Quem fez o pedido não pode decidir sobre ele: outra pessoa precisa aprovar (segregação de funções)");
        }
        ConflitoInteresse.exigirAusente(decisor.cpf(), cpfsDaFamilia, "a decisão sobre este pedido");
    }

    private void exigirPendente() {
        if (decisao.status() != StatusSolicitacao.PENDENTE) {
            String situacao = switch (decisao.status()) {
                case APROVADA -> "aprovado";
                case REJEITADA -> "recusado";
                default -> "cancelado";
            };
            throw new ConflitoException("SOLICITACAO_JA_DECIDIDA", "Este pedido já foi " + situacao);
        }
    }

    /* ---- Ganchos das subclasses ---- */

    /** Aplica o efeito do pedido aprovado. */
    public abstract void efetivar(EfeitosAprovacao efeitos);

    /** O que foi pedido, em texto, para guardar em jsonb e reconstruir depois (SolicitacaoFactory). */
    public abstract Map<String, String> dados();

    /** Identifica o alvo do pedido: não pode haver dois pedidos pendentes iguais. */
    public abstract String chave();

    /** Matrícula envolvida, para o conflito de interesse e para exibir. */
    public Optional<UUID> matriculaEnvolvida() {
        return Optional.empty();
    }

    public boolean pendente() {
        return decisao.status() == StatusSolicitacao.PENDENTE;
    }

    public TipoSolicitacao getTipo() {
        return tipo;
    }

    public String getResumo() {
        return resumo;
    }

    public UUID getSolicitadaPor() {
        return solicitadaPor;
    }

    public OffsetDateTime getSolicitadaEm() {
        return solicitadaEm;
    }

    public Decisao getDecisao() {
        return decisao;
    }
}
