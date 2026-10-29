package br.undb.semear.repository.academico;

import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.repository.base.Repository;

public interface AnoLetivoRepository extends Repository<AnoLetivo, UUID> {

    Optional<AnoLetivo> buscarPorAno(int ano);

    /** fn_encerrar_ano_letivo: devolve quantas matrículas ATIVAS foram encerradas (idempotente). */
    int encerrar(UUID anoLetivoId);
}
