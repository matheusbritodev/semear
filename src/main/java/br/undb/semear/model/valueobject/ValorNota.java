package br.undb.semear.model.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

import br.undb.semear.model.exception.DadoInvalidoException;

/** Nota de 0 a 10 com até duas casas (mesmo limite da coluna nota.valor numeric(4,2)). Imutável. */
public final class ValorNota {

    private static final BigDecimal DEZ = BigDecimal.TEN;

    private final BigDecimal valor;

    public ValorNota(BigDecimal valor) {
        if (valor == null) {
            throw new DadoInvalidoException("valor", "Informe a nota");
        }
        BigDecimal normalizado = valor.stripTrailingZeros();
        if (valor.signum() < 0 || valor.compareTo(DEZ) > 0) {
            throw new DadoInvalidoException("valor", "A nota deve ser de 0 a 10");
        }
        if (normalizado.scale() > 2) {
            throw new DadoInvalidoException("valor", "Use no máximo duas casas decimais");
        }
        this.valor = valor.setScale(2);
    }

    public BigDecimal getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof ValorNota n && valor.compareTo(n.valor) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return valor.toPlainString();
    }
}
