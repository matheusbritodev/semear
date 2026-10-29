package br.undb.semear.dto.pessoa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoMatricula;
import jakarta.validation.constraints.NotNull;

/** Entrada e saída dos endpoints de alunos (/api/alunos). */
public final class AlunoDtos {

    private AlunoDtos() {
    }

    /** Linha da listagem: aluno com a matrícula do ano consultado. */
    public record AlunoResumoResponse(UUID id, Long numeroCadastro, String nome, LocalDate dataNascimento, int idade,
                                      boolean ativo, UUID matriculaId, UUID turmaId, String turma,
                                      StatusMatricula statusMatricula, TipoMatricula tipoMatricula,
                                      String responsavelFinanceiro, int documentosPendentes) {
    }

    public record AlunoDetalheResponse(UUID id, Long numeroCadastro, String nome, LocalDate dataNascimento, int idade,
                                       boolean ativo, List<VinculoResponse> responsaveis,
                                       List<MatriculaHistoricoResponse> matriculas) {
    }

    public record VinculoResponse(UUID responsavelId, String nome, String parentesco, boolean financeiro,
                                  String telefone, String email) {
    }

    public record MatriculaHistoricoResponse(UUID id, int ano, UUID turmaId, String turma, TipoMatricula tipo,
                                             StatusMatricula status, LocalDate dataMatricula) {
    }

    public record DefinirResponsavelFinanceiroRequest(@NotNull(message = "Escolha o responsável") UUID responsavelId) {
    }
}
