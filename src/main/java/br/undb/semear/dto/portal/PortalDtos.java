package br.undb.semear.dto.portal;

import java.util.List;
import java.util.UUID;

import br.undb.semear.dto.financeiro.FinanceiroDtos.CobrancaResponse;
import br.undb.semear.model.enums.NivelEnsino;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Entrada e saída do portal do responsável (/api/portal). */
public final class PortalDtos {

    private PortalDtos() {
    }

    /** Primeiro acesso: liga o login a um cadastro de responsável (novo ou já existente pelo CPF). */
    public record CompletarCadastroRequest(
            @NotBlank(message = "Informe seu nome completo") @Size(max = 150) String nome,
            @NotBlank(message = "Informe seu CPF") String cpf,
            @NotBlank(message = "Informe um telefone") @Size(max = 20) String telefone) {
    }

    public record SerieResponse(UUID id, String nome, NivelEnsino nivel, String nivelRotulo, int ordem) {
    }

    /** O que a família pode escolher na pré-matrícula. */
    public record OpcoesMatriculaResponse(List<Integer> anos, List<SerieResponse> series) {
    }

    /** Cobranças em que o logado é o pagador. pagamentoSimulado: o Cora é falso (permite simular). */
    public record PortalCobrancasResponse(boolean pagamentoSimulado, List<CobrancaResponse> cobrancas) {
    }
}
