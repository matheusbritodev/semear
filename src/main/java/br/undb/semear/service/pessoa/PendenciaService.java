package br.undb.semear.service.pessoa;

import java.util.List;

import org.springframework.stereotype.Service;

import br.undb.semear.dto.pessoa.PendenciaDtos.PendenciaDocumentoResponse;
import br.undb.semear.dto.pessoa.PendenciaDtos.ResumoPendenciasResponse;
import br.undb.semear.repository.matricula.PendenciaRepository;
import br.undb.semear.service.academico.AnoLetivoService;

/**
 * Painel de pendências dos alunos (orientação do professor, 28/10/2026): a matrícula pode ser
 * concluída sem todos os documentos, mas o gestor precisa ver quem está pendente para cobrar,
 * porque esses documentos alimentam obrigações da escola (ex.: Censo/INEP).
 */
@Service
public class PendenciaService {

    private final PendenciaRepository pendencias;
    private final AnoLetivoService anosLetivos;

    public PendenciaService(PendenciaRepository pendencias, AnoLetivoService anosLetivos) {
        this.pendencias = pendencias;
        this.anosLetivos = anosLetivos;
    }

    /** Considera matrículas vigentes do ano corrente em diante (inclui quem já está matriculado no próximo ano). */
    public List<PendenciaDocumentoResponse> documentos() {
        return pendencias.documentosPendentes(anosLetivos.corrente().getAno());
    }

    public ResumoPendenciasResponse resumo() {
        int ano = anosLetivos.corrente().getAno();
        List<PendenciaDocumentoResponse> lista = pendencias.documentosPendentes(ano);
        return new ResumoPendenciasResponse(lista.size(),
                lista.stream().mapToInt(PendenciaDocumentoResponse::pendentes).sum(),
                pendencias.alunosComMatriculaVigente(ano));
    }
}
