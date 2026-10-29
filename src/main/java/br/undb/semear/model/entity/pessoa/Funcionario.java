package br.undb.semear.model.entity.pessoa;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Telefone;

/**
 * Funcionário da escola (RF07). Abstrata: cada tipo é uma subclasse que define
 * sua função e o que pode fazer (polimorfismo em getFuncao() e podeLecionar()).
 */
public abstract class Funcionario extends Pessoa {

    private Telefone telefone;
    private Email email;
    private LocalDate dataAdmissao;
    private boolean ativo;
    private final UUID usuarioId;

    protected Funcionario(DadosFuncionario dados) {
        super(dados.id(), dados.nome(), exigirCpf(dados));
        this.telefone = dados.telefone();
        this.email = dados.email();
        this.dataAdmissao = validarAdmissao(dados.dataAdmissao());
        this.ativo = dados.ativo();
        this.usuarioId = dados.usuarioId();
    }

    private static br.undb.semear.model.valueobject.Cpf exigirCpf(DadosFuncionario dados) {
        if (dados.cpf() == null) {
            throw new DadoInvalidoException("cpf", "O CPF do funcionário é obrigatório");
        }
        return dados.cpf();
    }

    private static LocalDate validarAdmissao(LocalDate data) {
        if (data != null && data.isAfter(LocalDate.now())) {
            throw new DadoInvalidoException("dataAdmissao", "A data de admissão não pode ser no futuro");
        }
        return data;
    }

    public abstract TipoFuncionario getTipo();

    /** Descrição da função para exibição. */
    public abstract String getFuncao();

    /** Só professores podem ser vinculados a turmas e lançar diário (RD07). */
    public boolean podeLecionar() {
        return false;
    }

    @Override
    public String descreverPapel() {
        return getFuncao();
    }

    public void atualizarDados(String nome, Telefone novoTelefone, Email novoEmail, LocalDate novaAdmissao) {
        renomear(nome);
        this.telefone = novoTelefone;
        this.email = novoEmail;
        this.dataAdmissao = validarAdmissao(novaAdmissao);
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
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

    public Optional<LocalDate> getDataAdmissao() {
        return Optional.ofNullable(dataAdmissao);
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Optional<UUID> getUsuarioId() {
        return Optional.ofNullable(usuarioId);
    }
}
