package br.undb.semear.service.pessoa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.model.entity.pessoa.MatrizPermissoes;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Permissao;
import br.undb.semear.repository.pessoa.PermissaoRepository;

/**
 * Perfis de acesso configuráveis pelo gestor (orientação do professor, 28/10/2026).
 * As mudanças valem na próxima requisição: as permissões são lidas do banco a cada acesso.
 */
@Service
public class PermissaoService {

    private final PermissaoRepository permissoes;

    public PermissaoService(PermissaoRepository permissoes) {
        this.permissoes = permissoes;
    }

    public MatrizPermissoes matriz() {
        return new MatrizPermissoes(permissoes.carregarMatriz());
    }

    @Transactional
    public MatrizPermissoes alterar(PerfilUsuario perfil, Modulo modulo, boolean ler, boolean gravar) {
        MatrizPermissoes matriz = matriz();
        Permissao nova = matriz.alterar(perfil, modulo, ler, gravar);
        permissoes.salvar(perfil, nova);
        return matriz;
    }

    @Transactional
    public MatrizPermissoes restaurarPadrao() {
        permissoes.restaurarPadrao();
        return matriz();
    }
}
