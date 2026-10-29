package br.undb.semear.model.entity.matricula;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.enums.StatusPreMatricula;
import br.undb.semear.model.enums.TipoFarda;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;

/**
 * Pedido de matrícula feito pelo responsável no portal (RF01.1, RN05).
 * A secretaria aprova (escolhendo a turma) ou rejeita com motivo; a decisão é definitiva (RD04).
 * A aprovação em si é feita pela função do banco fn_aprovar_pre_matricula; esta classe valida antes.
 */
public class PreMatricula extends Entidade {

    public static final int TAMANHO_MINIMO_MOTIVO = 10;

    private final int anoLetivo;
    private final Serie serie;
    private final UUID responsavelId;
    private final String parentesco;
    private final DadosCrianca crianca;
    private final Map<TipoFarda, String> fardas;
    private final StatusPreMatricula status;
    private final String motivoRejeicao;
    private final UUID alunoId;
    private final OffsetDateTime criadoEm;

    public PreMatricula(UUID id, int anoLetivo, Serie serie, UUID responsavelId, String parentesco,
                        DadosCrianca crianca, Map<TipoFarda, String> fardas, StatusPreMatricula status,
                        String motivoRejeicao, UUID alunoId, OffsetDateTime criadoEm) {
        super(id);
        this.anoLetivo = anoLetivo;
        this.serie = Objects.requireNonNull(serie, "serie");
        this.responsavelId = Objects.requireNonNull(responsavelId, "responsavelId");
        this.parentesco = parentesco;
        this.crianca = Objects.requireNonNull(crianca, "crianca");
        this.fardas = validarFardas(fardas);
        this.status = Objects.requireNonNull(status, "status");
        this.motivoRejeicao = motivoRejeicao;
        this.alunoId = alunoId;
        this.criadoEm = criadoEm;
    }

    /** Novo pedido, sempre PENDENTE. */
    public static PreMatricula nova(int anoLetivo, Serie serie, UUID responsavelId, String parentesco,
                                    DadosCrianca crianca, Map<TipoFarda, String> fardas) {
        return new PreMatricula(null, anoLetivo, serie, responsavelId, parentesco, crianca, fardas,
                StatusPreMatricula.PENDENTE, null, null, null);
    }

    /** RN06: tamanhos de uniforme e de educação física, cada um de uma lista conhecida. */
    private static Map<TipoFarda, String> validarFardas(Map<TipoFarda, String> fardas) {
        Map<TipoFarda, String> resultado = new EnumMap<>(TipoFarda.class);
        if (fardas != null) {
            fardas.forEach((tipo, tamanho) -> {
                String valido = TipoFarda.validarTamanho(tamanho,
                        tipo == TipoFarda.UNIFORME ? "fardaUniforme" : "fardaEdFisica");
                if (valido != null) {
                    resultado.put(tipo, valido);
                }
            });
        }
        return resultado;
    }

    /** RD04 + RN04: só pendente, turma da mesma série e ano, e com vaga. */
    public void validarAprovacao(Turma turma) {
        exigirPendente();
        if (!turma.getSerie().getId().equals(serie.getId()) || turma.getAnoLetivo() != anoLetivo) {
            throw new RegraNegocioException("TURMA_INCOMPATIVEL",
                    "A turma escolhida não é da série e do ano letivo pedidos");
        }
        turma.garantirVaga();
    }

    /** RD04: só pendente; o motivo é obrigatório e precisa explicar a decisão à família. */
    public String validarRejeicao(String motivo) {
        exigirPendente();
        String limpo = motivo == null ? "" : motivo.trim();
        if (limpo.length() < TAMANHO_MINIMO_MOTIVO) {
            throw new DadoInvalidoException("motivo", "Explique o motivo da rejeição (mínimo de " + TAMANHO_MINIMO_MOTIVO + " caracteres)");
        }
        return limpo;
    }

    private void exigirPendente() {
        if (status.foiDecidida()) {
            throw new ConflitoException("PRE_MATRICULA_JA_ANALISADA", "Esta pré-matrícula já foi " + status.name().toLowerCase());
        }
    }

    public boolean pertenceA(UUID idResponsavel) {
        return responsavelId.equals(idResponsavel);
    }

    public int getAnoLetivo() {
        return anoLetivo;
    }

    public Serie getSerie() {
        return serie;
    }

    public UUID getResponsavelId() {
        return responsavelId;
    }

    public String getParentesco() {
        return parentesco;
    }

    public DadosCrianca getCrianca() {
        return crianca;
    }

    public Map<TipoFarda, String> getFardas() {
        return Collections.unmodifiableMap(fardas);
    }

    public StatusPreMatricula getStatus() {
        return status;
    }

    public Optional<String> getMotivoRejeicao() {
        return Optional.ofNullable(motivoRejeicao);
    }

    public Optional<UUID> getAlunoId() {
        return Optional.ofNullable(alunoId);
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
