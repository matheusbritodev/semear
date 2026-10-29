package br.undb.semear.repository.base;

import java.util.List;
import java.util.Optional;

import br.undb.semear.model.entity.Entidade;

/**
 * Contrato genérico de persistência. Os services dependem destas interfaces,
 * nunca das classes JDBC: dá para trocar a implementação (ex.: em memória nos testes).
 */
public interface Repository<T extends Entidade, ID> {

    Optional<T> buscarPorId(ID id);

    List<T> listar();

    /** Insere se ainda não existir; senão, atualiza. Devolve a entidade como ficou no banco. */
    T salvar(T entidade);

    void remover(ID id);
}
