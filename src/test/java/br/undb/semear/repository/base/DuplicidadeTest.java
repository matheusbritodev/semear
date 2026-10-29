package br.undb.semear.repository.base;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import br.undb.semear.model.exception.SemearException;

class DuplicidadeTest {

    private final TradutorErroBanco tradutor = new TradutorErroBanco();

    private SemearException traduzir(String mensagem) {
        return tradutor.traduzir(new DuplicateKeyException("erro", new SQLException(mensagem, "23505")));
    }

    @Test
    void turmaRepetidaTemMensagemPropria() {
        SemearException e = traduzir("ERROR: duplicate key value violates unique constraint \"uq_turma\"");
        assertThat(e.getCodigo()).isEqualTo("TURMA_EXISTENTE");
        assertThat(e.getMessage()).contains("turma com esse nome");
    }

    @Test
    void outrasDuplicidadesContinuamGenericas() {
        assertThat(traduzir("ERROR: duplicate key value violates unique constraint \"uq_lista_material\"").getCodigo())
                .isEqualTo("DUPLICADO");
    }
}
