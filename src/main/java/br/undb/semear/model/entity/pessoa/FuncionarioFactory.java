package br.undb.semear.model.entity.pessoa;

import java.util.Objects;

import br.undb.semear.model.enums.TipoFuncionario;

/** Padrão Factory: cria a subclasse de Funcionario correspondente ao tipo gravado no banco. */
public final class FuncionarioFactory {

    private FuncionarioFactory() {
    }

    public static Funcionario criar(TipoFuncionario tipo, DadosFuncionario dados) {
        Objects.requireNonNull(tipo, "tipo");
        return switch (tipo) {
            case PROFESSOR -> new Professor(dados);
            case ADMINISTRATIVO -> new Administrativo(dados);
            case GESTOR -> new Gestor(dados);
            case PRESTADOR_SERVICO -> new PrestadorServico(dados);
        };
    }
}
