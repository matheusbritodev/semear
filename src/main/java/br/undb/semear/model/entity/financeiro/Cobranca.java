package br.undb.semear.model.entity.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Dinheiro;

/**
 * Cobrança do responsável financeiro (RF04). Valor bruto e desconto de bolsa vêm do banco (RN11/RD06);
 * o valor a pagar numa data (RN08/RN09) é calculado aqui, em ordem fixa (Template Method):
 * pago → valor pago; até o vencimento → desconto de pontualidade (se o tipo permitir);
 * depois → acréscimos da {@link RegraEncargos} (Strategy).
 */
public abstract class Cobranca extends Entidade implements Cobravel {

    /** RN08: 10% para quem paga até o vencimento. Incide sobre o valor já com bolsa (P03, provisório). */
    static final BigDecimal DESCONTO_PONTUALIDADE = BigDecimal.TEN;

    private final UUID matriculaId;
    private final UUID responsavelId;
    private final YearMonth competencia;
    private final Dinheiro valorBruto;
    private final Dinheiro descontoBolsa;
    private final Dinheiro valorFinal;
    private final LocalDate vencimento;
    private StatusCobranca status;
    private Pagamento pagamento;
    private ReferenciaCora cora;
    private final RegraEncargos regraEncargos;

    protected Cobranca(DadosCobranca dados, RegraEncargos regraEncargos) {
        super(dados.id());
        this.matriculaId = Objects.requireNonNull(dados.matriculaId(), "matriculaId");
        this.responsavelId = Objects.requireNonNull(dados.responsavelId(), "responsavelId");
        this.competencia = Objects.requireNonNull(dados.competencia(), "competencia");
        this.valorBruto = Objects.requireNonNull(dados.valorBruto(), "valorBruto");
        this.descontoBolsa = Objects.requireNonNull(dados.descontoBolsa(), "descontoBolsa");
        this.valorFinal = Objects.requireNonNull(dados.valorFinal(), "valorFinal");
        this.vencimento = Objects.requireNonNull(dados.vencimento(), "vencimento");
        this.status = Objects.requireNonNull(dados.status(), "status");
        this.pagamento = dados.pagamento();
        this.cora = dados.cora();
        this.regraEncargos = Objects.requireNonNull(regraEncargos, "regraEncargos");
        if (status == StatusCobranca.PAGO && pagamento == null) {
            throw new IllegalArgumentException("Cobrança paga sem dados do pagamento");
        }
    }

    public abstract TipoCobranca getTipo();

    /** Ponto de variação das subclasses: quem tem desconto de pontualidade (RN08). */
    protected abstract boolean permiteDescontoPontualidade();

    /** Template Method: a ordem do cálculo é fixa; subclasses e estratégia mudam só os passos. */
    public final ValorNaData calcular(LocalDate data) {
        if (status == StatusCobranca.PAGO) {
            return ValorNaData.jaPago(data, valorFinal, pagamento.valorPago());
        }
        if (status == StatusCobranca.CANCELADO) {
            return ValorNaData.calcular(data, Dinheiro.ZERO, Dinheiro.ZERO, Encargos.NENHUM);
        }
        if (!data.isAfter(vencimento) && permiteDescontoPontualidade()) {
            return ValorNaData.calcular(data, valorFinal, valorFinal.percentual(DESCONTO_PONTUALIDADE), Encargos.NENHUM);
        }
        return ValorNaData.calcular(data, valorFinal, Dinheiro.ZERO, regraEncargos.calcular(this, data));
    }

    @Override
    public final Dinheiro valorAPagar(LocalDate data) {
        return calcular(data).total();
    }

    /** Status real na data: pendente que passou do vencimento é VENCIDO, mesmo antes da rotina diária gravar. */
    public StatusCobranca statusEm(LocalDate hoje) {
        return status == StatusCobranca.PENDENTE && hoje.isAfter(vencimento) ? StatusCobranca.VENCIDO : status;
    }

    public boolean estaVencida(LocalDate hoje) {
        return statusEm(hoje) == StatusCobranca.VENCIDO;
    }

    public long diasDeAtraso(LocalDate hoje) {
        return estaVencida(hoje) ? ChronoUnit.DAYS.between(vencimento, hoje) : 0;
    }

    /** RD05 e regras da baixa: não se paga duas vezes nem se paga o que foi cancelado. */
    public void validarPagamento() {
        if (status == StatusCobranca.PAGO) {
            throw new RegraNegocioException("COBRANCA_JA_PAGA", "Esta cobrança já está paga");
        }
        if (status == StatusCobranca.CANCELADO) {
            throw new RegraNegocioException("COBRANCA_CANCELADA", "Cobrança cancelada não pode ser paga");
        }
    }

    /** Registra a baixa no objeto (o banco grava pela fn_registrar_pagamento). */
    public void pagar(Pagamento novo) {
        validarPagamento();
        this.pagamento = Objects.requireNonNull(novo, "pagamento");
        this.status = StatusCobranca.PAGO;
    }

    public boolean precisaEmitirBoleto() {
        return cora == null && status.emAberto();
    }

    public void vincularBoleto(ReferenciaCora boleto) {
        if (cora != null) {
            throw new RegraNegocioException("BOLETO_JA_EMITIDO", "Esta cobrança já tem boleto");
        }
        this.cora = Objects.requireNonNull(boleto, "boleto");
    }

    public UUID getMatriculaId() {
        return matriculaId;
    }

    public UUID getResponsavelId() {
        return responsavelId;
    }

    public YearMonth getCompetencia() {
        return competencia;
    }

    public Dinheiro getValorBruto() {
        return valorBruto;
    }

    public Dinheiro getDescontoBolsa() {
        return descontoBolsa;
    }

    public Dinheiro getValorFinal() {
        return valorFinal;
    }

    @Override
    public LocalDate getVencimento() {
        return vencimento;
    }

    public StatusCobranca getStatus() {
        return status;
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public ReferenciaCora getCora() {
        return cora;
    }
}
