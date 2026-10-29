package br.undb.semear.model.entity.matricula;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.TipoContrato;

/** Ficha de Sócio da Educação Infantil: o responsável se associa e paga a Taxa de Associado (RN01, RN07). */
public final class FichaSocio extends Contrato {

    public FichaSocio(UUID id, UUID matriculaId, StatusContrato status, UUID assinadoPor, OffsetDateTime assinadoEm) {
        super(id, matriculaId, status, assinadoPor, assinadoEm);
    }

    @Override
    public TipoContrato getTipo() {
        return TipoContrato.FICHA_SOCIO;
    }

    @Override
    public String getTitulo() {
        return "Ficha de Sócio";
    }

    @Override
    protected Clausula objeto(CondicoesContrato c) {
        return new Clausula("Associação",
                "O responsável se associa ao Centro Educacional Semeando para que o(a) aluno(a) " + c.aluno()
                        + " (nº de cadastro " + c.numeroCadastro() + ") frequente o " + c.serie() + ", turma " + c.turma()
                        + ", no ano letivo de " + c.ano() + " (" + periodo(c) + ").");
    }

    @Override
    protected List<Clausula> valores(CondicoesContrato c) {
        return List.of(new Clausula("Contribuições",
                "Matrícula de " + c.valorMatricula() + " e Taxa de Associado de " + c.valorMensal()
                        + " por mês, com vencimento no dia " + c.diaVencimento()
                        + ". A Taxa de Associado não tem desconto de pontualidade."));
    }
}
