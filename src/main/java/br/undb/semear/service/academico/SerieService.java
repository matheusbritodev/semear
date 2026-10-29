package br.undb.semear.service.academico;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.repository.academico.SerieRepository;

/** Séries da escola (Maternal 1 ao 5º ano), sempre na ordem do percurso. */
@Service
public class SerieService {

    private final SerieRepository series;

    public SerieService(SerieRepository series) {
        this.series = series;
    }

    public List<Serie> listar() {
        return series.listar().stream().sorted(Comparator.comparingInt(Serie::getOrdem)).toList();
    }
}
