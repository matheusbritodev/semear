package br.undb.semear.model.entity.pessoa;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.undb.semear.model.entity.controle.PoliticaSegregacao;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Permissao;

/**
 * Matriz perfil × módulo (tabela perfil_permissao), editável pelo gestor.
 * Concentra as regras da edição:
 * - quem altera também consulta (gravar implica ler);
 * - o ADMIN nunca perde Configurações e Usuários (senão ninguém mais conseguiria administrar o sistema).
 */
public final class MatrizPermissoes {

    /** Permissões que o ADMIN mantém sempre, para não se trancar fora do sistema. */
    private static final List<Modulo> ESSENCIAIS_DO_ADMIN = List.of(Modulo.CONFIGURACOES, Modulo.USUARIOS);

    private final Map<PerfilUsuario, Map<Modulo, Permissao>> matriz = new EnumMap<>(PerfilUsuario.class);

    public MatrizPermissoes(Map<PerfilUsuario, List<Permissao>> linhas) {
        Arrays.stream(PerfilUsuario.values()).forEach(p -> matriz.put(p, new EnumMap<>(Modulo.class)));
        linhas.forEach((perfil, permissoes) -> permissoes.forEach(p -> matriz.get(perfil).put(p.modulo(), p)));
    }

    /** Altera uma célula da matriz e devolve a permissão resultante (já normalizada). */
    public Permissao alterar(PerfilUsuario perfil, Modulo modulo, boolean ler, boolean gravar) {
        Permissao nova = new Permissao(modulo, ler || gravar, gravar);
        if (perfil == PerfilUsuario.ADMIN && ESSENCIAIS_DO_ADMIN.contains(modulo) && !nova.podeGravar()) {
            throw new RegraNegocioException("PERMISSAO_ESSENCIAL",
                    "O perfil Administração precisa manter Configurações e Usuários para continuar gerenciando o sistema");
        }
        if (modulo == Modulo.AUDITORIA && nova.podeGravar()) {
            throw new RegraNegocioException("AUDITORIA_SOMENTE_LEITURA",
                    "A trilha de auditoria é somente leitura: ninguém pode alterá-la");
        }
        Permissao anterior = matriz.get(perfil).get(modulo);
        matriz.get(perfil).put(modulo, nova);
        try {
            // SoD: a mudança não pode deixar o perfil com funções incompatíveis.
            PoliticaSegregacao.exigirSemConflito(perfil, matriz.get(perfil).values());
        } catch (RegraNegocioException e) {
            if (anterior == null) {
                matriz.get(perfil).remove(modulo);
            } else {
                matriz.get(perfil).put(modulo, anterior);
            }
            throw e;
        }
        return nova;
    }

    /** Conflitos de SoD de todos os perfis, inclusive as exceções documentadas do ADMIN. */
    public List<PoliticaSegregacao.Conflito> conflitos() {
        return Arrays.stream(PerfilUsuario.values())
                .flatMap(p -> PoliticaSegregacao.conflitos(p, doPerfil(p)).stream())
                .toList();
    }

    public Optional<Permissao> buscar(PerfilUsuario perfil, Modulo modulo) {
        return Optional.ofNullable(matriz.get(perfil).get(modulo));
    }

    /** Para exibir: todos os módulos de um perfil, inclusive os sem permissão (ler e gravar falsos). */
    public List<Permissao> doPerfil(PerfilUsuario perfil) {
        return Arrays.stream(Modulo.values())
                .map(m -> buscar(perfil, m).orElse(new Permissao(m, false, false)))
                .toList();
    }
}
