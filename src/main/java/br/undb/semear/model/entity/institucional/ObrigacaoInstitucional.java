package br.undb.semear.model.entity.institucional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.enums.OrgaoExterno;
import br.undb.semear.model.enums.SituacaoObrigacao;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;

/**
 * Obrigação da escola com um órgão externo (RF09): Censo/INEP, SMTT, Sistema Presença, Cora.
 * Periódica (renova sozinha ao ser cumprida) ou única (sem periodicidade).
 * Cumprir exige comprovante enviado no ciclo atual (decisão D14).
 */
public class ObrigacaoInstitucional extends Entidade {

    /** Fuso da escola (São Luís/MA): o dia do envio é o dia local, não o UTC gravado. */
    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

    private OrgaoExterno orgao;
    private String descricao;
    private Integer periodicidadeMeses;
    private LocalDate proximoVencimento;
    private int diasAlerta;
    private LocalDate ultimoCumprimento;
    private boolean encerrada;
    private UUID responsavelId;

    public ObrigacaoInstitucional(UUID id, OrgaoExterno orgao, String descricao, Integer periodicidadeMeses,
                                  LocalDate proximoVencimento, int diasAlerta, LocalDate ultimoCumprimento, boolean encerrada,
                                  UUID responsavelId) {
        super(id);
        this.orgao = orgao;
        this.descricao = descricao;
        this.periodicidadeMeses = periodicidadeMeses;
        this.proximoVencimento = proximoVencimento;
        this.diasAlerta = diasAlerta;
        this.ultimoCumprimento = ultimoCumprimento;
        this.encerrada = encerrada;
        this.responsavelId = responsavelId;
    }

    public static ObrigacaoInstitucional nova(OrgaoExterno orgao, String descricao, Integer periodicidadeMeses,
                                              LocalDate vencimento, int diasAlerta, UUID responsavelId) {
        ObrigacaoInstitucional o = new ObrigacaoInstitucional(null, orgao, descricao, periodicidadeMeses, vencimento, diasAlerta,
                null, false, responsavelId);
        o.alterar(orgao, descricao, periodicidadeMeses, vencimento, diasAlerta, responsavelId);
        return o;
    }

    public final void alterar(OrgaoExterno orgao, String descricao, Integer periodicidadeMeses, LocalDate vencimento,
                              int diasAlerta, UUID responsavelId) {
        String texto = descricao == null ? "" : descricao.trim();
        if (texto.isEmpty() || texto.length() > 255) {
            throw new DadoInvalidoException("descricao", "Descreva a obrigação (até 255 caracteres)");
        }
        if (periodicidadeMeses != null && (periodicidadeMeses < 1 || periodicidadeMeses > 60)) {
            throw new DadoInvalidoException("periodicidadeMeses", "Periodicidade de 1 a 60 meses (ou vazia para obrigação única)");
        }
        if (vencimento == null) {
            throw new DadoInvalidoException("proximoVencimento", "Informe o vencimento");
        }
        if (diasAlerta < 0 || diasAlerta > 365) {
            throw new DadoInvalidoException("diasAlerta", "Aviso de 0 a 365 dias antes");
        }
        this.orgao = Objects.requireNonNull(orgao, "orgao");
        this.descricao = texto;
        this.periodicidadeMeses = periodicidadeMeses;
        this.proximoVencimento = vencimento;
        this.diasAlerta = diasAlerta;
        this.responsavelId = responsavelId;
    }

    public SituacaoObrigacao situacaoEm(LocalDate hoje) {
        if (encerrada) {
            return SituacaoObrigacao.CUMPRIDA;
        }
        if (proximoVencimento.isBefore(hoje)) {
            return SituacaoObrigacao.ATRASADA;
        }
        return diasRestantes(hoje) <= diasAlerta ? SituacaoObrigacao.EM_ALERTA : SituacaoObrigacao.EM_DIA;
    }

    /** Negativo quando atrasada. */
    public long diasRestantes(LocalDate hoje) {
        return ChronoUnit.DAYS.between(hoje, proximoVencimento);
    }

    /** Comprovantes que contam para o ciclo atual: enviados depois do último cumprimento. */
    public long comprovantesDoCiclo(Collection<OffsetDateTime> enviadosEm) {
        return enviadosEm.stream()
                .filter(e -> ultimoCumprimento == null || e.atZoneSameInstant(FUSO).toLocalDate().isAfter(ultimoCumprimento))
                .count();
    }

    /**
     * Marca como cumprida hoje. Periódica: o próximo vencimento anda uma periodicidade (como fn_cumprir_obrigacao)
     * e a obrigação continua pendente; única: fica encerrada. Exige comprovante do ciclo.
     */
    public void cumprir(LocalDate hoje, Collection<OffsetDateTime> comprovantesEnviadosEm) {
        if (encerrada) {
            throw new ConflitoException("OBRIGACAO_JA_CUMPRIDA", "Esta obrigação já foi cumprida");
        }
        if (comprovantesDoCiclo(comprovantesEnviadosEm) == 0) {
            throw new RegraNegocioException("SEM_COMPROVANTE", "Anexe o comprovante antes de marcar como cumprida");
        }
        ultimoCumprimento = hoje;
        if (periodicidadeMeses == null) {
            encerrada = true;
        } else {
            proximoVencimento = proximoVencimento.plusMonths(periodicidadeMeses);
        }
    }

    public boolean periodica() {
        return periodicidadeMeses != null;
    }

    public OrgaoExterno getOrgao() {
        return orgao;
    }

    public String getDescricao() {
        return descricao;
    }

    public Integer getPeriodicidadeMeses() {
        return periodicidadeMeses;
    }

    public LocalDate getProximoVencimento() {
        return proximoVencimento;
    }

    public int getDiasAlerta() {
        return diasAlerta;
    }

    public LocalDate getUltimoCumprimento() {
        return ultimoCumprimento;
    }

    public boolean isEncerrada() {
        return encerrada;
    }

    public UUID getResponsavelId() {
        return responsavelId;
    }
}
