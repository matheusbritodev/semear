package br.undb.semear.model.entity.academico;

import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.NivelEnsino;

/** Série (Maternal 1 … 5º Ano). O nível de ensino define cobrança e contrato (RN01, RN07). */
public class Serie extends Entidade {

    private final String nome;
    private final NivelEnsino nivel;
    private final int ordem;

    public Serie(UUID id, String nome, NivelEnsino nivel, int ordem) {
        super(id);
        this.nome = Objects.requireNonNull(nome, "nome");
        this.nivel = Objects.requireNonNull(nivel, "nivel");
        this.ordem = ordem;
    }

    public String getNome() {
        return nome;
    }

    public NivelEnsino getNivel() {
        return nivel;
    }

    public int getOrdem() {
        return ordem;
    }
}
