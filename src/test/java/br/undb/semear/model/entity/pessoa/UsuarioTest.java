package br.undb.semear.model.entity.pessoa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.enums.Acao;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Permissao;

class UsuarioTest {

    private Usuario secretaria(boolean ativo) {
        return new Usuario(UUID.randomUUID(), new Email("secretaria@teste.exemplo"), PerfilUsuario.SECRETARIA,
                ativo, null, List.of(
                        new Permissao(Modulo.MATRICULA, true, true),
                        new Permissao(Modulo.PEDAGOGICO, true, false)));
    }

    @Test
    void permissoesSeguemAMatrizDoPerfil() {
        Usuario usuario = secretaria(true);
        assertThat(usuario.pode(Modulo.MATRICULA, Acao.GRAVAR)).isTrue();
        assertThat(usuario.pode(Modulo.PEDAGOGICO, Acao.LER)).isTrue();
        assertThat(usuario.pode(Modulo.PEDAGOGICO, Acao.GRAVAR)).isFalse();
        assertThat(usuario.pode(Modulo.FINANCEIRO, Acao.LER)).isFalse();
    }

    @Test
    void usuarioInativoNaoPodeNada() {
        Usuario usuario = secretaria(true);
        usuario.desativar();
        assertThat(usuario.pode(Modulo.MATRICULA, Acao.LER)).isFalse();
    }

    @Test
    void quemGravaTambemLe() {
        assertThat(new Permissao(Modulo.PORTAL, false, true).permite(Acao.LER)).isTrue();
    }

    @Test
    void perfilDefineAArea() {
        assertThat(PerfilUsuario.RESPONSAVEL.usaPortal()).isTrue();
        assertThat(PerfilUsuario.PROFESSOR.usaPortal()).isFalse();
    }
}
