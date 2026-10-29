package br.undb.semear.repository.pessoa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.pessoa.Funcionario;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.repository.base.Repository;

public interface FuncionarioRepository extends Repository<Funcionario, UUID> {

    /** Busca por nome (parcial) ou CPF; tipo opcional. */
    List<Funcionario> pesquisar(String busca, TipoFuncionario tipo);

    /** Funcionário ligado a um login (usado para descobrir o professor logado). */
    Optional<UUID> buscarIdPorUsuario(UUID usuarioId);
}
