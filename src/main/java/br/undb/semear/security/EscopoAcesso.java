package br.undb.semear.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.repository.pessoa.FuncionarioRepository;

/**
 * Filtro "dono do dado" (docs/04 §8.4), aplicado nos services a partir do token, nunca de parâmetro da View.
 * Professor: só as turmas em que leciona e os alunos delas.
 */
@Component
public class EscopoAcesso {

    private final UsuarioLogado usuarioLogado;
    private final FuncionarioRepository funcionarios;

    public EscopoAcesso(UsuarioLogado usuarioLogado, FuncionarioRepository funcionarios) {
        this.usuarioLogado = usuarioLogado;
        this.funcionarios = funcionarios;
    }

    /** Se o usuário logado é professor, o id de funcionário dele (para restringir consultas); senão vazio. */
    public Optional<UUID> professorRestrito() {
        return usuarioLogado.usuario()
                .filter(u -> u.getPerfil() == PerfilUsuario.PROFESSOR)
                .map(u -> funcionarios.buscarIdPorUsuario(u.getId())
                        .orElseThrow(() -> new AcessoNegadoException("SEM_CADASTRO_FUNCIONARIO",
                                "Seu login não está ligado a um cadastro de professor")));
    }

    /** Cadastro de funcionário de quem está logado (quem analisa pré-matrículas, concede bolsas...). */
    public UUID funcionarioLogado() {
        return funcionarios.buscarIdPorUsuario(usuarioLogado.exigir().getId())
                .orElseThrow(() -> new AcessoNegadoException("SEM_CADASTRO_FUNCIONARIO",
                        "Seu login não está ligado a um cadastro de funcionário"));
    }

    public boolean ehPerfil(PerfilUsuario perfil) {
        return usuarioLogado.usuario().map(u -> u.getPerfil() == perfil).orElse(false);
    }
}
