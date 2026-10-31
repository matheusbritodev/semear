package br.undb.semear.model.entity.relatorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.StatusMatricula;

class RelatorioAlunosTest {

    private static RelatorioAlunos.Linha linha(String aluno, String turma, StatusMatricula s, int pendentes) {
        return new RelatorioAlunos.Linha(1, aluno, LocalDate.of(2020, 3, 10), 6, turma, s, "Responsável", pendentes);
    }

    private final RelatorioAlunos relatorio = new RelatorioAlunos(2026, List.of(
            linha("Bruno", "1º Ano A", StatusMatricula.ATIVA, 0),
            linha("Ana", "1º Ano A", StatusMatricula.AGUARDANDO_PAGAMENTO, 2),
            linha("Caio", "Infantil 2 A", StatusMatricula.CANCELADA, 3),
            linha("Davi", "Infantil 2 A", StatusMatricula.ATIVA, 1)));

    @Test
    void ordenaPorTurmaEDepoisPorNome() {
        assertThat(relatorio.getLinhas()).extracting(RelatorioAlunos.Linha::aluno).containsExactly("Ana", "Bruno", "Caio", "Davi");
    }

    @Test
    void totaisPorSituacaoContamTodasAsSituacoes() {
        assertThat(relatorio.porSituacao())
                .containsEntry(StatusMatricula.ATIVA, 2)
                .containsEntry(StatusMatricula.AGUARDANDO_PAGAMENTO, 1)
                .containsEntry(StatusMatricula.CANCELADA, 1)
                .containsEntry(StatusMatricula.ENCERRADA, 0);
    }

    @Test
    void vigentesEPendenciasIgnoramMatriculaCancelada() {
        assertThat(relatorio.vigentes()).isEqualTo(3);
        assertThat(relatorio.comDocumentosPendentes()).isEqualTo(2);
    }

    @Test
    void totaisPorTurma() {
        assertThat(relatorio.porTurma()).containsExactly(
                new RelatorioAlunos.TotalTurma("1º Ano A", 2, 2),
                new RelatorioAlunos.TotalTurma("Infantil 2 A", 2, 1));
    }
}
