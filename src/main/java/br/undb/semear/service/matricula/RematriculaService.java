package br.undb.semear.service.matricula;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.matricula.MatriculaDtos.MatriculaResumoResponse;
import br.undb.semear.dto.matricula.MatriculaDtos.RematriculaOpcaoResponse;
import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.entity.academico.Turma;
import br.undb.semear.model.entity.matricula.Rematricula;
import br.undb.semear.model.enums.TipoFarda;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.academico.SerieRepository;
import br.undb.semear.repository.academico.TurmaRepository;
import br.undb.semear.repository.matricula.FluxoMatriculaRepository;
import br.undb.semear.repository.matricula.MatriculaRepository;
import br.undb.semear.service.academico.AnoLetivoService;

/**
 * Rematrícula (RF01.5) usada pelo portal e pela secretaria.
 * Junta os fatos do banco e deixa a decisão para Rematricula.avaliar (Model).
 */
@Service
public class RematriculaService {

    private final MatriculaRepository matriculas;
    private final TurmaRepository turmas;
    private final SerieRepository series;
    private final FluxoMatriculaRepository fluxo;
    private final AnoLetivoService anosLetivos;

    public RematriculaService(MatriculaRepository matriculas, TurmaRepository turmas, SerieRepository series,
                              FluxoMatriculaRepository fluxo, AnoLetivoService anosLetivos) {
        this.matriculas = matriculas;
        this.turmas = turmas;
        this.series = series;
        this.fluxo = fluxo;
        this.anosLetivos = anosLetivos;
    }

    public RematriculaOpcaoResponse opcao(UUID alunoId) {
        int anoAtual = anosLetivos.corrente().getAno();
        int anoDestino = anoAtual + 1;
        Optional<MatriculaResumoResponse> atual = matriculas.buscarDoAlunoNoAno(alunoId, anoAtual);
        Optional<MatriculaResumoResponse> destino = matriculas.buscarDoAlunoNoAno(alunoId, anoDestino);
        Rematricula.Avaliacao avaliacao = avaliar(alunoId, atual, destino.isPresent(), anoDestino);

        String nomeAluno = atual.or(() -> destino).map(MatriculaResumoResponse::aluno).orElse(null);
        return RematriculaOpcaoResponse.de(alunoId, nomeAluno, atual.map(MatriculaResumoResponse::turma).orElse(null),
                anoDestino, avaliacao,
                destino.map(MatriculaResumoResponse::id).orElse(null),
                destino.map(MatriculaResumoResponse::status).orElse(null));
    }

    /** Revalida tudo no momento do pedido e chama fn_rematricular (o trigger confirma as vagas). */
    @Transactional
    public UUID rematricular(UUID alunoId, String fardaUniforme, String fardaEdFisica) {
        String uniforme = TipoFarda.validarTamanho(fardaUniforme, "fardaUniforme");
        String edFisica = TipoFarda.validarTamanho(fardaEdFisica, "fardaEdFisica");
        int anoAtual = anosLetivos.corrente().getAno();
        int anoDestino = anoAtual + 1;
        Optional<MatriculaResumoResponse> atual = matriculas.buscarDoAlunoNoAno(alunoId, anoAtual);
        boolean jaTem = matriculas.buscarDoAlunoNoAno(alunoId, anoDestino).isPresent();
        Turma turma = Rematricula.exigirTurma(avaliar(alunoId, atual, jaTem, anoDestino));
        return fluxo.rematricular(alunoId, turma.getId(), uniforme, edFisica);
    }

    private Rematricula.Avaliacao avaliar(UUID alunoId, Optional<MatriculaResumoResponse> atual, boolean jaTemDestino,
                                          int anoDestino) {
        int ordemAtual = atual.map(m -> turmas.buscarPorId(m.turmaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Turma")).getSerie().getOrdem())
                .orElse(0);
        int ultimaOrdem = series.listar().stream().mapToInt(Serie::getOrdem).max().orElse(0);
        List<Turma> turmasDestino = ordemAtual == 0 ? List.of()
                : turmas.listarPorAno(anoDestino, null).stream()
                        .filter(t -> t.getSerie().getOrdem() == ordemAtual + 1)
                        .toList();
        return Rematricula.avaliar(new Rematricula.Fatos(
                atual.map(MatriculaResumoResponse::status),
                jaTemDestino,
                matriculas.possuiDebitosVencidos(alunoId),
                ordemAtual < ultimaOrdem,
                turmasDestino));
    }
}
