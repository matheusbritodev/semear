package br.undb.semear.service.pessoa;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.pessoa.UsuarioDtos.ContaResponse;
import br.undb.semear.dto.pessoa.UsuarioDtos.FuncionarioSemLoginResponse;
import br.undb.semear.dto.pessoa.UsuarioDtos.SenhaTemporariaResponse;
import br.undb.semear.integration.ProvedorIdentidade;
import br.undb.semear.model.entity.pessoa.AdministracaoContas;
import br.undb.semear.model.entity.pessoa.Funcionario;
import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.SenhaTemporaria;
import br.undb.semear.repository.pessoa.UsuarioRepository;
import br.undb.semear.repository.pessoa.UsuarioRepository.ContaComVinculo;
import br.undb.semear.security.UsuarioLogado;

/**
 * Logins da escola (módulo USUARIOS): dar acesso a funcionários, trocar perfil, desativar e gerar senha
 * provisória. As regras ficam em AdministracaoContas; a conta em si vive no Supabase Auth (ProvedorIdentidade).
 */
@Service
public class ContaUsuarioService {

    private static final Logger LOG = LoggerFactory.getLogger(ContaUsuarioService.class);

    private final UsuarioRepository usuarios;
    private final FuncionarioService funcionarios;
    private final ProvedorIdentidade identidade;
    private final UsuarioLogado usuarioLogado;

    public ContaUsuarioService(UsuarioRepository usuarios, FuncionarioService funcionarios, ProvedorIdentidade identidade,
                               UsuarioLogado usuarioLogado) {
        this.usuarios = usuarios;
        this.funcionarios = funcionarios;
        this.identidade = identidade;
        this.usuarioLogado = usuarioLogado;
    }

    public List<ContaResponse> listar() {
        UUID eu = usuarioLogado.id();
        return usuarios.listarContas().stream().map(c -> resposta(c, eu)).toList();
    }

    public List<FuncionarioSemLoginResponse> funcionariosSemLogin() {
        return funcionarios.pesquisar(null, null).stream()
                .filter(f -> f.isAtivo() && !f.possuiLogin())
                .map(f -> new FuncionarioSemLoginResponse(f.getId(), f.getNome(), f.getTipo(),
                        f.getEmail().map(Email::getEndereco).orElse(null)))
                .toList();
    }

    /**
     * Cria o login no Supabase Auth com senha provisória, grava o usuário e liga ao funcionário.
     * Se o banco da escola recusar, a conta criada no Auth é apagada (nada fica pela metade).
     */
    @Transactional
    public SenhaTemporariaResponse criarParaFuncionario(UUID funcionarioId, String email, PerfilUsuario perfil) {
        Funcionario funcionario = funcionarios.buscar(funcionarioId);
        AdministracaoContas.validarNovoAcesso(funcionario, perfil);
        Email endereco = new Email(email);
        String senha = SenhaTemporaria.gerar();
        UUID id = identidade.criarContaConfirmada(endereco, senha);
        try {
            usuarios.salvar(new Usuario(id, endereco, perfil, true, null, List.of()));
            if (!usuarios.vincularFuncionario(funcionarioId, id)) {
                throw new ConflitoException("FUNCIONARIO_COM_LOGIN", "Este funcionário já tem acesso ao sistema");
            }
        } catch (RuntimeException e) {
            identidade.remover(id);
            throw e;
        }
        LOG.info("Acesso criado para o funcionário {} (perfil {})", funcionarioId, perfil);
        return new SenhaTemporariaResponse(conta(id), senha);
    }

    @Transactional
    public ContaResponse alterarPerfil(UUID id, PerfilUsuario perfil) {
        ContaComVinculo c = buscar(id);
        administracao().trocarPerfil(c.usuario(), perfil, c.vinculo(), c.tipoFuncionario());
        usuarios.salvar(c.usuario());
        LOG.info("Perfil do usuário {} alterado para {}", id, perfil);
        return conta(id);
    }

    @Transactional
    public ContaResponse alterarAtivo(UUID id, boolean ativo) {
        ContaComVinculo c = buscar(id);
        administracao().alterarAtivo(c.usuario(), ativo);
        usuarios.salvar(c.usuario());
        LOG.info("Usuário {} {}", id, ativo ? "reativado" : "desativado");
        return conta(id);
    }

    /** Destrava quem esqueceu a senha: sem servidor de e-mail, a escola repassa uma provisória. */
    public SenhaTemporariaResponse gerarSenhaTemporaria(UUID id) {
        ContaComVinculo c = buscar(id);
        administracao().exigirPodeRedefinirSenha(c.usuario());
        String senha = SenhaTemporaria.gerar();
        identidade.definirSenha(id, senha);
        LOG.info("Senha provisória gerada para o usuário {}", id);
        return new SenhaTemporariaResponse(resposta(c, usuarioLogado.id()), senha);
    }

    private AdministracaoContas administracao() {
        return new AdministracaoContas(usuarioLogado.id(), usuarios.contarAdminsAtivos());
    }

    private ContaComVinculo buscar(UUID id) {
        return usuarios.buscarConta(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário"));
    }

    private ContaResponse conta(UUID id) {
        return resposta(buscar(id), usuarioLogado.id());
    }

    private static ContaResponse resposta(ContaComVinculo c, UUID eu) {
        Usuario u = c.usuario();
        return new ContaResponse(u.getId(), u.getEmail().getEndereco(), u.getPerfil(), u.isAtivo(), u.getUltimoLogin(),
                c.criadoEm(), c.vinculo(), c.nome(), c.tipoFuncionario(), u.getId().equals(eu));
    }
}
