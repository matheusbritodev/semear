package br.undb.semear.model.entity.pessoa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Cpf;

class FuncionarioTest {

    private DadosFuncionario dados(LocalDate admissao) {
        return new DadosFuncionario(UUID.randomUUID(), "Márcia Regina Castro", new Cpf("00000000191"),
                null, null, admissao, true, null);
    }

    @ParameterizedTest
    @EnumSource(TipoFuncionario.class)
    void fabricaCriaASubclasseDoTipo(TipoFuncionario tipo) {
        Funcionario f = FuncionarioFactory.criar(tipo, dados(LocalDate.of(2020, 2, 3)));
        assertThat(f.getTipo()).isEqualTo(tipo);
        assertThat(f.getFuncao()).isNotBlank();
    }

    @Test
    void polimorfismo_soProfessorPodeLecionar() {
        assertThat(FuncionarioFactory.criar(TipoFuncionario.PROFESSOR, dados(null)))
                .isInstanceOf(Professor.class)
                .extracting(Funcionario::podeLecionar).isEqualTo(true);
        assertThat(FuncionarioFactory.criar(TipoFuncionario.GESTOR, dados(null)).podeLecionar()).isFalse();
        assertThat(FuncionarioFactory.criar(TipoFuncionario.PRESTADOR_SERVICO, dados(null)).getFuncao())
                .isEqualTo("Prestador(a) de serviço");
    }

    @Test
    void descreverPapelUsaAFuncao() {
        Pessoa pessoa = FuncionarioFactory.criar(TipoFuncionario.PROFESSOR, dados(null));
        assertThat(pessoa.descreverPapel()).isEqualTo("Professor(a)");
    }

    @Test
    void cpfObrigatorioEAdmissaoNaoFutura() {
        assertThatThrownBy(() -> FuncionarioFactory.criar(TipoFuncionario.GESTOR,
                new DadosFuncionario(null, "Lúcia Campos", null, null, null, null, true, null)))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> FuncionarioFactory.criar(TipoFuncionario.GESTOR, dados(LocalDate.now().plusDays(1))))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void desativarEAtivar() {
        Funcionario f = FuncionarioFactory.criar(TipoFuncionario.ADMINISTRATIVO, dados(null));
        f.desativar();
        assertThat(f.isAtivo()).isFalse();
        f.ativar();
        assertThat(f.isAtivo()).isTrue();
    }
}
