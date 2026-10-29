package br.undb.semear.model.entity.pessoa;

import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RegraNegocioException;

/**
 * Regras de quem administra os logins da escola:
 * - ninguém altera o próprio perfil nem se desativa;
 * - sempre sobra pelo menos um ADMIN ativo;
 * - o perfil precisa combinar com o cadastro ligado ao login (o banco confere de novo em fn_valida_perfil_usuario).
 */
public final class AdministracaoContas {

    /** A que cadastro o login está ligado. */
    public enum Vinculo {
        FUNCIONARIO,
        RESPONSAVEL,
        ALUNO,
        NENHUM
    }

    private final UUID quemAdministra;
    private final long adminsAtivos;

    public AdministracaoContas(UUID quemAdministra, long adminsAtivos) {
        this.quemAdministra = Objects.requireNonNull(quemAdministra, "quemAdministra");
        this.adminsAtivos = adminsAtivos;
    }

    /** Login novo para um funcionário: ativo, ainda sem acesso e com perfil da área da escola compatível. */
    public static void validarNovoAcesso(Funcionario funcionario, PerfilUsuario perfil) {
        if (!funcionario.isAtivo()) {
            throw new RegraNegocioException("FUNCIONARIO_INATIVO", "Reative o funcionário antes de liberar o acesso");
        }
        if (funcionario.possuiLogin()) {
            throw new ConflitoException("FUNCIONARIO_COM_LOGIN", "Este funcionário já tem acesso ao sistema");
        }
        exigirCompativel(perfil, Vinculo.FUNCIONARIO, funcionario.getTipo());
    }

    public void trocarPerfil(Usuario alvo, PerfilUsuario novo, Vinculo vinculo, TipoFuncionario tipo) {
        exigirOutro(alvo);
        exigirCompativel(novo, vinculo, tipo);
        if (alvo.getPerfil() == PerfilUsuario.ADMIN && novo != PerfilUsuario.ADMIN) {
            exigirOutroAdmin(alvo);
        }
        alvo.trocarPerfil(novo);
    }

    public void alterarAtivo(Usuario alvo, boolean ativo) {
        exigirOutro(alvo);
        if (!ativo && alvo.getPerfil() == PerfilUsuario.ADMIN) {
            exigirOutroAdmin(alvo);
        }
        if (ativo) {
            alvo.ativar();
        } else {
            alvo.desativar();
        }
    }

    /** Gerar senha provisória para outra pessoa (a própria se troca pela tela de senha). */
    public void exigirPodeRedefinirSenha(Usuario alvo) {
        exigirOutro(alvo);
    }

    static void exigirCompativel(PerfilUsuario perfil, Vinculo vinculo, TipoFuncionario tipo) {
        boolean ok = switch (vinculo) {
            case FUNCIONARIO -> !perfil.usaPortal() && (perfil != PerfilUsuario.PROFESSOR || tipo == TipoFuncionario.PROFESSOR);
            case RESPONSAVEL -> perfil == PerfilUsuario.RESPONSAVEL;
            case ALUNO -> perfil == PerfilUsuario.ALUNO;
            case NENHUM -> perfil == PerfilUsuario.RESPONSAVEL;
        };
        if (!ok) {
            String motivo = vinculo == Vinculo.FUNCIONARIO && perfil == PerfilUsuario.PROFESSOR
                    ? "O perfil Professor é só para funcionário do tipo professor (ele vê apenas as próprias turmas)"
                    : "Este perfil não combina com o cadastro ligado ao login";
            throw new RegraNegocioException("PERFIL_INCOMPATIVEL", motivo);
        }
    }

    private void exigirOutro(Usuario alvo) {
        if (alvo.getId().equals(quemAdministra)) {
            throw new RegraNegocioException("PROPRIO_ACESSO", "Você não pode alterar o seu próprio acesso");
        }
    }

    private void exigirOutroAdmin(Usuario alvo) {
        if (alvo.isAtivo() && adminsAtivos <= 1) {
            throw new RegraNegocioException("ULTIMO_ADMIN", "É preciso manter pelo menos um administrador ativo");
        }
    }
}
