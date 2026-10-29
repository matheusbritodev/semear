package br.undb.semear.model.entity.pessoa;

import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Telefone;

/** Pai, mãe ou outro responsável. Relaciona-se N:N com alunos (RN02) por VinculoResponsavel. */
public class Responsavel extends Pessoa {

    private Telefone telefone;
    private Email email;
    private final UUID usuarioId;

    public Responsavel(UUID id, String nome, Cpf cpf, Telefone telefone, Email email, UUID usuarioId) {
        super(id, nome, exigirCpf(cpf));
        this.telefone = telefone;
        this.email = email;
        this.usuarioId = usuarioId;
    }

    private static Cpf exigirCpf(Cpf cpf) {
        if (cpf == null) {
            throw new DadoInvalidoException("cpf", "O CPF do responsável é obrigatório");
        }
        return cpf;
    }

    @Override
    public String descreverPapel() {
        return "Responsável";
    }

    /** Pelo menos uma forma de contato precisa existir (a escola precisa avisar a família). */
    public void atualizarContato(Telefone novoTelefone, Email novoEmail) {
        if (novoTelefone == null && novoEmail == null) {
            throw new DadoInvalidoException("telefone", "Informe ao menos um telefone ou e-mail");
        }
        this.telefone = novoTelefone;
        this.email = novoEmail;
    }

    public boolean possuiLogin() {
        return usuarioId != null;
    }

    public Optional<Telefone> getTelefone() {
        return Optional.ofNullable(telefone);
    }

    public Optional<Email> getEmail() {
        return Optional.ofNullable(email);
    }

    public Optional<UUID> getUsuarioId() {
        return Optional.ofNullable(usuarioId);
    }

    public Cpf getCpfObrigatorio() {
        return getCpf().orElseThrow();
    }
}
