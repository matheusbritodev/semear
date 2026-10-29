package br.undb.semear.repository.pessoa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.AlunoDoResponsavelResponse;
import br.undb.semear.dto.pessoa.ResponsavelDtos.ResponsavelResumoResponse;
import br.undb.semear.model.entity.pessoa.Responsavel;
import br.undb.semear.repository.base.Repository;

public interface ResponsavelRepository extends Repository<Responsavel, UUID> {

    /** Busca por nome (parcial) ou CPF (com ou sem máscara). */
    PaginaResponse<ResponsavelResumoResponse> pesquisar(String busca, int pagina, int tamanho);

    List<AlunoDoResponsavelResponse> listarAlunos(UUID responsavelId);

    /** Cadastro de responsável ligado a um login do portal. */
    Optional<Responsavel> buscarPorUsuario(UUID usuarioId);

    Optional<Responsavel> buscarPorCpf(String cpfDigitos);

    /** Liga o login ao cadastro (o banco exige que o usuário tenha perfil RESPONSAVEL). */
    void vincularUsuario(UUID responsavelId, UUID usuarioId);

    /** O aluno é filho (vínculo) deste responsável? Base do filtro do portal. */
    boolean estaVinculadoAoAluno(UUID responsavelId, UUID alunoId);
}
