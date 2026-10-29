package br.undb.semear.model.entity.pessoa;

import br.undb.semear.model.enums.TipoFuncionario;

public final class Professor extends Funcionario {

    public Professor(DadosFuncionario dados) {
        super(dados);
    }

    @Override
    public TipoFuncionario getTipo() {
        return TipoFuncionario.PROFESSOR;
    }

    @Override
    public String getFuncao() {
        return "Professor(a)";
    }

    @Override
    public boolean podeLecionar() {
        return true;
    }
}
