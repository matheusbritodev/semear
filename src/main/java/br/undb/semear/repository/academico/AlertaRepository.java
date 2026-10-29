package br.undb.semear.repository.academico;

import java.util.List;
import java.util.UUID;

import br.undb.semear.dto.academico.AcademicoDtos.AlertaResponse;

/** Avisos à secretaria (tabela alerta_secretaria), criados pelo banco quando uma turma lota (RF03). */
public interface AlertaRepository {

    List<AlertaResponse> listar(boolean apenasNaoLidos);

    /** Devolve false se o alerta não existir. */
    boolean marcarComoLido(UUID id);
}
