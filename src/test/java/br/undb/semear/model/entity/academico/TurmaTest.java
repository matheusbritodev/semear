package br.undb.semear.model.entity.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.enums.TipoContrato;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.exception.TurmaCheiaException;

class TurmaTest {

    private static final Serie PRIMEIRO_ANO = new Serie(UUID.randomUUID(), "1º Ano", NivelEnsino.ENSINO_FUNDAMENTAL, 5);

    private Turma turma(int capacidade, int ocupadas) {
        return new Turma(UUID.randomUUID(), PRIMEIRO_ANO, 2026, "A", capacidade, ocupadas);
    }

    @Test
    void rn04_naoMatriculaAcimaDe30() {
        Turma lotada = turma(30, 30);
        assertThat(lotada.temVaga()).isFalse();
        assertThatThrownBy(lotada::garantirVaga)
                .isInstanceOf(TurmaCheiaException.class)
                .extracting("codigo").isEqualTo("TURMA_LOTADA");
    }

    @Test
    void rn04_turmaComVagaAceitaMatricula() {
        Turma turma = turma(30, 29);
        turma.garantirVaga();
        assertThat(turma.vagasRestantes()).isEqualTo(1);
        assertThat(turma.percentualOcupacao()).isEqualTo(97);
    }

    @Test
    void rn04_capacidadeEntre1e30() {
        assertThatThrownBy(() -> turma(31, 0)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> turma(20, 0).alterarCapacidade(0)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void rn04_capacidadeNaoFicaAbaixoDaOcupacao() {
        Turma turma = turma(30, 22);
        assertThatThrownBy(() -> turma.alterarCapacidade(20))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("CAPACIDADE_ABAIXO_OCUPACAO");
        turma.alterarCapacidade(22);
        assertThat(turma.estaLotada()).isTrue();
    }

    @Test
    void nomeCompletoJuntaSerieETurma() {
        assertThat(turma(30, 0).getNomeCompleto()).isEqualTo("1º Ano A");
    }

    @Test
    void rn01_rn07_nivelDefineCobrancaEContrato() {
        assertThat(NivelEnsino.EDUCACAO_INFANTIL.tipoCobrancaMensal()).isEqualTo(TipoCobranca.TAXA_ASSOCIADO);
        assertThat(NivelEnsino.EDUCACAO_INFANTIL.tipoContrato()).isEqualTo(TipoContrato.FICHA_SOCIO);
        assertThat(NivelEnsino.ENSINO_FUNDAMENTAL.tipoCobrancaMensal()).isEqualTo(TipoCobranca.MENSALIDADE);
        assertThat(NivelEnsino.ENSINO_FUNDAMENTAL.tipoContrato()).isEqualTo(TipoContrato.CONTRATO_MATRICULA);
    }
}
