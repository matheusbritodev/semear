package br.undb.semear.model.enums;

import java.math.BigDecimal;

import br.undb.semear.model.valueobject.Dinheiro;

/** Espelha o enum tipo_desconto do banco. Cada tipo sabe calcular o seu desconto (polimorfismo por enum). */
public enum TipoDesconto {

    PERCENTUAL("Percentual") {
        @Override
        public Dinheiro aplicar(Dinheiro valorBruto, BigDecimal valor) {
            return valorBruto.percentual(valor);
        }
    },
    VALOR_FIXO("Valor fixo") {
        @Override
        public Dinheiro aplicar(Dinheiro valorBruto, BigDecimal valor) {
            return Dinheiro.de(valor);
        }
    };

    private final String rotulo;

    TipoDesconto(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Desconto sobre o valor bruto, sem limite (o limite fica em Bolsa.descontoTotal). */
    public abstract Dinheiro aplicar(Dinheiro valorBruto, BigDecimal valor);
}
