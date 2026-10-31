package br.undb.semear.service.relatorio;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.AlunoDtos.AlunoResumoResponse;
import br.undb.semear.dto.relatorio.RelatorioDtos.RelatorioAlunosResponse;
import br.undb.semear.model.entity.pessoa.Usuario;
import br.undb.semear.model.entity.relatorio.RelatorioAlunos;
import br.undb.semear.repository.pessoa.AlunoRepository;
import br.undb.semear.security.EscopoAcesso;
import br.undb.semear.security.UsuarioLogado;
import br.undb.semear.service.academico.AnoLetivoService;
import br.undb.semear.service.pessoa.UsuarioService;

/**
 * Relatórios com os dados cadastrados. O relatório de alunos usa a mesma consulta da lista de alunos,
 * com o mesmo filtro de dono do dado: o professor só vê as próprias turmas.
 */
@Service
public class RelatorioService {

    /** Busca em páginas para não depender de um limite fixo de alunos. */
    private static final int PAGINA = 500;

    private final AlunoRepository alunos;
    private final AnoLetivoService anosLetivos;
    private final EscopoAcesso escopo;
    private final UsuarioLogado usuarioLogado;
    private final UsuarioService usuarios;
    private final Clock relogio;

    public RelatorioService(AlunoRepository alunos, AnoLetivoService anosLetivos, EscopoAcesso escopo,
                            UsuarioLogado usuarioLogado, UsuarioService usuarios, Clock relogio) {
        this.alunos = alunos;
        this.anosLetivos = anosLetivos;
        this.escopo = escopo;
        this.usuarioLogado = usuarioLogado;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    public RelatorioAlunosResponse alunos(Integer anoInformado, UUID turmaId) {
        int ano = anosLetivos.anoOuCorrente(anoInformado);
        UUID professor = escopo.professorRestrito().orElse(null);
        List<AlunoResumoResponse> todos = new ArrayList<>();
        int pagina = 0;
        PaginaResponse<AlunoResumoResponse> lote;
        do {
            lote = alunos.pesquisar(null, ano, turmaId, professor, pagina++, PAGINA);
            todos.addAll(lote.itens());
        } while (todos.size() < lote.total() && !lote.itens().isEmpty());

        RelatorioAlunos relatorio = new RelatorioAlunos(ano, todos.stream()
                .map(a -> new RelatorioAlunos.Linha(a.numeroCadastro() == null ? 0 : a.numeroCadastro(), a.nome(),
                        a.dataNascimento(), a.idade(), a.turma(), a.statusMatricula(), a.responsavelFinanceiro(),
                        a.documentosPendentes()))
                .toList());
        String turma = turmaId == null ? null : todos.stream().findFirst().map(AlunoResumoResponse::turma).orElse(null);
        Usuario eu = usuarioLogado.exigir();
        String emissor = usuarios.nomeExibicao(eu.getId()).orElse(eu.getEmail().getEndereco());
        return RelatorioAlunosResponse.de(relatorio, turma, OffsetDateTime.now(relogio), emissor);
    }
}
