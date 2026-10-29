package br.undb.semear.model.entity;

import java.util.Objects;
import java.util.UUID;

/**
 * Superclasse de toda entidade do domínio. A identidade é o id (uuid do banco).
 * Duas entidades são iguais quando têm o mesmo id; sem id, só é igual a si mesma.
 */
public abstract class Entidade {

    protected final UUID id;

    protected Entidade(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public boolean estaPersistida() {
        return id != null;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Entidade entidade
                && getClass() == entidade.getClass()
                && id != null
                && id.equals(entidade.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
