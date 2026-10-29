package br.undb.semear.repository.base;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;

import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.exception.SemearException;

class TradutorErroBancoTest {

    private final TradutorErroBanco tradutor = new TradutorErroBanco();

    private SemearException traduzir(String mensagem, String sqlState) {
        return tradutor.traduzir(new UncategorizedSQLException("teste", "SQL", new SQLException(mensagem, sqlState)));
    }

    @Test
    void rn04_turmaLotadaVira409() {
        SemearException e = traduzir("ERROR: Turma \"A\" lotada: limite de 30 alunos atingido\n  Where: PL/pgSQL", "P0001");
        assertThat(e).isInstanceOf(ConflitoException.class);
        assertThat(e.getCodigo()).isEqualTo("TURMA_LOTADA");
        assertThat(e.getStatusHttp()).isEqualTo(409);
        assertThat(e.getMessage()).isEqualTo("Turma \"A\" lotada: limite de 30 alunos atingido");
    }

    @Test
    void rd05_cobrancaPagaImutavel() {
        assertThat(traduzir("ERROR: Cobrança paga não pode ser alterada", "P0001").getCodigo())
                .isEqualTo("COBRANCA_IMUTAVEL");
    }

    @Test
    void mensagemDesconhecidaDoBancoViraRegraGenerica() {
        SemearException e = traduzir("ERROR: alguma regra nova", "P0001");
        assertThat(e).isInstanceOf(RegraNegocioException.class);
        assertThat(e.getCodigo()).isEqualTo("REGRA_DO_BANCO");
    }

    @Test
    void violacaoDeUnicidadeViraDuplicado() {
        assertThat(traduzir("duplicate key value", "23505").getCodigo()).isEqualTo("DUPLICADO");
    }

    @Test
    void erroTecnicoVira500SemExporDetalhes() {
        SemearException e = traduzir("connection refused", "08001");
        assertThat(e.getStatusHttp()).isEqualTo(500);
        assertThat(e.getMessage()).doesNotContain("connection");
    }
}
