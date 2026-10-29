package br.undb.semear.model.entity.pedagogico;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.enums.TipoCalendario;

/**
 * Calendário de um ano letivo: o período e os dias marcados. Responde se há aula numa data,
 * o que o diário usa para não aceitar chamada em feriado, recesso ou fim de semana.
 */
public final class CalendarioLetivo {

    private final AnoLetivo ano;
    private final List<EventoCalendario> eventos;

    public CalendarioLetivo(AnoLetivo ano, Collection<EventoCalendario> eventos) {
        this.ano = ano;
        this.eventos = List.copyOf(eventos);
    }

    /** Vazio se há aula; senão o motivo (ex.: "Feriado: Adesão do Maranhão"). */
    public Optional<String> motivoSemAula(LocalDate dia) {
        if (!ano.contem(dia)) {
            return Optional.of("Fora do ano letivo " + ano.getAno());
        }
        List<EventoCalendario> doDia = eventos.stream().filter(e -> e.getData().equals(dia)).toList();
        Optional<EventoCalendario> semAula = doDia.stream().filter(e -> !e.getTipo().temAula()).findFirst();
        if (semAula.isPresent()) {
            return Optional.of(semAula.get().getTipo().getRotulo() + ": " + semAula.get().getDescricao());
        }
        boolean diaLetivoEspecial = doDia.stream().anyMatch(e -> e.getTipo() == TipoCalendario.AULA);
        DayOfWeek semana = dia.getDayOfWeek();
        if (!diaLetivoEspecial && (semana == DayOfWeek.SATURDAY || semana == DayOfWeek.SUNDAY)) {
            return Optional.of("Fim de semana");
        }
        return Optional.empty();
    }

    public boolean temAula(LocalDate dia) {
        return motivoSemAula(dia).isEmpty();
    }

    public AnoLetivo getAno() {
        return ano;
    }

    public List<EventoCalendario> getEventos() {
        return eventos;
    }
}
