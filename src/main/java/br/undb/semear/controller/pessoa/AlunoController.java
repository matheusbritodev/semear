package br.undb.semear.controller.pessoa;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.AlunoDetalheResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.AlunoResumoResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.DefinirResponsavelFinanceiroRequest;
import br.undb.semear.dto.pessoa.AlunoDtos.VinculoResponse;
import br.undb.semear.model.entity.pessoa.Aluno;
import br.undb.semear.service.pessoa.AlunoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/alunos")
public class AlunoController {

    private final AlunoService alunos;

    public AlunoController(AlunoService alunos) {
        this.alunos = alunos;
    }

    @GetMapping
    @PreAuthorize("@permissoes.pode('ALUNOS', 'LER')")
    public PaginaResponse<AlunoResumoResponse> pesquisar(@RequestParam(required = false) String busca,
                                                         @RequestParam(required = false) Integer ano,
                                                         @RequestParam(required = false) UUID turmaId,
                                                         @RequestParam(defaultValue = "0") int pagina,
                                                         @RequestParam(defaultValue = "20") int tamanho) {
        return alunos.pesquisar(busca, ano, turmaId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissoes.pode('ALUNOS', 'LER')")
    public AlunoDetalheResponse detalhar(@PathVariable UUID id) {
        return paraResposta(alunos.buscar(id));
    }

    /** RD01: troca o responsável financeiro do aluno (quem recebe as cobranças). */
    @PutMapping("/{id}/responsavel-financeiro")
    @PreAuthorize("@permissoes.pode('ALUNOS', 'GRAVAR')")
    public AlunoDetalheResponse definirResponsavelFinanceiro(@PathVariable UUID id,
                                                             @Valid @RequestBody DefinirResponsavelFinanceiroRequest pedido) {
        return paraResposta(alunos.definirResponsavelFinanceiro(id, pedido.responsavelId()));
    }

    private AlunoDetalheResponse paraResposta(Aluno aluno) {
        var responsaveis = aluno.getVinculos().stream()
                .map(v -> new VinculoResponse(v.responsavel().getId(), v.responsavel().getNome(), v.parentesco(),
                        v.financeiro(),
                        v.responsavel().getTelefone().map(t -> t.formatado()).orElse(null),
                        v.responsavel().getEmail().map(e -> e.getEndereco()).orElse(null)))
                .sorted((a, b) -> Boolean.compare(b.financeiro(), a.financeiro()))
                .toList();
        return new AlunoDetalheResponse(aluno.getId(), aluno.getNumeroCadastro(), aluno.getNome(),
                aluno.getDataNascimento(), alunos.idade(aluno), aluno.isAtivo(), responsaveis,
                alunos.matriculas(aluno.getId()));
    }
}
