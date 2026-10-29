package br.undb.semear.model.entity.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.entity.pedagogico.ProfessoresDaTurma;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Dinheiro;

class CadastrosTest {

    private final Serie quintoAno = new Serie(UUID.randomUUID(), "5º Ano", NivelEnsino.ENSINO_FUNDAMENTAL, 9);

    @Test
    void reajusteDaTabelaDePrecos() {
        TabelaPreco p2027 = new TabelaPreco(quintoAno.getId(), 2027, Dinheiro.de("398.00"), Dinheiro.de("398.00"), 10, 7);
        TabelaPreco p2028 = p2027.reajustada(2028, new BigDecimal("6.5"));
        assertThat(p2028.getAno()).isEqualTo(2028);
        assertThat(p2028.getValorMensal()).isEqualTo(Dinheiro.de("423.87"));
        assertThat(p2028.getDiaVencimento()).isEqualTo(10);
    }

    @Test
    void validaPrecoEVencimento() {
        assertThatThrownBy(() -> new TabelaPreco(quintoAno.getId(), 2028, Dinheiro.ZERO, Dinheiro.ZERO, 10, 7))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> new TabelaPreco(quintoAno.getId(), 2028, Dinheiro.ZERO, Dinheiro.de("100"), 31, 7))
                .isInstanceOf(DadoInvalidoException.class);
        // Matrícula gratuita é permitida (o banco ativa direto).
        assertThat(new TabelaPreco(quintoAno.getId(), 2028, Dinheiro.ZERO, Dinheiro.de("100"), 5, 7).getValorMatricula().ehZero()).isTrue();
    }

    @Test
    void turmaNovaESoApagaSemAlunos() {
        Turma b = Turma.nova(quintoAno, 2028, " b ", 25);
        assertThat(b.getNome()).isEqualTo("B");
        assertThatThrownBy(() -> Turma.nova(quintoAno, 2028, "", 25)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> Turma.nova(quintoAno, 2028, "C", 31)).isInstanceOf(DadoInvalidoException.class);
        Turma comAlunos = new Turma(UUID.randomUUID(), quintoAno, 2026, "A", 30, 12);
        assertThatThrownBy(comAlunos::exigirRemovivel).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void anoLetivoDentroDoProprioAno() {
        assertThat(AnoLetivo.definir(null, 2028, LocalDate.of(2028, 2, 7), LocalDate.of(2028, 12, 15)).getAno()).isEqualTo(2028);
        assertThatThrownBy(() -> AnoLetivo.definir(null, 2028, LocalDate.of(2028, 2, 7), LocalDate.of(2029, 1, 10)))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void umRegenteEUmEspecialistaPorDisciplina() {
        UUID regente = UUID.randomUUID();
        UUID ingles = UUID.randomUUID();
        ProfessoresDaTurma turma = new ProfessoresDaTurma(List.of(new ProfessoresDaTurma.Atribuicao(regente, null),
                new ProfessoresDaTurma.Atribuicao(UUID.randomUUID(), ingles)));
        assertThatThrownBy(() -> turma.exigirPodeVincular(UUID.randomUUID(), null))
                .isInstanceOf(ConflitoException.class).extracting("codigo").isEqualTo("TURMA_COM_REGENTE");
        assertThatThrownBy(() -> turma.exigirPodeVincular(UUID.randomUUID(), ingles))
                .extracting("codigo").isEqualTo("DISCIPLINA_COM_PROFESSOR");
        turma.exigirPodeVincular(UUID.randomUUID(), UUID.randomUUID()); // outra disciplina: pode
    }
}
