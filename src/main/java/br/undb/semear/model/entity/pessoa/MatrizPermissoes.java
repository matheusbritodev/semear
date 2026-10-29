package br.undb.semear.model.entity.pessoa;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        matriz.get(perfil).put(modulo, nova);
        return nova;
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
