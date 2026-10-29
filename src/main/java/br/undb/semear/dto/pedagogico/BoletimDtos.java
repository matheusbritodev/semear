package br.undb.semear.dto.pedagogico;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.enums.NivelEnsino;

/** Boletim no portal (RF10): notas por bimestre e frequência. */
public final class BoletimDtos {

    private BoletimDtos() {
    }

    /** {@code notas} tem sempre 4 posições (bimestre 1 a 4); null = ainda não lançada. */
    public record DisciplinaResponse(String disciplina, List<BigDecimal> notas, BigDecimal mediaParcial) {
    }

    public record FrequenciaResponse(int aulas, int presencas, int faltas, BigDecimal percentual) {
    }

    public record BoletimResponse(UUID alunoId, UUID matriculaId, int ano, String serie, String turma, NivelEnsino nivel,
                                  List<Integer> anosDisponiveis, int bimestresLancados, List<DisciplinaResponse> disciplinas,
                                  BigDecimal mediaGeral, FrequenciaResponse frequencia) {
    }
}
