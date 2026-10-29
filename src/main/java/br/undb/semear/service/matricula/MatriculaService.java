package br.undb.semear.service.matricula;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.CobrancaResumoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.MatriculaDetalheResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.MatriculaResumoResponse;
import br.undb.semear.model.entity.matricula.ChecklistDocumentos;
import br.undb.semear.model.entity.matricula.Matricula;
import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.exception.ConflitoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.matricula.FluxoMatriculaRepository;
import br.undb.semear.repository.matricula.MatriculaRepository;
import br.undb.semear.service.academico.AnoLetivoService;
import br.undb.semear.service.pessoa.AlunoService;

@Service
public class MatriculaService {

    private final MatriculaRepository matriculas;
    private final FluxoMatriculaRepository fluxo;
    private final AnoLetivoService anosLetivos;
    private final DocumentoService documentos;

    public MatriculaService(MatriculaRepository matriculas, FluxoMatriculaRepository fluxo, AnoLetivoService anosLetivos,
                            DocumentoService documentos) {
        this.matriculas = matriculas;
        this.fluxo = fluxo;
        this.anosLetivos = anosLetivos;
        this.documentos = documentos;
    }

    public PaginaResponse<MatriculaResumoResponse> pesquisar(String busca, Integer ano, StatusMatricula status,
                                                             int pagina, int tamanho) {
        return matriculas.pesquisar(busca, anosLetivos.anoOuCorrente(ano), status, Math.max(0, pagina),
                Math.clamp(tamanho, 1, AlunoService.TAMANHO_MAXIMO_PAGINA));
    }

    public MatriculaDetalheResponse detalhar(UUID id) {
        MatriculaResumoResponse resumo = matriculas.buscarResumo(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula"));
        Matricula matricula = matriculas.buscarPorId(id).orElseThrow();
        ChecklistDocumentos checklist = documentos.checklist(resumo.alunoId());
        return new MatriculaDetalheResponse(resumo,
                matriculas.nomeResponsavelFinanceiro(resumo.alunoId()).orElse(null),
                matricula.getMotivoCancelamento().orElse(null),
                matriculas.listarContratos(id),
                matriculas.listarFardas(id),
                matriculas.buscarCobrancaMatricula(id).orElse(null),
                matriculas.possuiDebitosVencidos(resumo.alunoId()),
                checklist.estaCompleto(),
                checklist.pendentes().size());
    }

    /** RN13: a Matricula recusa o cancelamento se houver débito; cobranças futuras deixam de valer. */
    @Transactional
    public MatriculaDetalheResponse cancelar(UUID id, String motivo) {
        Matricula matricula = matriculas.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula"));
        matricula.cancelar(motivo, matriculas.possuiDebitosVencidos(matricula.getAlunoId()));
        matriculas.salvar(matricula);
        matriculas.cancelarCobrancasFuturas(id, anosLetivos.hoje());
        return detalhar(id);
    }

    /** Baixa presencial da cobrança de matrícula; o banco ativa a matrícula ao pagar (RD02). */
    @Transactional
    public MatriculaDetalheResponse registrarPagamentoMatricula(UUID id, OrigemPagamento origem) {
        CobrancaResumoResponse cobranca = matriculas.buscarCobrancaMatricula(id)
                .orElseThrow(() -> new ConflitoException("SEM_COBRANCA_MATRICULA", "Esta matrícula não tem cobrança de matrícula"));
        if (!cobranca.status().emAberto()) {
            throw new ConflitoException("COBRANCA_NAO_ABERTA", "A cobrança de matrícula não está em aberto");
        }
        fluxo.registrarPagamento(cobranca.id(), origem);
        return detalhar(id);
    }
}
