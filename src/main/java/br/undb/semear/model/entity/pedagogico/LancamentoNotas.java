package br.undb.semear.model.entity.pedagogico;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.ValorNota;

/**
 * Notas de uma disciplina num bimestre para os alunos de uma turma.
 * Só no Ensino Fundamental, só alunos ativos da turma e só por quem leciona a disciplina.
 * Nota vazia apaga a lançada (correção).
 */
public final class LancamentoNotas {

    private final UUID disciplinaId;
    private final int bimestre;
    private final Map<UUID, Optional<ValorNota>> notas;

    public LancamentoNotas(NivelEnsino nivel, ProfessoresDaTurma professores, UUID professorId, UUID disciplinaId,
                           int bimestre, Map<UUID, BigDecimal> informadas, Set<UUID> matriculasAtivas) {
        if (nivel != NivelEnsino.ENSINO_FUNDAMENTAL) {
            throw new RegraNegocioException("INFANTIL_SEM_NOTAS", "Na Educação Infantil não há lançamento de notas");
        }
        if (bimestre < 1 || bimestre > Boletim.BIMESTRES) {
            throw new DadoInvalidoException("bimestre", "Bimestre deve ser de 1 a 4");
        }
        if (!professores.podeLancarNotas(professorId, disciplinaId)) {
            throw new AcessoNegadoException("DISCIPLINA_DE_OUTRO_PROFESSOR", "Você não leciona esta disciplina nesta turma");
        }
        Map<UUID, Optional<ValorNota>> validadas = new LinkedHashMap<>();
        informadas.forEach((matricula, valor) -> {
            if (!matriculasAtivas.contains(matricula)) {
                throw new RegraNegocioException("ALUNO_FORA_DA_TURMA", "Há aluno que não está ativo nesta turma");
            }
            validadas.put(matricula, Optional.ofNullable(valor).map(ValorNota::new));
        });
        this.disciplinaId = disciplinaId;
        this.bimestre = bimestre;
        this.notas = Collections.unmodifiableMap(validadas);
    }

    public UUID getDisciplinaId() {
        return disciplinaId;
    }

    public int getBimestre() {
        return bimestre;
    }

    /** Matrícula → nota (vazio = apagar a lançada). */
    public Map<UUID, Optional<ValorNota>> getNotas() {
        return notas;
    }
}
