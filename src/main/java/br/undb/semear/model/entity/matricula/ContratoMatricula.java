package br.undb.semear.model.entity.matricula;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.TipoContrato;

/** Contrato de prestação de serviços educacionais do Ensino Fundamental (RN07). */
public final class ContratoMatricula extends Contrato {

    public ContratoMatricula(UUID id, UUID matriculaId, StatusContrato status, UUID assinadoPor, OffsetDateTime assinadoEm) {
        super(id, matriculaId, status, assinadoPor, assinadoEm);
    }

    @Override
    public TipoContrato getTipo() {
        return TipoContrato.CONTRATO_MATRICULA;
    }

    @Override
    public String getTitulo() {
        return "Contrato de Matrícula";
    }

    @Override
    protected Clausula objeto(CondicoesContrato c) {
        return new Clausula("Objeto",
                "Prestação de serviços educacionais do Centro Educacional Semeando ao(à) aluno(a) " + c.aluno()
                        + " (nº de cadastro " + c.numeroCadastro() + "), no " + c.serie() + ", turma " + c.turma()
                        + ", durante o ano letivo de " + c.ano() + " (" + periodo(c) + ").");
    }

    @Override
    protected List<Clausula> valores(CondicoesContrato c) {
        return List.of(
                new Clausula("Valores",
                        "Matrícula de " + c.valorMatricula() + " e mensalidade de " + c.valorMensal()
                                + ", com vencimento no dia " + c.diaVencimento() + " de cada mês do ano letivo, conforme a "
                                + "tabela de preços da escola para a série."),
                new Clausula("Desconto de pontualidade",
                        "A mensalidade paga até a data do vencimento tem desconto de 10%."));
    }
}
