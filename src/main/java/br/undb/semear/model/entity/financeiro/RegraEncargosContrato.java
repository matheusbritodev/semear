package br.undb.semear.model.entity.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import br.undb.semear.model.valueobject.CalendarioDiasUteis;

/**
 * RN09 (contrato de prestação de serviços): após o vencimento, multa de 2% e juros de 0,033% ao dia,
 * contados desde o vencimento. Sem acréscimo até o 5º dia útil do mês do vencimento.
 * Os percentuais são constantes até existir uma tabela de parâmetros (docs/03 §14).
 */
public final class RegraEncargosContrato implements RegraEncargos {

    static final BigDecimal MULTA = new BigDecimal("2");
    static final BigDecimal JUROS_AO_DIA = new BigDecimal("0.033");
    static final int DIA_UTIL_CARENCIA = 5;

    private final CalendarioDiasUteis calendario;

    public RegraEncargosContrato(CalendarioDiasUteis calendario) {
        this.calendario = Objects.requireNonNull(calendario, "calendario");
    }

    @Override
    public Encargos calcular(Cobranca cobranca, LocalDate data) {
        LocalDate vencimento = cobranca.getVencimento();
        LocalDate carencia = calendario.enesimoDiaUtil(YearMonth.from(vencimento), DIA_UTIL_CARENCIA);
        if (!data.isAfter(vencimento) || !data.isAfter(carencia)) {
            return Encargos.NENHUM;
        }
        long dias = ChronoUnit.DAYS.between(vencimento, data);
        return new Encargos(
                cobranca.getValorFinal().percentual(MULTA),
                cobranca.getValorFinal().percentual(JUROS_AO_DIA.multiply(BigDecimal.valueOf(dias))),
                dias);
    }
}
