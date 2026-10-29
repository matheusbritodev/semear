package br.undb.semear.model.entity.academico;

import java.time.LocalDate;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;

/** Ano letivo com seu período de aulas. */
public class AnoLetivo extends Entidade {

    private final int ano;
    private final LocalDate dataInicio;
    private final LocalDate dataFim;

    public AnoLetivo(UUID id, int ano, LocalDate dataInicio, LocalDate dataFim) {
        super(id);
        if (ano < 2000 || ano > 2100) {
            throw new DadoInvalidoException("ano", "Ano letivo inválido");
        }
        if (dataInicio == null || dataFim == null || !dataFim.isAfter(dataInicio)) {
            throw new DadoInvalidoException("dataFim", "O fim do ano letivo deve ser depois do início");
        }
        this.ano = ano;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    /** Ano letivo novo ou ajustado: as datas precisam cair no próprio ano civil. */
    public static AnoLetivo definir(UUID id, int ano, LocalDate inicio, LocalDate fim) {
        AnoLetivo a = new AnoLetivo(id, ano, inicio, fim);
        if (inicio.getYear() != ano || fim.getYear() != ano) {
            throw new DadoInvalidoException("dataInicio", "As datas do ano letivo " + ano + " precisam estar dentro de " + ano);
        }
        return a;
    }

    public boolean contem(LocalDate data) {
        return !data.isBefore(dataInicio) && !data.isAfter(dataFim);
    }

    /** Encerrar (matrículas ATIVAS viram ENCERRADAS) só depois do último dia letivo. */
    public void exigirEncerravel(LocalDate hoje) {
        if (!hoje.isAfter(dataFim)) {
            throw new RegraNegocioException("ANO_LETIVO_EM_ANDAMENTO",
                    "O ano letivo " + ano + " só pode ser encerrado depois de " + String.format("%02d/%02d/%d",
                            dataFim.getDayOfMonth(), dataFim.getMonthValue(), dataFim.getYear()));
        }
    }

    public boolean emAndamento(LocalDate hoje) {
        return contem(hoje);
    }

    public int getAno() {
        return ano;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }
}
