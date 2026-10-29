package br.undb.semear.model.entity.pessoa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.entity.pessoa.AdministracaoContas.Vinculo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.SenhaTemporaria;

class AdministracaoContasTest {

    private final UUID eu = UUID.randomUUID();

    private Usuario usuario(UUID id, PerfilUsuario perfil, boolean ativo) {
        return new Usuario(id, new Email("pessoa@example.com"), perfil, ativo, null, List.of());
    }

    @Test
    void ninguemAlteraOProprioAcesso() {
        AdministracaoContas adm = new AdministracaoContas(eu, 2);
        Usuario proprio = usuario(eu, PerfilUsuario.ADMIN, true);
        assertThatThrownBy(() -> adm.alterarAtivo(proprio, false)).extracting("codigo").isEqualTo("PROPRIO_ACESSO");
        assertThatThrownBy(() -> adm.trocarPerfil(proprio, PerfilUsuario.SECRETARIA, Vinculo.FUNCIONARIO, TipoFuncionario.GESTOR))
                .extracting("codigo").isEqualTo("PROPRIO_ACESSO");
    }

    @Test
    void sempreSobraUmAdministradorAtivo() {
        Usuario outroAdmin = usuario(UUID.randomUUID(), PerfilUsuario.ADMIN, true);
        assertThatThrownBy(() -> new AdministracaoContas(eu, 1).alterarAtivo(outroAdmin, false))
                .extracting("codigo").isEqualTo("ULTIMO_ADMIN");
        new AdministracaoContas(eu, 2).alterarAtivo(outroAdmin, false);
        assertThat(outroAdmin.isAtivo()).isFalse();
    }

    @Test
    void perfilPrecisaCombinarComOCadastro() {
        AdministracaoContas adm = new AdministracaoContas(eu, 2);
        Usuario secretaria = usuario(UUID.randomUUID(), PerfilUsuario.SECRETARIA, true);
        // Professor só para funcionário do tipo professor (o escopo da turma depende disso).
        assertThatThrownBy(() -> adm.trocarPerfil(secretaria, PerfilUsuario.PROFESSOR, Vinculo.FUNCIONARIO, TipoFuncionario.ADMINISTRATIVO))
                .isInstanceOf(RegraNegocioException.class).extracting("codigo").isEqualTo("PERFIL_INCOMPATIVEL");
        // Funcionário não vira responsável; responsável não vira secretaria.
        assertThatThrownBy(() -> adm.trocarPerfil(secretaria, PerfilUsuario.RESPONSAVEL, Vinculo.FUNCIONARIO, TipoFuncionario.ADMINISTRATIVO))
                .isInstanceOf(RegraNegocioException.class);
        Usuario responsavel = usuario(UUID.randomUUID(), PerfilUsuario.RESPONSAVEL, true);
        assertThatThrownBy(() -> adm.trocarPerfil(responsavel, PerfilUsuario.SECRETARIA, Vinculo.RESPONSAVEL, null))
                .isInstanceOf(RegraNegocioException.class);

        adm.trocarPerfil(secretaria, PerfilUsuario.FINANCEIRO, Vinculo.FUNCIONARIO, TipoFuncionario.ADMINISTRATIVO);
        assertThat(secretaria.getPerfil()).isEqualTo(PerfilUsuario.FINANCEIRO);
    }

    @Test
    void senhaTemporariaForteESemCaracteresAmbiguos() {
        for (int i = 0; i < 200; i++) {
            String s = SenhaTemporaria.gerar();
            assertThat(s).hasSize(12).matches(".*[A-Za-z].*").matches(".*\\d.*").doesNotContainPattern("[0O1lI]");
        }
    }
}
