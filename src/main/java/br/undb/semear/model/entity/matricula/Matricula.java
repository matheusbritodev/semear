package br.undb.semear.model.entity.matricula;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.enums.TipoMatricula;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;

/**
 * Aluno em uma turma em um ano letivo: a entidade central do sistema.
 * As transições de status seguem StatusMatricula.podeIrPara (padrão State via enum).
 * Ativação pelo pagamento é feita pelo banco (RD02); aqui ficam cancelamento e encerramento.
 */
public class Matricula extends Entidade {

    private final UUID alunoId;
    private final UUID turmaId;
    private final int anoLetivo;
    private final TipoMatricula tipo;
    private StatusMatricula status;
    private final LocalDate dataMatricula;
    private String motivoCancelamento;

    public Matricula(UUID id, UUID alunoId, UUID turmaId, int anoLetivo, TipoMatricula tipo,
                     StatusMatricula status, LocalDate dataMatricula, String motivoCancelamento) {
        super(id);
        this.alunoId = Objects.requireNonNull(alunoId, "alunoId");
        this.turmaId = Objects.requireNonNull(turmaId, "turmaId");
        this.anoLetivo = anoLetivo;
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.status = Objects.requireNonNull(status, "status");
        this.dataMatricula = dataMatricula;
        this.motivoCancelamento = motivoCancelamento;
    }

    /**
     * Cancela a matrícula. RN13 (contrato, cl. 9 e 11): cancelamento ou transferência exige quitação,
     * por isso quem chama informa se há cobranças vencidas em aberto.
     */
    public void cancelar(String motivo, boolean possuiDebitosVencidos) {
        mudarPara(StatusMatricula.CANCELADA);
        String limpo = motivo == null ? "" : motivo.trim();
        if (limpo.length() < 10) {
            throw new DadoInvalidoException("motivo", "Explique o motivo do cancelamento (mínimo de 10 caracteres)");
        }
        if (possuiDebitosVencidos) {
            throw new RegraNegocioException("DEBITOS_EM_ABERTO",
                    "Há cobranças vencidas: o cancelamento exige a quitação (contrato, cláusulas 9 e 11)");
        }
        this.status = StatusMatricula.CANCELADA;
        this.motivoCancelamento = limpo;
    }

    private void mudarPara(StatusMatricula destino) {
        if (!status.podeIrPara(destino)) {
            throw new ConflitoException("TRANSICAO_INVALIDA",
                    "Uma matrícula " + status.name().toLowerCase().replace('_', ' ') + " não pode ser "
                            + destino.name().toLowerCase());
        }
    }

    public boolean ocupaVaga() {
        return status.ocupaVaga();
    }

    public UUID getAlunoId() {
        return alunoId;
    }

    public UUID getTurmaId() {
        return turmaId;
    }

    public int getAnoLetivo() {
        return anoLetivo;
    }

    public TipoMatricula getTipo() {
        return tipo;
    }

    public StatusMatricula getStatus() {
        return status;
    }

    public LocalDate getDataMatricula() {
        return dataMatricula;
    }

    public Optional<String> getMotivoCancelamento() {
        return Optional.ofNullable(motivoCancelamento);
    }
}
