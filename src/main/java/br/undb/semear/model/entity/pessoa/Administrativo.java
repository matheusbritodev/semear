package br.undb.semear.model.entity.pessoa;

import br.undb.semear.model.enums.TipoFuncionario;

public final class Administrativo extends Funcionario {

    public Administrativo(DadosFuncionario dados) {
        super(dados);
    }

    @Override
    public TipoFuncionario getTipo() {
        return TipoFuncionario.ADMINISTRATIVO;
    }

    @Override
    public String getFuncao() {
        return "Administrativo";
    }
}
