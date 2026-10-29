package br.undb.semear.model.entity.pessoa;

import br.undb.semear.model.enums.TipoFuncionario;

public final class Gestor extends Funcionario {

    public Gestor(DadosFuncionario dados) {
        super(dados);
    }

    @Override
    public TipoFuncionario getTipo() {
        return TipoFuncionario.GESTOR;
    }

    @Override
    public String getFuncao() {
        return "Gestão escolar";
    }
}
