package br.undb.semear.service.pedagogico;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import br.undb.semear.dto.pedagogico.BoletimDtos.BoletimResponse;
import br.undb.semear.dto.pedagogico.BoletimDtos.DisciplinaResponse;
import br.undb.semear.dto.pedagogico.BoletimDtos.FrequenciaResponse;
import br.undb.semear.model.entity.pedagogico.Boletim;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.pedagogico.BoletimRepository;
import br.undb.semear.repository.pedagogico.BoletimRepository.MatriculaDoBoletim;
import br.undb.semear.service.academico.AnoLetivoService;

/** Boletim de um aluno num ano (padrão: o ano letivo em andamento). Na Educação Infantil só há frequência. */
@Service
public class BoletimService {

    private final BoletimRepository boletins;
    private final AnoLetivoService anosLetivos;

    public BoletimService(BoletimRepository boletins, AnoLetivoService anosLetivos) {
        this.boletins = boletins;
        this.anosLetivos = anosLetivos;
    }

    public BoletimResponse doAluno(UUID alunoId, Integer ano) {
        List<MatriculaDoBoletim> matriculas = boletins.matriculasDoAluno(alunoId);
        // Sem ano informado: o ano letivo em andamento; se o aluno não estiver nele, o mais recente.
        int alvo = ano != null ? ano : anosLetivos.corrente().getAno();
        MatriculaDoBoletim m = matriculas.stream().filter(x -> x.ano() == alvo).findFirst()
                .or(() -> ano == null ? matriculas.stream().findFirst() : java.util.Optional.empty())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Boletim"));
        Boletim boletim = new Boletim(boletins.notas(m.matriculaId()), boletins.frequencia(m.matriculaId()));
        List<DisciplinaResponse> disciplinas = boletim.disciplinas().stream()
                .map(d -> new DisciplinaResponse(d,
                        IntStream.rangeClosed(1, Boletim.BIMESTRES).mapToObj(b -> boletim.nota(d, b).orElse(null)).toList(),
                        boletim.mediaParcial(d).orElse(null)))
                .toList();
        Boletim.Frequencia f = boletim.getFrequencia();
        BigDecimal percentual = f.percentual().orElse(null);
        return new BoletimResponse(alunoId, m.matriculaId(), m.ano(), m.serie(), m.turma(), m.nivel(),
                matriculas.stream().map(MatriculaDoBoletim::ano).toList(), boletim.bimestresLancados(), disciplinas,
                boletim.mediaGeral().orElse(null), new FrequenciaResponse(f.aulas(), f.presencas(), f.faltas(), percentual));
    }
}
