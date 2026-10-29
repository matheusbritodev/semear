package br.undb.semear.dto.institucional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.enums.OrgaoExterno;
import br.undb.semear.model.enums.SituacaoObrigacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Obrigações institucionais (RF09). */
public final class ObrigacaoDtos {

    private ObrigacaoDtos() {
    }

    public record ComprovanteResponse(UUID id, String extensao, OffsetDateTime enviadoEm, boolean doCicloAtual) {
    }

    /** {@code podeCumprir}: há comprovante do ciclo atual e a obrigação não está encerrada. */
    public record ObrigacaoResponse(UUID id, OrgaoExterno orgao, String orgaoRotulo, String descricao,
                                    Integer periodicidadeMeses, LocalDate proximoVencimento, long diasRestantes,
                                    int diasAlerta, SituacaoObrigacao situacao, LocalDate ultimoCumprimento,
                                    UUID responsavelId, String responsavel, List<ComprovanteResponse> comprovantes,
                                    boolean podeCumprir) {
    }

    public record ObrigacaoRequest(@NotNull OrgaoExterno orgao, @NotBlank @Size(max = 255) String descricao,
                                   @Min(1) @Max(60) Integer periodicidadeMeses, @NotNull LocalDate proximoVencimento,
                                   @NotNull @Min(0) @Max(365) Integer diasAlerta, UUID responsavelId) {
    }

    public record FuncionarioOpcaoResponse(UUID id, String nome) {
    }
}
