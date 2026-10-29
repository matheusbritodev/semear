package br.undb.semear.model.entity.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.TipoDesconto;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Dinheiro;

class BolsaTest {

    private Bolsa bolsa(TipoDesconto tipo, String valor) {
        return new Bolsa(UUID.randomUUID(), "Teste", null, tipo, new BigDecimal(valor), true);
    }

    @Test
    void rn11_mesmoCalculoDaFuncaoDoBanco() {
        // fn_desconto_bolsa: Bolsa Social 30% sobre R$ 375,00 = R$ 112,50; Convênio R$ 40,00 fixo.
        Dinheiro bruto = Dinheiro.de("375.00");
        assertThat(bolsa(TipoDesconto.PERCENTUAL, "30").calcularDesconto(bruto)).isEqualTo(Dinheiro.de("112.50"));
        assertThat(bolsa(TipoDesconto.VALOR_FIXO, "40").calcularDesconto(bruto)).isEqualTo(Dinheiro.de("40.00"));
        assertThat(Bolsa.descontoTotal(List.of(bolsa(TipoDesconto.PERCENTUAL, "10"), bolsa(TipoDesconto.VALOR_FIXO, "40")),
                bruto)).isEqualTo(Dinheiro.de("77.50"));
    }

    @Test
    void rd06_descontoNuncaPassaDoValorBruto() {
        Dinheiro bruto = Dinheiro.de("125.00");
        assertThat(Bolsa.descontoTotal(List.of(bolsa(TipoDesconto.PERCENTUAL, "100"), bolsa(TipoDesconto.VALOR_FIXO, "40")),
                bruto)).isEqualTo(bruto);
    }

    @Test
    void bolsaInativaNaoDaDesconto() {
        Bolsa inativa = new Bolsa(UUID.randomUUID(), "Antiga", null, TipoDesconto.PERCENTUAL, BigDecimal.TEN, false);
        assertThat(inativa.calcularDesconto(Dinheiro.de("375.00"))).isEqualTo(Dinheiro.ZERO);
    }

    @Test
    void percentualAcimaDeCemEhRecusado() {
        assertThatThrownBy(() -> bolsa(TipoDesconto.PERCENTUAL, "120")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> bolsa(TipoDesconto.VALOR_FIXO, "0")).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void revogarEncerraHojeOuApagaSeAindaNaoComecou() {
        LocalDate hoje = LocalDate.of(2026, 10, 28);
        ConcessaoBolsa vigente = new ConcessaoBolsa(null, UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 2, 1), null, null);
        assertThat(vigente.revogar(hoje)).isFalse();
        assertThat(vigente.getVigenciaFim()).isEqualTo(hoje);

        ConcessaoBolsa futura = new ConcessaoBolsa(null, UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2027, 2, 1), null, null);
        assertThat(futura.revogar(hoje)).isTrue();

        ConcessaoBolsa encerrada = new ConcessaoBolsa(null, UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 12, 31), null);
        assertThatThrownBy(() -> encerrada.revogar(hoje)).isInstanceOf(RegraNegocioException.class);
    }
}
