package br.undb.semear.model.valueobject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Set;

/**
 * Dias úteis da escola: segunda a sexta, menos os feriados do calendário letivo (P06, provisório).
 * Usado na carência de juros e multa (RN09: sem acréscimo até o 5º dia útil do mês do vencimento).
 */
public final class CalendarioDiasUteis {

    private final Set<LocalDate> feriados;

    public CalendarioDiasUteis(Collection<LocalDate> feriados) {
        this.feriados = Set.copyOf(feriados);
    }

    public static CalendarioDiasUteis semFeriados() {
        return new CalendarioDiasUteis(Set.of());
    }

    public boolean ehDiaUtil(LocalDate dia) {
        DayOfWeek semana = dia.getDayOfWeek();
        return semana != DayOfWeek.SATURDAY && semana != DayOfWeek.SUNDAY && !feriados.contains(dia);
    }

    /** O n-ésimo dia útil do mês (n ≥ 1). */
    public LocalDate enesimoDiaUtil(YearMonth mes, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n deve ser pelo menos 1");
        }
        LocalDate dia = mes.atDay(1);
        int contados = 0;
        while (true) {
            if (ehDiaUtil(dia) && ++contados == n) {
                return dia;
            }
            dia = dia.plusDays(1);
            if (!YearMonth.from(dia).equals(mes)) {
                // Mês sem n dias úteis (não acontece com n = 5): usa o último dia do mês.
                return mes.atEndOfMonth();
            }
        }
    }
}
