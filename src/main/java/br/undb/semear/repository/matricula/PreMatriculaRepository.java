package br.undb.semear.repository.matricula;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.dto.matricula.MatriculaDtos.PreMatriculaResumoResponse;
import br.undb.semear.model.entity.matricula.PreMatricula;
import br.undb.semear.model.enums.StatusPreMatricula;
import br.undb.semear.repository.base.Repository;

/** salvar() só insere: decisões passam pelas funções do banco (FluxoMatriculaRepository). */
public interface PreMatriculaRepository extends Repository<PreMatricula, UUID> {

    /** Fila da secretaria; status e ano opcionais. Pendentes saem das mais antigas para as mais novas. */
    List<PreMatriculaResumoResponse> listar(StatusPreMatricula status, Integer ano);

    List<PreMatriculaResumoResponse> listarDoResponsavel(UUID responsavelId);

    Optional<PreMatriculaResumoResponse> buscarResumo(UUID id);
}
