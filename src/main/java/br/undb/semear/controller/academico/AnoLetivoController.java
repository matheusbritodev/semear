package br.undb.semear.controller.academico;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.academico.AcademicoDtos.AnoLetivoRequest;
import br.undb.semear.dto.academico.AcademicoDtos.AnoLetivoResponse;
import br.undb.semear.service.academico.AnoLetivoService;

@RestController
@RequestMapping("/api/anos-letivos")
public class AnoLetivoController {

    private final AnoLetivoService anosLetivos;

    public AnoLetivoController(AnoLetivoService anosLetivos) {
        this.anosLetivos = anosLetivos;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('TURMAS', 'LER') or @permissoes.pode('ALUNOS', 'LER')")
    public List<AnoLetivoResponse> listar() {
        LocalDate hoje = anosLetivos.hoje();
        return anosLetivos.listar().stream().map(a -> AnoLetivoResponse.de(a, hoje)).toList();
    }

    /** Ano de referência das telas (em andamento ou o último iniciado). */
    @GetMapping("/corrente")
    @PreAuthorize("@permissoes.pode('TURMAS', 'LER') or @permissoes.pode('ALUNOS', 'LER')")
    public AnoLetivoResponse corrente() {
        return AnoLetivoResponse.de(anosLetivos.corrente(), anosLetivos.hoje());
    }

    /** Encerra o ano (matrículas ATIVAS → ENCERRADAS). Só depois do último dia letivo. */
    @PostMapping("/{ano}/encerrar")
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public Map<String, Integer> encerrar(@PathVariable int ano) {
        return Map.of("encerradas", anosLetivos.encerrar(ano));
    }

    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public AnoLetivoResponse criar(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody AnoLetivoRequest pedido) {
        return AnoLetivoResponse.de(anosLetivos.criar(pedido.ano(), pedido.dataInicio(), pedido.dataFim()), anosLetivos.hoje());
    }

    @org.springframework.web.bind.annotation.PutMapping("/{ano}")
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public AnoLetivoResponse alterarDatas(@PathVariable int ano,
                                          @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody AnoLetivoRequest pedido) {
        return AnoLetivoResponse.de(anosLetivos.alterarDatas(ano, pedido.dataInicio(), pedido.dataFim()), anosLetivos.hoje());
    }
}
