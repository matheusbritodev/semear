package br.undb.semear.controller.academico;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.portal.PortalDtos.SerieResponse;
import br.undb.semear.service.academico.SerieService;

/** Séries da escola, usadas nos filtros da área da escola (ex.: listas de materiais). */
@RestController
@RequestMapping("/api/series")
public class SerieController {

    private final SerieService series;

    public SerieController(SerieService series) {
        this.series = series;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('MATRICULA', 'LER') or @permissoes.pode('TURMAS', 'LER')")
    public List<SerieResponse> listar() {
        return series.listar().stream()
                .map(s -> new SerieResponse(s.getId(), s.getNome(), s.getNivel(), s.getNivel().getRotulo(), s.getOrdem()))
                .toList();
    }
}
