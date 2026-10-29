package br.undb.semear.model.entity.financeiro;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.exception.RegraNegocioException;

/** Bolsa concedida a uma matrícula (tabela bolsa_matricula), com vigência. RN11: revogável. */
public class ConcessaoBolsa extends Entidade {

    private final UUID bolsaId;
    private final UUID matriculaId;
    private final LocalDate vigenciaInicio;
    private LocalDate vigenciaFim;
    private final UUID concedidaPor;

    public ConcessaoBolsa(UUID id, UUID bolsaId, UUID matriculaId, LocalDate vigenciaInicio, LocalDate vigenciaFim,
                          UUID concedidaPor) {
        super(id);
        this.bolsaId = Objects.requireNonNull(bolsaId, "bolsaId");
        this.matriculaId = Objects.requireNonNull(matriculaId, "matriculaId");
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio, "vigenciaInicio");
        if (vigenciaFim != null && vigenciaFim.isBefore(vigenciaInicio)) {
            throw new RegraNegocioException("VIGENCIA_INVALIDA", "O fim da bolsa não pode ser antes do início");
        }
        this.vigenciaFim = vigenciaFim;
        this.concedidaPor = concedidaPor;
    }

    public boolean vigenteEm(LocalDate dia) {
        return !dia.isBefore(vigenciaInicio) && (vigenciaFim == null || !dia.isAfter(vigenciaFim));
    }

    /** Ainda vale hoje ou vai valer (não foi encerrada antes de hoje). */
    public boolean ativaOuFutura(LocalDate hoje) {
        return vigenciaFim == null || !vigenciaFim.isBefore(hoje);
    }

    /**
     * Revoga a partir de hoje. Se a bolsa ainda nem começou, não há o que guardar:
     * devolve true para indicar que a concessão pode ser apagada.
     */
    public boolean revogar(LocalDate hoje) {
        if (!ativaOuFutura(hoje)) {
            throw new RegraNegocioException("BOLSA_JA_ENCERRADA", "Esta bolsa já estava encerrada");
        }
        if (vigenciaInicio.isAfter(hoje)) {
            return true;
        }
        vigenciaFim = hoje;
        return false;
    }

    public UUID getBolsaId() {
        return bolsaId;
    }

    public UUID getMatriculaId() {
        return matriculaId;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public UUID getConcedidaPor() {
        return concedidaPor;
    }
}
