package br.undb.semear.model.entity.pedagogico;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.exception.DadoInvalidoException;

/** Curso de formação continuada de um funcionário (RF08.4). Sem data de conclusão = em andamento. */
public class FormacaoContinuada extends Entidade {

    private final UUID funcionarioId;
    private final String curso;
    private final int cargaHoraria;
    private final LocalDate concluidaEm;

    public FormacaoContinuada(UUID id, UUID funcionarioId, String curso, int cargaHoraria, LocalDate concluidaEm) {
        super(id);
        this.funcionarioId = Objects.requireNonNull(funcionarioId, "funcionarioId");
        this.curso = curso;
        this.cargaHoraria = cargaHoraria;
        this.concluidaEm = concluidaEm;
    }

    public static FormacaoContinuada nova(UUID funcionarioId, String curso, int cargaHoraria, LocalDate concluidaEm,
                                          LocalDate hoje) {
        String nome = curso == null ? "" : curso.trim();
        if (nome.isEmpty() || nome.length() > 200) {
            throw new DadoInvalidoException("curso", "Informe o curso (até 200 caracteres)");
        }
        if (cargaHoraria < 1 || cargaHoraria > 2000) {
            throw new DadoInvalidoException("cargaHoraria", "Carga horária de 1 a 2.000 horas");
        }
        if (concluidaEm != null && concluidaEm.isAfter(hoje)) {
            throw new DadoInvalidoException("concluidaEm", "A conclusão não pode ser no futuro");
        }
        return new FormacaoContinuada(null, funcionarioId, nome, cargaHoraria, concluidaEm);
    }

    public boolean concluida() {
        return concluidaEm != null;
    }

    public UUID getFuncionarioId() {
        return funcionarioId;
    }

    public String getCurso() {
        return curso;
    }

    public int getCargaHoraria() {
        return cargaHoraria;
    }

    public LocalDate getConcluidaEm() {
        return concluidaEm;
    }
}
