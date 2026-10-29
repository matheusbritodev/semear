package br.undb.semear.repository.pessoa;

import java.util.List;
import java.util.Map;

import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Permissao;

/** Matriz perfil × módulo (perfil_permissao) e a cópia padrão (perfil_permissao_padrao). */
public interface PermissaoRepository {

    Map<PerfilUsuario, List<Permissao>> carregarMatriz();

    /** Grava uma célula; ler e gravar falsos removem a linha (sem acesso ao módulo). */
    void salvar(PerfilUsuario perfil, Permissao permissao);

    void restaurarPadrao();
}
