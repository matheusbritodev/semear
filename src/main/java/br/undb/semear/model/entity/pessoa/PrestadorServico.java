package br.undb.semear.model.entity.pessoa;

import br.undb.semear.model.enums.TipoFuncionario;

public final class PrestadorServico extends Funcionario {

    public PrestadorServico(DadosFuncionario dados) {
        super(dados);
    }

    @Override
    public TipoFuncionario getTipo() {
        return TipoFuncionario.PRESTADOR_SERVICO;
    }

    @Override
    public String getFuncao() {
        return "Prestador(a) de serviço";
    }
}
