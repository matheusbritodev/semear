package br.undb.semear.service.pessoa;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.AlunoDoResponsavelResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.ResponsavelResumoResponse;
import br.undb.semear.model.entity.pessoa.Responsavel;
import br.undb.semear.model.enums.PerfilUsuario;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Telefone;
import br.undb.semear.repository.pessoa.ResponsavelRepository;
import br.undb.semear.security.EscopoAcesso;

/** Professor não acessa o cadastro geral de famílias (CPF e contatos de todos): LGPD. */
@Service
public class ResponsavelService {

    private final ResponsavelRepository responsaveis;
    private final EscopoAcesso escopo;

    public ResponsavelService(ResponsavelRepository responsaveis, EscopoAcesso escopo) {
        this.responsaveis = responsaveis;
        this.escopo = escopo;
    }

    public PaginaResponse<ResponsavelResumoResponse> pesquisar(String busca, int pagina, int tamanho) {
        bloquearProfessor();
        return responsaveis.pesquisar(busca, Math.max(0, pagina), Math.clamp(tamanho, 1, AlunoService.TAMANHO_MAXIMO_PAGINA));
    }

    public Responsavel buscar(UUID id) {
        bloquearProfessor();
        return responsaveis.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Responsável"));
    }

    public List<AlunoDoResponsavelResponse> alunos(UUID responsavelId) {
        return responsaveis.listarAlunos(responsavelId);
    }

    /** Os value objects validam o formato; o Responsavel exige ao menos um contato. */
    @Transactional
    public Responsavel atualizarContato(UUID id, String telefone, String email) {
        Responsavel responsavel = buscar(id);
        responsavel.atualizarContato(Telefone.opcional(telefone), Email.opcional(email));
        return responsaveis.salvar(responsavel);
    }

    private void bloquearProfessor() {
        if (escopo.ehPerfil(PerfilUsuario.PROFESSOR)) {
            throw new AcessoNegadoException("ACESSO_NEGADO", "O cadastro de responsáveis é restrito à secretaria");
        }
    }
}
