package br.undb.semear.model.entity.financeiro;

import java.util.Objects;

import br.undb.semear.model.valueobject.CalendarioDiasUteis;

/**
 * Factory: escolhe a subclasse pelo tipo gravado e liga a regra de encargos certa.
 * Mensalidade e Taxa de Associado seguem o contrato (RN09); a de matrícula não tem encargos.
 */
public final class CobrancaFactory {

    private final RegraEncargos encargosContrato;

    public CobrancaFactory(CalendarioDiasUteis calendario) {
        this.encargosContrato = new RegraEncargosContrato(Objects.requireNonNull(calendario, "calendario"));
    }

    public Cobranca criar(DadosCobranca dados) {
        return switch (Objects.requireNonNull(dados.tipo(), "tipo")) {
            case MENSALIDADE -> new Mensalidade(dados, encargosContrato);
            case TAXA_ASSOCIADO -> new TaxaAssociado(dados, encargosContrato);
            case MATRICULA -> new CobrancaMatricula(dados, SemEncargos.INSTANCIA);
        };
    }
}
