package br.undb.semear.model.entity.pessoa;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.Acao;
import br.undb.semear.model.enums.Modulo;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Permissao;

/**
 * Acesso de uma pessoa ao sistema. O id é o mesmo do usuário no Supabase Auth (auth.users.id);
 * as credenciais ficam no Auth. O perfil define as permissões por módulo.
 */
public class Usuario extends Entidade {

    private final Email email;
    private final PerfilUsuario perfil;
    private boolean ativo;
    private final OffsetDateTime ultimoLogin;
    private final Map<Modulo, Permissao> permissoes;

    public Usuario(UUID id, Email email, PerfilUsuario perfil, boolean ativo,
                   OffsetDateTime ultimoLogin, Collection<Permissao> permissoes) {
        super(Objects.requireNonNull(id, "O id do usuário vem do Supabase Auth e é obrigatório"));
        this.email = Objects.requireNonNull(email, "email");
        this.perfil = Objects.requireNonNull(perfil, "perfil");
        this.ativo = ativo;
        this.ultimoLogin = ultimoLogin;
        this.permissoes = new EnumMap<>(Modulo.class);
        permissoes.forEach(p -> this.permissoes.put(p.modulo(), p));
    }

    /** Pode executar a ação no módulo? Usuário inativo não pode nada. */
    public boolean pode(Modulo modulo, Acao acao) {
        Permissao permissao = permissoes.get(modulo);
        return ativo && permissao != null && permissao.permite(acao);
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public Email getEmail() {
        return email;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getUltimoLogin() {
        return ultimoLogin;
    }

    public List<Permissao> getPermissoes() {
        return List.copyOf(permissoes.values());
    }

    @Override
    public String toString() {
        return "Usuario[id=" + id + ", perfil=" + perfil + ", ativo=" + ativo + "]";
    }
}
