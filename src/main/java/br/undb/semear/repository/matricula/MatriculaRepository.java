package br.undb.semear.repository.matricula;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.CobrancaResumoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.ContratoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.FardaResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.MatriculaResumoResponse;
import br.undb.semear.model.entity.matricula.Matricula;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.repository.base.Repository;

/** Matrículas nascem pelas funções do banco; salvar() só grava cancelamento. */
public interface MatriculaRepository extends Repository<Matricula, UUID> {

    PaginaResponse<MatriculaResumoResponse> pesquisar(String busca, int ano, StatusMatricula status,
                                                      int pagina, int tamanho);

    Optional<MatriculaResumoResponse> buscarResumo(UUID id);

    /** Matrícula do aluno em um ano letivo, se houver. */
    Optional<MatriculaResumoResponse> buscarDoAlunoNoAno(UUID alunoId, int ano);

    List<ContratoResponse> listarContratos(UUID matriculaId);

    List<FardaResponse> listarFardas(UUID matriculaId);

    Optional<CobrancaResumoResponse> buscarCobrancaMatricula(UUID matriculaId);

    /** RN13: há cobrança vencida em aberto para o aluno (vw_inadimplencia)? */
    boolean possuiDebitosVencidos(UUID alunoId);

    Optional<String> nomeResponsavelFinanceiro(UUID alunoId);

    /** No cancelamento, as cobranças ainda não vencidas deixam de valer. */
    int cancelarCobrancasFuturas(UUID matriculaId, LocalDate hoje);
}
