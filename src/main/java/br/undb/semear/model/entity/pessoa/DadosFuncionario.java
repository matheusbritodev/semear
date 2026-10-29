package br.undb.semear.model.entity.pessoa;

import java.time.LocalDate;
import java.util.UUID;

import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Telefone;

/** Dados comuns a todo funcionário, usados pela FuncionarioFactory para criar a subclasse certa. */
public record DadosFuncionario(UUID id, String nome, Cpf cpf, Telefone telefone, Email email,
                               LocalDate dataAdmissao, boolean ativo, UUID usuarioId) {
}
