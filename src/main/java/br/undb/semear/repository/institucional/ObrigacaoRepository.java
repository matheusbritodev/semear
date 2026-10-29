package br.undb.semear.repository.institucional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.institucional.ComprovanteObrigacao;
import br.undb.semear.model.entity.institucional.ObrigacaoInstitucional;

public interface ObrigacaoRepository {

    record ObrigacaoComContexto(ObrigacaoInstitucional obrigacao, String responsavel, List<ComprovanteObrigacao> comprovantes,
                                String cumpridaPor) {
    }

    record Funcionario(UUID id, String nome) {
    }

    List<ObrigacaoComContexto> listar();

    Optional<ObrigacaoComContexto> buscar(UUID id);

    /** Insere ou atualiza; o status gravado acompanha (CUMPRIDA, ATRASADA ou PENDENTE). Devolve o id. */
    UUID salvar(ObrigacaoInstitucional obrigacao);

    /** fn_cumprir_obrigacao: registra hoje e programa o próximo vencimento; guarda quem marcou. */
    void cumprir(UUID id, UUID cumpridaPor);

    void remover(UUID id);

    UUID inserirComprovante(ComprovanteObrigacao comprovante);

    Optional<ComprovanteObrigacao> comprovante(UUID id);

    void removerComprovante(UUID id);

    /** Funcionários ativos que podem responder por uma obrigação. */
    List<Funcionario> funcionariosAtivos();
}
