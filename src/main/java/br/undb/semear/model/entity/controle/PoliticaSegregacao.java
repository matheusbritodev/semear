package br.undb.semear.model.entity.controle;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Permissao;

/**
 * Matriz de funções incompatíveis (SoD — segregação de funções). Um mesmo perfil não pode
 * GRAVAR nos dois módulos de uma regra, porque uma pessoa sozinha conseguiria fazer e esconder um desvio.
 * <p>
 * O perfil Administração (gestor) é a exceção documentada: ele precisa de tudo para administrar a escola.
 * O risco dele é coberto por controles compensatórios: dupla aprovação, trilha de auditoria e revisão de acessos.
 */
public final class PoliticaSegregacao {

    /** Funções que um mesmo perfil não pode acumular. */
    public record Regra(String codigo, Modulo moduloA, Modulo moduloB, String risco) {

        boolean violadaPor(Map<Modulo, Permissao> permissoes) {
            return grava(permissoes, moduloA) && grava(permissoes, moduloB);
        }

        private static boolean grava(Map<Modulo, Permissao> permissoes, Modulo modulo) {
            Permissao p = permissoes.get(modulo);
            return p != null && p.podeGravar();
        }
    }

    /** Regra violada por um perfil. Para o ADMIN, é exceção aceita (controle compensatório). */
    public record Conflito(PerfilUsuario perfil, Regra regra, boolean excecaoDocumentada) {
    }

    public static final List<Regra> REGRAS = List.of(
            new Regra("SOD-01", Modulo.USUARIOS, Modulo.FINANCEIRO,
                    "Quem cria e libera logins não deve registrar recebimentos: poderia criar um acesso só para dar baixa."),
            new Regra("SOD-02", Modulo.USUARIOS, Modulo.BOLSAS,
                    "Quem libera logins não deve pedir descontos: poderia criar um segundo acesso para aprovar o próprio pedido."),
            new Regra("SOD-03", Modulo.CONFIGURACOES, Modulo.FINANCEIRO,
                    "Quem define preços não deve receber pagamentos: poderia baixar o preço, receber o valor cheio e ficar com a diferença."),
            new Regra("SOD-04", Modulo.MATRICULA, Modulo.FINANCEIRO,
                    "Quem matricula e escolhe o responsável financeiro não deve dar baixa nas cobranças dessa família."),
            new Regra("SOD-05", Modulo.CONFIGURACOES, Modulo.APROVACOES,
                    "Quem altera preços e perfis de acesso não deve ser quem aprova alterações de preço."));

    /** Perfil com exceção documentada. */
    public static final PerfilUsuario PERFIL_PRIVILEGIADO = PerfilUsuario.ADMIN;

    private PoliticaSegregacao() {
    }

    public static List<Conflito> conflitos(PerfilUsuario perfil, Collection<Permissao> permissoes) {
        Map<Modulo, Permissao> porModulo = new EnumMap<>(Modulo.class);
        permissoes.forEach(p -> porModulo.put(p.modulo(), p));
        return REGRAS.stream()
                .filter(r -> r.violadaPor(porModulo))
                .map(r -> new Conflito(perfil, r, perfil == PERFIL_PRIVILEGIADO))
                .toList();
    }

    /** Bloqueia a mudança na matriz que deixaria o perfil com funções incompatíveis. */
    public static void exigirSemConflito(PerfilUsuario perfil, Collection<Permissao> permissoes) {
        conflitos(perfil, permissoes).stream().filter(c -> !c.excecaoDocumentada()).findFirst().ifPresent(c -> {
            throw new RegraNegocioException("CONFLITO_SOD", "Combinação não permitida (" + c.regra().codigo() + "): "
                    + c.regra().risco());
        });
    }
}
