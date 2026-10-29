package br.undb.semear.model.entity.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.StatusPreMatricula;
import br.undb.semear.model.enums.TipoFarda;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.exception.TurmaCheiaException;

class PreMatriculaTest {

    private static final Serie MATERNAL_1 = new Serie(UUID.randomUUID(), "Maternal 1", NivelEnsino.EDUCACAO_INFANTIL, 1);
    private static final Serie PRIMEIRO_ANO = new Serie(UUID.randomUUID(), "1º Ano", NivelEnsino.ENSINO_FUNDAMENTAL, 5);
    private static final DadosCrianca CRIANCA = new DadosCrianca("Helena Sousa Lima", LocalDate.of(2024, 6, 1), null);

    private PreMatricula pendente() {
        return PreMatricula.nova(2027, MATERNAL_1, UUID.randomUUID(), "Mãe", CRIANCA,
                Map.of(TipoFarda.UNIFORME, "2", TipoFarda.EDUCACAO_FISICA, "p"));
    }

    private PreMatricula decidida(StatusPreMatricula status) {
        return new PreMatricula(UUID.randomUUID(), 2027, MATERNAL_1, UUID.randomUUID(), "Mãe", CRIANCA,
                Map.of(), status, null, null, null);
    }

    @Test
    void rn05_novaPreMatriculaNasceEmAnalise() {
        assertThat(pendente().getStatus()).isEqualTo(StatusPreMatricula.PENDENTE);
    }

    @Test
    void rn06_fardasSeparadasEValidadas() {
        PreMatricula pre = pendente();
        assertThat(pre.getFardas()).containsEntry(TipoFarda.UNIFORME, "2").containsEntry(TipoFarda.EDUCACAO_FISICA, "P");
        assertThatThrownBy(() -> PreMatricula.nova(2027, MATERNAL_1, UUID.randomUUID(), "Mãe", CRIANCA,
                Map.of(TipoFarda.UNIFORME, "XXL")))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void rn04_aprovaSoEmTurmaDaSerieComVaga() {
        PreMatricula pre = pendente();
        pre.validarAprovacao(new Turma(UUID.randomUUID(), MATERNAL_1, 2027, "A", 15, 14));
        assertThatThrownBy(() -> pre.validarAprovacao(new Turma(UUID.randomUUID(), MATERNAL_1, 2027, "A", 15, 15)))
                .isInstanceOf(TurmaCheiaException.class);
        assertThatThrownBy(() -> pre.validarAprovacao(new Turma(UUID.randomUUID(), PRIMEIRO_ANO, 2027, "A", 30, 0)))
                .isInstanceOf(RegraNegocioException.class).extracting("codigo").isEqualTo("TURMA_INCOMPATIVEL");
        assertThatThrownBy(() -> pre.validarAprovacao(new Turma(UUID.randomUUID(), MATERNAL_1, 2026, "A", 15, 0)))
                .isInstanceOf(RegraNegocioException.class).extracting("codigo").isEqualTo("TURMA_INCOMPATIVEL");
    }

    @Test
    void rd04_decisaoEhDefinitiva() {
        Turma turma = new Turma(UUID.randomUUID(), MATERNAL_1, 2027, "A", 15, 0);
        assertThatThrownBy(() -> decidida(StatusPreMatricula.APROVADA).validarAprovacao(turma))
                .isInstanceOf(ConflitoException.class).extracting("codigo").isEqualTo("PRE_MATRICULA_JA_ANALISADA");
        assertThatThrownBy(() -> decidida(StatusPreMatricula.REJEITADA).validarRejeicao("Documentação incompleta"))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void rd04_rejeitarExigeMotivoExplicado() {
        assertThatThrownBy(() -> pendente().validarRejeicao("não")).isInstanceOf(DadoInvalidoException.class);
        assertThat(pendente().validarRejeicao("  Documentação incompleta  ")).isEqualTo("Documentação incompleta");
    }

    @Test
    void idadePrecisaCaberNaEscola() {
        LocalDate inicio2027 = LocalDate.of(2027, 2, 1);
        new DadosCrianca("Helena Sousa Lima", LocalDate.of(2024, 6, 1), null).validarIdadePara(inicio2027);
        assertThatThrownBy(() -> new DadosCrianca("Helena Sousa Lima", LocalDate.of(2026, 9, 1), null).validarIdadePara(inicio2027))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new DadosCrianca("Helena", LocalDate.of(2024, 6, 1), null))
                .isInstanceOf(DadoInvalidoException.class);
    }
}
