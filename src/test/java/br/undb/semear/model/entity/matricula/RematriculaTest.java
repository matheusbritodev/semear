package br.undb.semear.model.entity.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.entity.matricula.Rematricula.Fatos;
import br.undb.semear.model.entity.matricula.Rematricula.Situacao;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.exception.RegraNegocioException;

class RematriculaTest {

    private static final Serie SEGUNDO_ANO = new Serie(UUID.randomUUID(), "2º Ano", NivelEnsino.ENSINO_FUNDAMENTAL, 6);
    private static final Optional<StatusMatricula> ATIVA = Optional.of(StatusMatricula.ATIVA);

    private Turma turma(String nome, int ocupadas) {
        return new Turma(UUID.randomUUID(), SEGUNDO_ANO, 2027, nome, 30, ocupadas);
    }

    @Test
    void rf01_5_alunoAtivoAdimplenteVaiParaATurmaComMaisVagas() {
        Turma a = turma("A", 25);
        Turma b = turma("B", 10);
        var avaliacao = Rematricula.avaliar(new Fatos(ATIVA, false, false, true, List.of(a, b)));
        assertThat(avaliacao.situacao()).isEqualTo(Situacao.DISPONIVEL);
        assertThat(avaliacao.turmaSugerida()).contains(b);
    }

    @Test
    void rn13_inadimplenteNaoRematricula() {
        var avaliacao = Rematricula.avaliar(new Fatos(ATIVA, false, true, true, List.of(turma("A", 0))));
        assertThat(avaliacao.situacao()).isEqualTo(Situacao.PENDENCIA_FINANCEIRA);
        assertThatThrownBy(() -> Rematricula.exigirTurma(avaliacao))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("REMATRICULA_PENDENCIA_FINANCEIRA");
    }

    @Test
    void rn03_naoDuplicaMatriculaNoMesmoAno() {
        assertThat(Rematricula.avaliar(new Fatos(ATIVA, true, false, true, List.of())).situacao())
                .isEqualTo(Situacao.JA_REMATRICULADO);
    }

    @Test
    void quinto_anoConcluiAEscola() {
        assertThat(Rematricula.avaliar(new Fatos(ATIVA, false, false, false, List.of())).situacao())
                .isEqualTo(Situacao.CONCLUIU_ESCOLA);
    }

    @Test
    void rn04_semVagaNaSerieSeguinte() {
        assertThat(Rematricula.avaliar(new Fatos(ATIVA, false, false, true, List.of(turma("A", 30)))).situacao())
                .isEqualTo(Situacao.SEM_VAGAS);
    }

    @Test
    void semMatriculaAtivaOuSemTurmaAberta() {
        assertThat(Rematricula.avaliar(new Fatos(Optional.of(StatusMatricula.CANCELADA), false, false, true, List.of())).situacao())
                .isEqualTo(Situacao.SEM_MATRICULA_ATIVA);
        assertThat(Rematricula.avaliar(new Fatos(ATIVA, false, false, true, List.of())).situacao())
                .isEqualTo(Situacao.SEM_TURMA);
    }
}
