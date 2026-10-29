package br.undb.semear.model.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.exception.DadoInvalidoException;

class DinheiroTest {

    @Test
    void sempreComDuasCasas() {
        assertThat(Dinheiro.de("375").getValor()).isEqualByComparingTo("375.00");
        assertThat(Dinheiro.de("375").getValor().scale()).isEqualTo(2);
    }

    @Test
    void operacoesBasicas() {
        Dinheiro mensalidade = Dinheiro.de("375.00");
        assertThat(mensalidade.somar(Dinheiro.de("50"))).isEqualTo(Dinheiro.de("425.00"));
        assertThat(mensalidade.subtrair(Dinheiro.de("75"))).isEqualTo(Dinheiro.de("300.00"));
        assertThat(Dinheiro.de("10").menor(Dinheiro.de("20"))).isEqualTo(Dinheiro.de("10"));
    }

    @Test
    void rn08_descontoDe10PorCentoDaMensalidadeDo1Ano() {
        Dinheiro mensalidade = Dinheiro.de("310.00");
        Dinheiro desconto = mensalidade.percentual(new BigDecimal("10"));
        assertThat(mensalidade.subtrair(desconto)).isEqualTo(Dinheiro.de("279.00"));
    }

    @Test
    void rn08_descontoDe10PorCentoDe375DaHalfEven337e50() {
        // Decisão pendente P02: a tabela da escola mostra R$ 337,00.
        Dinheiro mensalidade = Dinheiro.de("375.00");
        assertThat(mensalidade.subtrair(mensalidade.percentual(new BigDecimal("10")))).isEqualTo(Dinheiro.de("337.50"));
    }

    @Test
    void rn09_jurosDiariosArredondamUmaVezSo() {
        // 0,033% ao dia por 10 dias sobre R$ 375,00 = R$ 1,2375 -> R$ 1,24
        Dinheiro juros = Dinheiro.de("375.00").percentual(new BigDecimal("0.033").multiply(BigDecimal.TEN));
        assertThat(juros).isEqualTo(Dinheiro.de("1.24"));
    }

    @Test
    void formatoBrasileiro() {
        assertThat(Dinheiro.de("1234.5").toString()).isEqualTo("R$ 1.234,50");
    }

    @Test
    void rejeitaValorInvalido() {
        assertThatThrownBy(() -> Dinheiro.de("abc")).isInstanceOf(DadoInvalidoException.class);
    }
}
