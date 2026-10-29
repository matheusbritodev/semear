package br.undb.semear.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.undb.semear.controller.pessoa.FuncionarioController;
import br.undb.semear.model.entity.pessoa.DadosFuncionario;
import br.undb.semear.model.entity.pessoa.FuncionarioFactory;
import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Permissao;
import br.undb.semear.security.Permissoes;
import br.undb.semear.security.SecurityConfig;
import br.undb.semear.security.UsuarioLogado;
import br.undb.semear.service.pessoa.FuncionarioService;
import br.undb.semear.service.pessoa.UsuarioService;

/** A matriz perfil_permissao decide o acesso: RH é só do ADMIN. */
@WebMvcTest(controllers = FuncionarioController.class)
@Import({SecurityConfig.class, UsuarioLogado.class, Permissoes.class})
@TestPropertySource(properties = {
        "semear.supabase.url=https://exemplo.supabase.co",
        "semear.supabase.jwt-issuer=https://exemplo.supabase.co/auth/v1",
        "semear.supabase.jwt-audience=authenticated",
        "semear.cors.origens=http://localhost:5173"})
class PermissoesControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private FuncionarioService funcionarioService;

    private UUID logar(PerfilUsuario perfil, Permissao... permissoes) {
        UUID id = UUID.randomUUID();
        when(usuarioService.buscar(id)).thenReturn(Optional.of(
                new Usuario(id, new Email("teste@example.com"), perfil, true, null, List.of(permissoes))));
        return id;
    }

    @Test
    void professorNaoAcessaRH() throws Exception {
        UUID id = logar(PerfilUsuario.PROFESSOR, new Permissao(Modulo.PEDAGOGICO, true, true));
        mvc.perform(get("/api/funcionarios").with(jwt().jwt(j -> j.subject(id.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void adminListaFuncionariosComAFuncaoPolimorfica() throws Exception {
        UUID id = logar(PerfilUsuario.ADMIN, new Permissao(Modulo.RH, true, true));
        when(funcionarioService.pesquisar(any(), any())).thenReturn(List.of(
                FuncionarioFactory.criar(TipoFuncionario.PROFESSOR, new DadosFuncionario(UUID.randomUUID(),
                        "Márcia Regina Castro", new Cpf("00000000191"), null, null, LocalDate.of(2020, 2, 3), true, null))));

        mvc.perform(get("/api/funcionarios").with(jwt().jwt(j -> j.subject(id.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].funcao").value("Professor(a)"))
                .andExpect(jsonPath("$[0].podeLecionar").value(true))
                .andExpect(jsonPath("$[0].cpf").value("000.000.001-91"));
    }

    @Test
    void parametroInvalidoDa400() throws Exception {
        UUID id = logar(PerfilUsuario.ADMIN, new Permissao(Modulo.RH, true, true));
        mvc.perform(get("/api/funcionarios").param("tipo", "ASTRONAUTA").with(jwt().jwt(j -> j.subject(id.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"));
    }
}
