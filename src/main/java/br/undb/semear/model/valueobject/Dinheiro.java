package br.undb.semear.model.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import br.undb.semear.model.exception.DadoInvalidoException;

/**
 * Valor monetário em reais, sempre com 2 casas decimais. Imutável.
 * Todo arredondamento do sistema passa por aqui (decisão pendente P02: HALF_EVEN provisório).
 */
public final class Dinheiro implements Comparable<Dinheiro> {

    // ARREDONDAMENTO precisa vir antes de ZERO: o construtor o usa durante a inicialização estática.
    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    private final BigDecimal valor;

    private Dinheiro(BigDecimal valor) {
        this.valor = valor.setScale(2, ARREDONDAMENTO);
    }

    public static Dinheiro de(BigDecimal valor) {
        if (valor == null) {
            throw new DadoInvalidoException("valor", "Valor monetário não informado");
        }
        return new Dinheiro(valor);
    }

    public static Dinheiro de(String valor) {
        try {
            return de(new BigDecimal(valor));
        } catch (NumberFormatException | NullPointerException e) {
            throw new DadoInvalidoException("valor", "Valor monetário inválido: " + valor);
        }
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(valor.subtract(outro.valor));
    }

    /** Ex.: percentual(new BigDecimal("10")) = 10% deste valor. */
    public Dinheiro percentual(BigDecimal percentual) {
        return new Dinheiro(valor.multiply(percentual).divide(CEM, 10, ARREDONDAMENTO));
    }

    public Dinheiro menor(Dinheiro outro) {
        return compareTo(outro) <= 0 ? this : outro;
    }

    public boolean ehZero() {
        return valor.signum() == 0;
    }

    public boolean ehNegativo() {
        return valor.signum() < 0;
    }

    public BigDecimal getValor() {
        return valor;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return valor.compareTo(outro.valor);
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Dinheiro dinheiro && valor.compareTo(dinheiro.valor) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor.stripTrailingZeros());
    }

    /** Formato brasileiro: "R$ 1.234,56". */
    @Override
    public String toString() {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(valor).replace(' ', ' ');
    }
}
