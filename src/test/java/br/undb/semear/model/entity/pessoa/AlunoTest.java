package br.undb.semear.model.entity.pessoa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.model.valueobject.Telefone;

class AlunoTest {

    private Aluno aluno;
    private Responsavel mae;
    private Responsavel pai;

    @BeforeEach
    void preparar() {
        aluno = new Aluno(UUID.randomUUID(), 1L, "Alice Costa Serra", LocalDate.of(2019, 5, 10), null, true);
        mae = new Responsavel(UUID.randomUUID(), "Ana Costa Serra", new Cpf("00000000191"),
                new Telefone("98999991234"), null, null);
        pai = new Responsavel(UUID.randomUUID(), "José Ribamar Serra", new Cpf("00000000272"),
                null, null, null);
        aluno.vincularResponsavel(mae, "Mãe", true);
        aluno.vincularResponsavel(pai, "Pai", false);
    }

    @Test
    void rn02_alunoTemVariosResponsaveis() {
        assertThat(aluno.getVinculos()).hasSize(2);
    }

    @Test
    void rd01_trocarResponsavelFinanceiroMantemSoUm() {
        aluno.definirResponsavelFinanceiro(pai.getId());
        assertThat(aluno.getResponsavelFinanceiro()).contains(pai);
        assertThat(aluno.getVinculos().stream().filter(VinculoResponsavel::financeiro)).hasSize(1);
    }

    @Test
    void rd01_vincularNovoFinanceiroDesmarcaOAnterior() {
        Responsavel avo = new Responsavel(UUID.randomUUID(), "Maria Serra", new Cpf("00000000353"), null, null, null);
        aluno.vincularResponsavel(avo, "Avó", true);
        assertThat(aluno.getResponsavelFinanceiro()).contains(avo);
        assertThat(aluno.getVinculos().stream().filter(VinculoResponsavel::financeiro)).hasSize(1);
    }

    @Test
    void rd01_financeiroPrecisaSerVinculado() {
        assertThatThrownBy(() -> aluno.definirResponsavelFinanceiro(UUID.randomUUID()))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("RESPONSAVEL_NAO_VINCULADO");
    }

    @Test
    void naoVinculaOMesmoResponsavelDuasVezes() {
        assertThatThrownBy(() -> aluno.vincularResponsavel(mae, "Mãe", false))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void idadeNaData() {
        assertThat(aluno.idadeEm(LocalDate.of(2026, 10, 28))).isEqualTo(7);
        assertThat(aluno.idadeEm(LocalDate.of(2026, 5, 9))).isEqualTo(6);
    }

    @Test
    void responsavelPrecisaDeAoMenosUmContato() {
        assertThatThrownBy(() -> mae.atualizarContato(null, null)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void nomeEhNormalizadoEValidado() {
        Aluno a = new Aluno(null, null, "  Theo   Lima  Sousa ", LocalDate.of(2020, 1, 1), null, true);
        assertThat(a.getNome()).isEqualTo("Theo Lima Sousa");
        assertThatThrownBy(() -> new Aluno(null, null, "", LocalDate.of(2020, 1, 1), null, true))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void toStringNaoExpoeDadosPessoais() {
        assertThat(aluno.toString()).doesNotContain("Alice").startsWith("Aluno(a)");
    }
}
