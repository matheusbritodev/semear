package br.undb.semear.dto.matricula;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.TipoContrato;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Contrato de Matrícula e Ficha de Sócio (RF01.4, RD03). */
public final class ContratoDtos {

    private ContratoDtos() {
    }

    public record ClausulaResponse(String titulo, String texto) {
    }

    public record AssinanteResponse(UUID id, String nome, String parentesco, boolean financeiro) {
    }

    /** Termo completo. {@code podeAssinar}: pendente e matrícula vigente (o servidor confere de novo). */
    public record ContratoTermoResponse(UUID id, TipoContrato tipo, String titulo, StatusContrato status, UUID matriculaId,
                                        UUID alunoId, String aluno, int ano, String serie, String turma,
                                        String assinadoPor, OffsetDateTime assinadoEm, boolean podeAssinar,
                                        List<ClausulaResponse> clausulas, List<AssinanteResponse> responsaveis) {
    }

    public record ContratoResumoResponse(UUID id, TipoContrato tipo, String titulo, StatusContrato status, int ano,
                                         String serie, String assinadoPor, OffsetDateTime assinadoEm, boolean podeAssinar) {
    }

    /** Aceite eletrônico no portal: o responsável precisa marcar que leu e concorda. */
    public record AceiteRequest(@NotNull @AssertTrue(message = "Marque que leu e concorda com o documento") Boolean aceite) {
    }

    /** Assinatura feita na secretaria: quem assinou o papel. */
    public record AssinaturaPresencialRequest(@NotNull UUID responsavelId) {
    }
}
