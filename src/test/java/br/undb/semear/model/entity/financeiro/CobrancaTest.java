package br.undb.semear.model.entity.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.CalendarioDiasUteis;
import br.undb.semear.model.valueobject.Dinheiro;

class CobrancaTest {

    /** Carnaval de 2026 (16 e 17/02) e Tiradentes como feriados, como no calendário do seed. */
    private final CobrancaFactory fabrica = new CobrancaFactory(new CalendarioDiasUteis(List.of(
            LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17), LocalDate.of(2026, 4, 21))));

    private Cobranca cobranca(TipoCobranca tipo, String valorFinal, LocalDate vencimento) {
        Dinheiro valor = Dinheiro.de(valorFinal);
        return fabrica.criar(new DadosCobranca(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), tipo,
                YearMonth.from(vencimento), valor, Dinheiro.ZERO, valor, vencimento, StatusCobranca.PENDENTE, null, null));
    }

    @Test
    void fabricaEscolheASubclassePeloTipo() {
        LocalDate venc = LocalDate.of(2026, 3, 10);
        assertThat(cobranca(TipoCobranca.MENSALIDADE, "375.00", venc)).isInstanceOf(Mensalidade.class);
        assertThat(cobranca(TipoCobranca.TAXA_ASSOCIADO, "125.00", venc)).isInstanceOf(TaxaAssociado.class);
        assertThat(cobranca(TipoCobranca.MATRICULA, "375.00", venc)).isInstanceOf(CobrancaMatricula.class);
    }

    @Test
    void rn08_mensalidadePagaAteOVencimentoTemDezPorCento() {
        Cobranca c = cobranca(TipoCobranca.MENSALIDADE, "310.00", LocalDate.of(2026, 3, 10));
        assertThat(c.valorAPagar(LocalDate.of(2026, 3, 10))).isEqualTo(Dinheiro.de("279.00"));
        assertThat(c.valorAPagar(LocalDate.of(2026, 3, 2))).isEqualTo(Dinheiro.de("279.00"));
    }

    @Test
    void rn08_taxaDeAssociadoNaoTemDesconto() {
        Cobranca c = cobranca(TipoCobranca.TAXA_ASSOCIADO, "125.00", LocalDate.of(2026, 3, 10));
        assertThat(c.valorAPagar(LocalDate.of(2026, 3, 5))).isEqualTo(Dinheiro.de("125.00"));
    }

    @Test
    void rn09_atrasoTemMultaDeDoisPorCentoMaisJurosAoDia() {
        // Mesmo caso do seed: R$ 375,00 vencida em 10/02/2026, paga em 21/02 (11 dias) = R$ 383,86.
        Cobranca c = cobranca(TipoCobranca.MENSALIDADE, "375.00", LocalDate.of(2026, 2, 10));
        ValorNaData valor = c.calcular(LocalDate.of(2026, 2, 21));
        assertThat(valor.encargos().multa()).isEqualTo(Dinheiro.de("7.50"));
        assertThat(valor.encargos().juros()).isEqualTo(Dinheiro.de("1.36"));
        assertThat(valor.encargos().diasDeAtraso()).isEqualTo(11);
        assertThat(valor.total()).isEqualTo(Dinheiro.de("383.86"));
    }

    @Test
    void rn09_semAcrescimoAteOQuintoDiaUtilDoMes() {
        // Vencimento no dia 2 de fevereiro: o 5º dia útil é 06/02. Pagar em 05/02 não tem multa.
        Cobranca c = cobranca(TipoCobranca.TAXA_ASSOCIADO, "125.00", LocalDate.of(2026, 2, 2));
        assertThat(c.valorAPagar(LocalDate.of(2026, 2, 5))).isEqualTo(Dinheiro.de("125.00"));
        assertThat(c.valorAPagar(LocalDate.of(2026, 2, 9))).isGreaterThan(Dinheiro.de("125.00"));
    }

    @Test
    void feriadosEmpurramOQuintoDiaUtil() {
        CalendarioDiasUteis calendario = new CalendarioDiasUteis(List.of(LocalDate.of(2026, 4, 21)));
        // Abril/2026: 1, 2, 3 (sex), 6, 7 → 5º útil = 07/04 (sem feriado no começo do mês).
        assertThat(calendario.enesimoDiaUtil(YearMonth.of(2026, 4), 5)).isEqualTo(LocalDate.of(2026, 4, 7));
        // Fevereiro/2026 com Carnaval: 2,3,4,5,6 → 06/02; o 11º útil pula 16 e 17 → 18/02... confere o salto.
        CalendarioDiasUteis carnaval = new CalendarioDiasUteis(List.of(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17)));
        assertThat(carnaval.enesimoDiaUtil(YearMonth.of(2026, 2), 11)).isEqualTo(LocalDate.of(2026, 2, 18));
    }

    @Test
    void cobrancaDeMatriculaNaoTemEncargos() {
        Cobranca c = cobranca(TipoCobranca.MATRICULA, "310.00", LocalDate.of(2026, 10, 1));
        assertThat(c.valorAPagar(LocalDate.of(2026, 10, 28))).isEqualTo(Dinheiro.de("310.00"));
    }

    @Test
    void statusVencidoMesmoAntesDaRotinaGravar() {
        Cobranca c = cobranca(TipoCobranca.MENSALIDADE, "375.00", LocalDate.of(2026, 10, 10));
        assertThat(c.statusEm(LocalDate.of(2026, 10, 10))).isEqualTo(StatusCobranca.PENDENTE);
        assertThat(c.statusEm(LocalDate.of(2026, 10, 28))).isEqualTo(StatusCobranca.VENCIDO);
        assertThat(c.diasDeAtraso(LocalDate.of(2026, 10, 28))).isEqualTo(18);
    }

    @Test
    void rd05_naoSePagaDuasVezes() {
        Cobranca c = cobranca(TipoCobranca.MENSALIDADE, "375.00", LocalDate.of(2026, 10, 10));
        Pagamento pagamento = new Pagamento(OffsetDateTime.parse("2026-10-09T10:00:00-03:00"), Dinheiro.de("337.50"),
                OrigemPagamento.PRESENCIAL);
        c.pagar(pagamento);
        assertThat(c.valorAPagar(LocalDate.of(2026, 12, 1))).isEqualTo(Dinheiro.de("337.50"));
        assertThatThrownBy(() -> c.pagar(pagamento)).isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("COBRANCA_JA_PAGA");
    }
}
