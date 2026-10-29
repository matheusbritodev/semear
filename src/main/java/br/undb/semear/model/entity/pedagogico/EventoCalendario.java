package br.undb.semear.model.entity.pedagogico;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.enums.TipoCalendario;
import br.undb.semear.model.exception.DadoInvalidoException;

/** Dia marcado no calendário letivo (RF08.3): feriado, recesso, evento ou dia letivo especial. */
public class EventoCalendario extends Entidade {

    private final int ano;
    private final LocalDate data;
    private final TipoCalendario tipo;
    private final String descricao;

    public EventoCalendario(UUID id, int ano, LocalDate data, TipoCalendario tipo, String descricao) {
        super(id);
        this.ano = ano;
        this.data = Objects.requireNonNull(data, "data");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.descricao = descricao;
    }

    /** Novo evento: a data precisa estar no ano letivo (o banco confere de novo) e ter descrição. */
    public static EventoCalendario novo(AnoLetivo ano, LocalDate data, TipoCalendario tipo, String descricao) {
        if (data == null || !ano.contem(data)) {
            throw new DadoInvalidoException("data", "A data precisa estar dentro do ano letivo " + ano.getAno());
        }
        String texto = descricao == null ? "" : descricao.trim();
        if (texto.isEmpty() || texto.length() > 200) {
            throw new DadoInvalidoException("descricao", "Descreva o dia (até 200 caracteres)");
        }
        return new EventoCalendario(null, ano.getAno(), data, tipo, texto);
    }

    public int getAno() {
        return ano;
    }

    public LocalDate getData() {
        return data;
    }

    public TipoCalendario getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }
}
