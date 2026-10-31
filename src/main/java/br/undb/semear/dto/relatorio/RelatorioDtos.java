package br.undb.semear.dto.relatorio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import br.undb.semear.model.entity.relatorio.RelatorioAlunos;
import br.undb.semear.model.enums.StatusMatricula;

/** Relatórios emitidos pela escola (GET /api/relatorios/...). */
public final class RelatorioDtos {

    private RelatorioDtos() {
    }

    public record RelatorioAlunosResponse(int ano, String turma, OffsetDateTime emitidoEm, String emitidoPor, int total,
                                          long vigentes, long comDocumentosPendentes,
                                          Map<StatusMatricula, Integer> porSituacao,
                                          List<RelatorioAlunos.TotalTurma> porTurma,
                                          List<RelatorioAlunos.Linha> linhas) {

        public static RelatorioAlunosResponse de(RelatorioAlunos r, String turma, OffsetDateTime emitidoEm,
                                                 String emitidoPor) {
            return new RelatorioAlunosResponse(r.getAno(), turma, emitidoEm, emitidoPor, r.getLinhas().size(),
                    r.vigentes(), r.comDocumentosPendentes(), r.porSituacao(), r.porTurma(), r.getLinhas());
        }
    }
}
