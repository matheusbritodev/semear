package br.undb.semear.model.entity.academico;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Dinheiro;

/**
 * Valores de uma série num ano letivo (tabela_preco). É de onde saem matrícula, Taxa de Associado e Mensalidade:
 * os valores nunca são digitados no contrato nem na cobrança. Mudar o preço vale só para cobranças novas.
 */
public final class TabelaPreco {

    private final UUID serieId;
    private final int ano;
    private final Dinheiro valorMatricula;
    private final Dinheiro valorMensal;
    private final int diaVencimento;
    private final int diasPrazoMatricula;

    public TabelaPreco(UUID serieId, int ano, Dinheiro valorMatricula, Dinheiro valorMensal, int diaVencimento,
                       int diasPrazoMatricula) {
        this.serieId = Objects.requireNonNull(serieId, "serieId");
        this.ano = ano;
        if (valorMatricula == null || valorMatricula.ehNegativo()) {
            throw new DadoInvalidoException("valorMatricula", "O valor da matrícula não pode ser negativo (zero = matrícula gratuita)");
        }
        if (valorMensal == null || valorMensal.ehZero() || valorMensal.ehNegativo()) {
            throw new DadoInvalidoException("valorMensal", "O valor mensal deve ser maior que zero");
        }
        if (diaVencimento < 1 || diaVencimento > 28) {
            throw new DadoInvalidoException("diaVencimento", "O vencimento deve ser entre o dia 1 e o dia 28 (vale para todos os meses)");
        }
        if (diasPrazoMatricula < 1 || diasPrazoMatricula > 60) {
            throw new DadoInvalidoException("diasPrazoMatricula", "O prazo para pagar a matrícula deve ser de 1 a 60 dias");
        }
        this.valorMatricula = valorMatricula;
        this.valorMensal = valorMensal;
        this.diaVencimento = diaVencimento;
        this.diasPrazoMatricula = diasPrazoMatricula;
    }

    /** Mesma série em outro ano, com reajuste percentual sobre os dois valores (ex.: 6,5). */
    public TabelaPreco reajustada(int novoAno, BigDecimal percentual) {
        if (percentual == null || percentual.compareTo(BigDecimal.valueOf(-50)) < 0 || percentual.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DadoInvalidoException("reajuste", "Reajuste entre -50% e 100%");
        }
        return new TabelaPreco(serieId, novoAno, valorMatricula.somar(valorMatricula.percentual(percentual)),
                valorMensal.somar(valorMensal.percentual(percentual)), diaVencimento, diasPrazoMatricula);
    }

    public UUID getSerieId() {
        return serieId;
    }

    public int getAno() {
        return ano;
    }

    public Dinheiro getValorMatricula() {
        return valorMatricula;
    }

    public Dinheiro getValorMensal() {
        return valorMensal;
    }

    public int getDiaVencimento() {
        return diaVencimento;
    }

    public int getDiasPrazoMatricula() {
        return diasPrazoMatricula;
    }
}
