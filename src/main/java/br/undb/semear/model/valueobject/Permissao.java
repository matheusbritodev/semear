package br.undb.semear.model.valueobject;

import java.util.Objects;

import br.undb.semear.model.enums.Acao;
import br.undb.semear.model.enums.Modulo;

/** Uma linha da matriz perfil_permissao: o que o perfil pode fazer em um módulo. */
public record Permissao(Modulo modulo, boolean podeLer, boolean podeGravar) {

    public Permissao {
        Objects.requireNonNull(modulo, "modulo");
    }

    /** Quem grava também lê. */
    public boolean permite(Acao acao) {
        return switch (acao) {
            case LER -> podeLer || podeGravar;
            case GRAVAR -> podeGravar;
        };
    }
}
