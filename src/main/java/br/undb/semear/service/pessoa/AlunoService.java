package br.undb.semear.service.pessoa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.AlunoResumoResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.MatriculaHistoricoResponse;
import br.undb.semear.model.entity.pessoa.Aluno;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.pessoa.AlunoRepository;
import br.undb.semear.security.EscopoAcesso;
import br.undb.semear.service.academico.AnoLetivoService;

@Service
public class AlunoService {

    public static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final AlunoRepository alunos;
    private final AnoLetivoService anosLetivos;
    private final EscopoAcesso escopo;

    public AlunoService(AlunoRepository alunos, AnoLetivoService anosLetivos, EscopoAcesso escopo) {
        this.alunos = alunos;
        this.anosLetivos = anosLetivos;
        this.escopo = escopo;
    }

    public PaginaResponse<AlunoResumoResponse> pesquisar(String busca, Integer ano, UUID turmaId, int pagina, int tamanho) {
        return alunos.pesquisar(busca, anosLetivos.anoOuCorrente(ano), turmaId,
                escopo.professorRestrito().orElse(null),
                Math.max(0, pagina), Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA));
    }

    public Aluno buscar(UUID id) {
        Aluno aluno = alunos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Aluno"));
        Optional<UUID> professor = escopo.professorRestrito();
        if (professor.isPresent() && !alunos.estudaComProfessor(id, professor.get())) {
            throw new AcessoNegadoException("ALUNO_DE_OUTRA_TURMA", "Você só pode acessar alunos das suas turmas");
        }
        return aluno;
    }

    public List<MatriculaHistoricoResponse> matriculas(UUID alunoId) {
        return alunos.listarMatriculas(alunoId);
    }

    /** RD01: a regra fica no Aluno; aqui só se carrega, aplica e grava em uma transação. */
    @Transactional
    public Aluno definirResponsavelFinanceiro(UUID alunoId, UUID responsavelId) {
        Aluno aluno = buscar(alunoId);
        aluno.definirResponsavelFinanceiro(responsavelId);
        return alunos.salvar(aluno);
    }

    public int idade(Aluno aluno) {
        return aluno.idadeEm(anosLetivos.hoje());
    }
}
