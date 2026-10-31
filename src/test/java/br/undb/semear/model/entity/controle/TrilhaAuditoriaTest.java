package br.undb.semear.model.entity.controle;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.undb.semear.model.entity.pessoa.AdministracaoContas.Vinculo;
import br.undb.semear.model.entity.pessoa.MatrizPermissoes;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Permissao;

/** SOX: cadeia de hashes da auditoria, campos alterados e revisão de acessos. */
class TrilhaAuditoriaTest {

    /** Monta uma cadeia válida, do mesmo jeito que o trigger fn_auditar. */
    private static List<RegistroAuditoria> cadeia(int tamanho) {
        List<RegistroAuditoria> lista = new ArrayList<>();
        String anterior = CadeiaAuditoria.HASH_INICIAL;
        for (int i = 1; i <= tamanho; i++) {
            RegistroAuditoria semHash = new RegistroAuditoria(i, "2026-10-31T13:00:0" + i + ".000000", null, "postgres",
                    "cobranca", "UPDATE", "id-" + i, "{\"status\": \"ABERTA\"}", "{\"status\": \"PAGA\"}", anterior, "");
            String hash = semHash.hashCalculado();
            lista.add(new RegistroAuditoria(i, semHash.ocorridoEmUtc(), null, "postgres", "cobranca", "UPDATE", "id-" + i,
                    semHash.dadosAntes(), semHash.dadosDepois(), anterior, hash));
            anterior = hash;
        }
        return lista;
    }

    private static CadeiaAuditoria.Resultado conferir(List<RegistroAuditoria> registros) {
        CadeiaAuditoria c = new CadeiaAuditoria();
        registros.forEach(c::conferir);
        return c.resultado();
    }

    @Test
    void cadeiaIntegra() {
        CadeiaAuditoria.Resultado r = conferir(cadeia(5));
        assertThat(r.integra()).isTrue();
        assertThat(r.verificados()).isEqualTo(5);
        assertThat(r.ultimoId()).isEqualTo(5);
    }

    @Test
    void conteudoAlteradoEhDetectado() {
        List<RegistroAuditoria> lista = new ArrayList<>(cadeia(4));
        RegistroAuditoria r = lista.get(2);
        lista.set(2, new RegistroAuditoria(r.id(), r.ocorridoEmUtc(), r.usuarioId(), r.papelBanco(), r.tabela(),
                r.operacao(), r.registroId(), r.dadosAntes(), "{\"status\": \"CANCELADA\"}", r.hashAnterior(), r.hash()));
        CadeiaAuditoria.Resultado res = conferir(lista);
        assertThat(res.integra()).isFalse();
        assertThat(res.falha().tipo()).isEqualTo("CONTEUDO_ALTERADO");
        assertThat(res.falha().registroId()).isEqualTo(3);
    }

    @Test
    void registroApagadoNoMeioEhDetectado() {
        List<RegistroAuditoria> lista = new ArrayList<>(cadeia(4));
        lista.remove(1);
        CadeiaAuditoria.Resultado res = conferir(lista);
        assertThat(res.falha().tipo()).isEqualTo("ELO_QUEBRADO");
        assertThat(res.falha().registroId()).isEqualTo(3);
    }

    @Test
    void fimCortadoEhDetectadoPelaAncora() {
        List<RegistroAuditoria> lista = cadeia(4);
        String hashDoQuarto = lista.get(3).hash();
        CadeiaAuditoria c = new CadeiaAuditoria();
        lista.subList(0, 3).forEach(c::conferir); // o 4º foi apagado
        assertThat(c.conferirAncora(4, hashDoQuarto, null)).isFalse();
        assertThat(c.resultado().falha().tipo()).isEqualTo("ANCORA_DIVERGENTE");
    }

    @Test
    void alteracoesMostramSoOQueMudou() {
        Map<String, Object> antes = new LinkedHashMap<>();
        antes.put("status", "ABERTA");
        antes.put("valor_final", 690.0);
        antes.put("atualizado_em", "ontem");
        Map<String, Object> depois = new LinkedHashMap<>(antes);
        depois.put("status", "PAGA");
        depois.put("atualizado_em", "hoje");
        assertThat(AlteracaoCampo.entre(antes, depois)).containsExactly(new AlteracaoCampo("status", "ABERTA", "PAGA"));
        assertThat(AlteracaoCampo.entre(null, depois)).hasSize(2).allMatch(a -> a.antes() == null);
    }

    @Test
    void revisaoApontaContasDeRisco() {
        LocalDate hoje = LocalDate.of(2026, 10, 31);
        MatrizPermissoes matriz = new MatrizPermissoes(Map.of(
                PerfilUsuario.SECRETARIA, List.of(new Permissao(Modulo.MATRICULA, true, true))));
        List<RevisaoAcesso.Conta> contas = List.of(
                new RevisaoAcesso.Conta(UUID.randomUUID(), "gestao@example.com", "Gestora", PerfilUsuario.ADMIN, true,
                        Vinculo.FUNCIONARIO, true, OffsetDateTime.parse("2026-10-30T09:00:00-03:00")),
                new RevisaoAcesso.Conta(UUID.randomUUID(), "saiu@example.com", "Ex-funcionária", PerfilUsuario.SECRETARIA,
                        true, Vinculo.FUNCIONARIO, false, OffsetDateTime.parse("2026-05-01T09:00:00-03:00")),
                new RevisaoAcesso.Conta(UUID.randomUUID(), "ok@example.com", "Secretária", PerfilUsuario.SECRETARIA,
                        true, Vinculo.FUNCIONARIO, true, OffsetDateTime.parse("2026-10-29T09:00:00-03:00")),
                new RevisaoAcesso.Conta(UUID.randomUUID(), "off@example.com", null, PerfilUsuario.SECRETARIA,
                        false, Vinculo.NENHUM, null, null));
        RevisaoAcesso revisao = new RevisaoAcesso(contas, matriz, hoje);

        assertThat(revisao.getContas().get(0).alertas()).extracting(RevisaoAcesso.Alerta::tipo)
                .containsExactly(RevisaoAcesso.TipoAlerta.PERFIL_PRIVILEGIADO);
        assertThat(revisao.getContas().get(1).alertas()).extracting(RevisaoAcesso.Alerta::tipo)
                .containsExactly(RevisaoAcesso.TipoAlerta.FUNCIONARIO_DESLIGADO, RevisaoAcesso.TipoAlerta.SEM_ACESSO_RECENTE);
        assertThat(revisao.getContas().get(2).alertas()).isEmpty();
        assertThat(revisao.getContas().get(3).alertas()).as("conta desativada não oferece risco").isEmpty();
        assertThat(revisao.contasComAlerta()).isEqualTo(2);
    }

    @Test
    void revisaoTrimestral() {
        LocalDate hoje = LocalDate.of(2026, 10, 31);
        assertThat(RevisaoAcesso.proximaRevisao(null, hoje)).isEqualTo(hoje);
        assertThat(RevisaoAcesso.proximaRevisao(LocalDate.of(2026, 10, 1), hoje)).isEqualTo(LocalDate.of(2026, 12, 30));
    }
}
