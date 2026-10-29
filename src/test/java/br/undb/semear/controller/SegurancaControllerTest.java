package br.undb.semear.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import br.undb.semear.controller.pessoa.MeController;
import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Permissao;
import br.undb.semear.security.SecurityConfig;
import br.undb.semear.security.UsuarioLogado;
import br.undb.semear.service.SaudeService;
import br.undb.semear.service.pessoa.UsuarioService;

@WebMvcTest(controllers = {SaudeController.class, MeController.class})
@Import({SecurityConfig.class, UsuarioLogado.class})
@TestPropertySource(properties = {
        "semear.supabase.url=https://exemplo.supabase.co",
        "semear.supabase.jwt-issuer=https://exemplo.supabase.co/auth/v1",
        "semear.supabase.jwt-audience=authenticated",
        "semear.cors.origens=http://localhost:5173"})
class SegurancaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SaudeService saudeService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void saudeEhPublica() throws Exception {
        when(saudeService.bancoDisponivel()).thenReturn(true);
        mvc.perform(get("/api/saude"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void semTokenRecebe401NoFormatoPadrao() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void usuarioCadastradoRecebePerfilEPermissoes() throws Exception {
        UUID id = UUID.randomUUID();
        Usuario secretaria = new Usuario(id, new Email("secretaria@teste.exemplo"), PerfilUsuario.SECRETARIA,
                true, null, List.of(new Permissao(Modulo.MATRICULA, true, true)));
        when(usuarioService.buscar(id)).thenReturn(Optional.of(secretaria));
        when(usuarioService.nomeExibicao(id)).thenReturn(Optional.of("Secretaria Teste"));

        mvc.perform(get("/api/me").with(jwt().jwt(j -> j.subject(id.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("SECRETARIA"))
                .andExpect(jsonPath("$.nome").value("Secretaria Teste"))
                .andExpect(jsonPath("$.cadastroCompleto").value(true))
                .andExpect(jsonPath("$.permissoes[0].modulo").value("MATRICULA"));
    }

    @Test
    void loginSemCadastroIndicaPrimeiroAcesso() throws Exception {
        UUID id = UUID.randomUUID();
        when(usuarioService.buscar(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/me").with(jwt().jwt(j -> j.subject(id.toString()).claim("email", "mae@teste.exemplo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastroCompleto").value(false))
                .andExpect(jsonPath("$.email").value("mae@teste.exemplo"));
    }
}
