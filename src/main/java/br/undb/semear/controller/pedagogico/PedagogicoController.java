package br.undb.semear.controller.pedagogico;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.pedagogico.PedagogicoDtos.ChamadaResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.DiarioRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.DiarioResumoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.EventoRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.EventoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.FormacaoRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.FormacaoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.FrequenciaAlunoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.LancarNotasRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.NotasTurmaResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.PlanoRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.PlanoResponse;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.TurmaPedagogicaResponse;
import br.undb.semear.service.pedagogico.CalendarioService;
import br.undb.semear.service.pedagogico.FormacaoService;
import br.undb.semear.service.pedagogico.PedagogicoService;
import jakarta.validation.Valid;

/**
 * Pedagógico (RF08). Ler: módulo PEDAGOGICO (professor só nas suas turmas).
 * Escrever diário, planos e notas: PEDAGOGICO:G e estar vinculado à turma (RD07).
 */
@RestController
@RequestMapping("/api")
public class PedagogicoController {

    private final PedagogicoService pedagogico;
    private final CalendarioService calendario;
    private final FormacaoService formacoes;

    public PedagogicoController(PedagogicoService pedagogico, CalendarioService calendario, FormacaoService formacoes) {
        this.pedagogico = pedagogico;
        this.calendario = calendario;
        this.formacoes = formacoes;
    }

    @GetMapping("/pedagogico/turmas")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'LER')")
    public List<TurmaPedagogicaResponse> turmas(@RequestParam(required = false) Integer ano) {
        return pedagogico.turmas(ano);
    }

    /* ---- Diário ---- */

    @GetMapping("/turmas/{id}/chamada")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'LER')")
    public ChamadaResponse chamada(@PathVariable UUID id,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return pedagogico.chamada(id, data);
    }

    /** Grava (ou refaz) o diário do professor logado no dia: conteúdo e chamada completa. */
    @PutMapping("/turmas/{id}/diarios/{data}")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'GRAVAR')")
    public ChamadaResponse registrarDiario(@PathVariable UUID id,
                                           @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                           @Valid @RequestBody DiarioRequest pedido) {
        return pedagogico.registrarDiario(id, data, pedido);
    }

    @GetMapping("/turmas/{id}/diarios")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'LER')")
    public List<DiarioResumoResponse> diarios(@PathVariable UUID id,
                                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return pedagogico.diarios(id, mes);
    }

    @GetMapping("/turmas/{id}/frequencia")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'LER')")
    public List<FrequenciaAlunoResponse> frequencia(@PathVariable UUID id) {
        return pedagogico.frequencia(id);
    }

    /* ---- Planos ---- */

    @GetMapping("/turmas/{id}/planos")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'LER')")
    public List<PlanoResponse> planos(@PathVariable UUID id,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return pedagogico.planos(id, de, ate);
    }

    @PostMapping("/turmas/{id}/planos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'GRAVAR')")
    public Map<String, UUID> criarPlano(@PathVariable UUID id, @Valid @RequestBody PlanoRequest pedido) {
        return Map.of("id", pedagogico.criarPlano(id, pedido));
    }

    @PutMapping("/planos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'GRAVAR')")
    public void alterarPlano(@PathVariable UUID id, @Valid @RequestBody PlanoRequest pedido) {
        pedagogico.alterarPlano(id, pedido);
    }

    @DeleteMapping("/planos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'GRAVAR')")
    public void removerPlano(@PathVariable UUID id) {
        pedagogico.removerPlano(id);
    }

    /* ---- Notas ---- */

    @GetMapping("/turmas/{id}/notas")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'LER')")
    public NotasTurmaResponse notas(@PathVariable UUID id, @RequestParam(required = false) UUID disciplinaId,
                                    @RequestParam(required = false) Integer bimestre) {
        return pedagogico.notas(id, disciplinaId, bimestre);
    }

    @PutMapping("/turmas/{id}/notas")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'GRAVAR')")
    public NotasTurmaResponse lancarNotas(@PathVariable UUID id, @Valid @RequestBody LancarNotasRequest pedido) {
        return pedagogico.lancarNotas(id, pedido);
    }

    /* ---- Calendário (alterar: Configurações) ---- */

    @GetMapping("/calendario")
    @PreAuthorize("@permissoes.pode('PEDAGOGICO', 'LER') or @permissoes.pode('CONFIGURACOES', 'LER')")
    public List<EventoResponse> calendario(@RequestParam(required = false) Integer ano) {
        return calendario.eventos(ano);
    }

    @PostMapping("/calendario")
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public List<EventoResponse> criarEvento(@Valid @RequestBody EventoRequest pedido) {
        return calendario.criar(pedido);
    }

    @DeleteMapping("/calendario/{id}")
    @PreAuthorize("@permissoes.pode('CONFIGURACOES', 'GRAVAR')")
    public List<EventoResponse> removerEvento(@PathVariable UUID id) {
        return calendario.remover(id);
    }

    /* ---- Formação continuada (RH) ---- */

    @GetMapping("/funcionarios/{id}/formacoes")
    @PreAuthorize("@permissoes.pode('RH', 'LER')")
    public List<FormacaoResponse> formacoes(@PathVariable UUID id) {
        return formacoes.doFuncionario(id);
    }

    @PostMapping("/funcionarios/{id}/formacoes")
    @PreAuthorize("@permissoes.pode('RH', 'GRAVAR')")
    public List<FormacaoResponse> registrarFormacao(@PathVariable UUID id, @Valid @RequestBody FormacaoRequest pedido) {
        return formacoes.registrar(id, pedido);
    }

    @DeleteMapping("/funcionarios/{id}/formacoes/{formacaoId}")
    @PreAuthorize("@permissoes.pode('RH', 'GRAVAR')")
    public List<FormacaoResponse> removerFormacao(@PathVariable UUID id, @PathVariable UUID formacaoId) {
        return formacoes.remover(id, formacaoId);
    }
}
