package br.undb.semear.model.entity.pessoa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Permissao;

class MatrizPermissoesTest {

    private MatrizPermissoes matriz() {
        return new MatrizPermissoes(Map.of(
                PerfilUsuario.ADMIN, List.of(new Permissao(Modulo.CONFIGURACOES, true, true),
                        new Permissao(Modulo.USUARIOS, true, true)),
                PerfilUsuario.SECRETARIA, List.of(new Permissao(Modulo.PENDENCIAS, true, true))));
    }

    @Test
    void quemAlteraTambemConsulta() {
        Permissao resultado = matriz().alterar(PerfilUsuario.PROFESSOR, Modulo.PEDAGOGICO, false, true);
        assertThat(resultado.podeLer()).isTrue();
        assertThat(resultado.podeGravar()).isTrue();
    }

    @Test
    void gestorPodeDesligarPendenciasDaSecretaria() {
        MatrizPermissoes m = matriz();
        m.alterar(PerfilUsuario.SECRETARIA, Modulo.PENDENCIAS, false, false);
        Permissao celula = m.buscar(PerfilUsuario.SECRETARIA, Modulo.PENDENCIAS).orElseThrow();
        assertThat(celula.podeLer()).isFalse();
        assertThat(celula.podeGravar()).isFalse();
    }

    @Test
    void adminNaoPerdeConfiguracoesNemUsuarios() {
        MatrizPermissoes m = matriz();
        assertThatThrownBy(() -> m.alterar(PerfilUsuario.ADMIN, Modulo.CONFIGURACOES, true, false))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo").isEqualTo("PERMISSAO_ESSENCIAL");
        assertThatThrownBy(() -> m.alterar(PerfilUsuario.ADMIN, Modulo.USUARIOS, false, false))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void adminPodePerderOutrosModulos() {
        Permissao resultado = matriz().alterar(PerfilUsuario.ADMIN, Modulo.FINANCEIRO, true, false);
        assertThat(resultado.podeGravar()).isFalse();
    }

    @Test
    void perfilListaTodosOsModulosMesmoSemPermissao() {
        List<Permissao> doProfessor = matriz().doPerfil(PerfilUsuario.PROFESSOR);
        assertThat(doProfessor).hasSize(Modulo.values().length)
                .allMatch(p -> !p.podeLer() && !p.podeGravar());
    }
}
