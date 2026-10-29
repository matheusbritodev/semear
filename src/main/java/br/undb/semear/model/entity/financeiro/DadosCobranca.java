package br.undb.semear.model.entity.financeiro;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.valueobject.Dinheiro;

/** Dados gravados de uma cobrança (linha da tabela cobranca). A CobrancaFactory monta a subclasse certa. */
public record DadosCobranca(UUID id, UUID matriculaId, UUID responsavelId, TipoCobranca tipo, YearMonth competencia,
                            Dinheiro valorBruto, Dinheiro descontoBolsa, Dinheiro valorFinal, LocalDate vencimento,
                            StatusCobranca status, Pagamento pagamento, ReferenciaCora cora) {
}
