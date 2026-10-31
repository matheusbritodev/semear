package br.undb.semear.model.entity.controle;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.pessoa.AdministracaoContas.Vinculo;
import br.undb.semear.model.entity.pessoa.MatrizPermissoes;
import br.undb.semear.model.enums.PerfilUsuario;

/**
 * Revisão periódica de acessos (SOX): a cada trimestre o gestor confere quem tem login na Área da escola
 * e se ainda precisa dele. Esta classe aponta o que merece atenção em cada conta.
 */
public final class RevisaoAcesso {

    public static final int PERIODICIDADE_DIAS = 90;
    public static final int DIAS_SEM_ACESSO = 90;
    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

    /** Conta da equipe como está hoje. funcionarioAtivo é nulo quando o login não tem cadastro de funcionário. */
    public record Conta(UUID usuarioId, String email, String nome, PerfilUsuario perfil, boolean ativo, Vinculo vinculo,
                        Boolean funcionarioAtivo, OffsetDateTime ultimoAcesso) {
    }

    public enum TipoAlerta {
        FUNCIONARIO_DESLIGADO("Funcionário desligado com login ativo: desative o acesso"),
        SEM_VINCULO("Login da equipe sem cadastro de funcionário: não dá para saber quem é"),
        CONFLITO_SOD("Perfil com funções incompatíveis"),
        NUNCA_ACESSOU("Nunca entrou no sistema: confirme se ainda precisa do acesso"),
        SEM_ACESSO_RECENTE("Sem entrar há mais de " + DIAS_SEM_ACESSO + " dias"),
        PERFIL_PRIVILEGIADO("Acesso total (Administração): confirme que a pessoa ainda precisa");

        private final String descricao;

        TipoAlerta(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public record Alerta(TipoAlerta tipo, String detalhe) {
    }

    public record ContaAvaliada(Conta conta, List<Alerta> alertas) {
    }

    private final List<ContaAvaliada> contas;

    public RevisaoAcesso(List<Conta> contas, MatrizPermissoes matriz, LocalDate hoje) {
        Objects.requireNonNull(matriz, "matriz");
        this.contas = contas.stream().map(c -> new ContaAvaliada(c, alertas(c, matriz, hoje))).toList();
    }

    private static List<Alerta> alertas(Conta c, MatrizPermissoes matriz, LocalDate hoje) {
        List<Alerta> alertas = new ArrayList<>();
        if (!c.ativo()) {
            return alertas; // conta desativada não oferece risco
        }
        if (c.vinculo() == Vinculo.FUNCIONARIO && Boolean.FALSE.equals(c.funcionarioAtivo())) {
            alertas.add(alerta(TipoAlerta.FUNCIONARIO_DESLIGADO, null));
        }
        if (c.vinculo() != Vinculo.FUNCIONARIO) {
            alertas.add(alerta(TipoAlerta.SEM_VINCULO, null));
        }
        PoliticaSegregacao.conflitos(c.perfil(), matriz.doPerfil(c.perfil())).stream()
                .filter(k -> !k.excecaoDocumentada())
                .forEach(k -> alertas.add(alerta(TipoAlerta.CONFLITO_SOD, k.regra().codigo() + ": " + k.regra().risco())));
        if (c.ultimoAcesso() == null) {
            alertas.add(alerta(TipoAlerta.NUNCA_ACESSOU, null));
        } else {
            LocalDate ultimo = c.ultimoAcesso().atZoneSameInstant(FUSO).toLocalDate();
            if (ultimo.plusDays(DIAS_SEM_ACESSO).isBefore(hoje)) {
                alertas.add(alerta(TipoAlerta.SEM_ACESSO_RECENTE, "Último acesso em " + ultimo));
            }
        }
        if (c.perfil() == PoliticaSegregacao.PERFIL_PRIVILEGIADO) {
            alertas.add(alerta(TipoAlerta.PERFIL_PRIVILEGIADO, null));
        }
        return List.copyOf(alertas);
    }

    private static Alerta alerta(TipoAlerta tipo, String detalhe) {
        return new Alerta(tipo, detalhe == null ? tipo.getDescricao() : detalhe);
    }

    /** Até quando a próxima revisão deve ser feita. Sem nenhuma revisão, já está vencida (hoje). */
    public static LocalDate proximaRevisao(LocalDate ultima, LocalDate hoje) {
        return ultima == null ? hoje : ultima.plusDays(PERIODICIDADE_DIAS);
    }

    public List<ContaAvaliada> getContas() {
        return contas;
    }

    public int totalAlertas() {
        return contas.stream().mapToInt(c -> c.alertas().size()).sum();
    }

    public long contasComAlerta() {
        return contas.stream().filter(c -> !c.alertas().isEmpty()).count();
    }
}
