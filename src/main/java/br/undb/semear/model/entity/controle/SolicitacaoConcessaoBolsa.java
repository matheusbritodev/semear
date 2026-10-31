package br.undb.semear.model.entity.controle;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.enums.TipoSolicitacao;

/** Conceder uma bolsa a uma matrícula: o desconto só entra nas cobranças depois da aprovação. */
public class SolicitacaoConcessaoBolsa extends SolicitacaoAprovacao {

    private final UUID matriculaId;
    private final UUID bolsaId;
    private final LocalDate vigenciaInicio;

    public SolicitacaoConcessaoBolsa(UUID id, String resumo, UUID solicitadaPor, OffsetDateTime solicitadaEm,
                                     Decisao decisao, UUID matriculaId, UUID bolsaId, LocalDate vigenciaInicio) {
        super(id, TipoSolicitacao.CONCESSAO_BOLSA, resumo, solicitadaPor, solicitadaEm, decisao);
        this.matriculaId = Objects.requireNonNull(matriculaId, "matriculaId");
        this.bolsaId = Objects.requireNonNull(bolsaId, "bolsaId");
        this.vigenciaInicio = vigenciaInicio;
    }

    @Override
    public void efetivar(EfeitosAprovacao efeitos) {
        efeitos.concederBolsa(matriculaId, bolsaId, vigenciaInicio, getSolicitadaPor());
    }

    @Override
    public Map<String, String> dados() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("matriculaId", matriculaId.toString());
        d.put("bolsaId", bolsaId.toString());
        if (vigenciaInicio != null) {
            d.put("vigenciaInicio", vigenciaInicio.toString());
        }
        return d;
    }

    @Override
    public String chave() {
        return matriculaId + ":" + bolsaId;
    }

    @Override
    public Optional<UUID> matriculaEnvolvida() {
        return Optional.of(matriculaId);
    }

    public UUID getBolsaId() {
        return bolsaId;
    }
}
