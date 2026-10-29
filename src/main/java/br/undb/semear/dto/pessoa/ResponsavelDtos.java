package br.undb.semear.dto.pessoa;

import java.util.List;
import java.util.UUID;

import br.undb.semear.model.entity.pessoa.Responsavel;
import br.undb.semear.model.enums.StatusMatricula;
import jakarta.validation.constraints.Size;

/** Entrada e saída dos endpoints de responsáveis (/api/responsaveis). */
public final class ResponsavelDtos {

    private ResponsavelDtos() {
    }

    public record ResponsavelResumoResponse(UUID id, String nome, String cpf, String telefone, String email,
                                            int quantidadeAlunos, boolean possuiLogin) {
    }

    public record ResponsavelDetalheResponse(UUID id, String nome, String cpf, String telefone, String email,
                                             boolean possuiLogin, List<AlunoDoResponsavelResponse> alunos) {

        public static ResponsavelDetalheResponse de(Responsavel r, List<AlunoDoResponsavelResponse> alunos) {
            return new ResponsavelDetalheResponse(r.getId(), r.getNome(), r.getCpfObrigatorio().formatado(),
                    r.getTelefone().map(t -> t.formatado()).orElse(null),
                    r.getEmail().map(e -> e.getEndereco()).orElse(null),
                    r.possuiLogin(), alunos);
        }
    }

    public record AlunoDoResponsavelResponse(UUID alunoId, String nome, String parentesco, boolean financeiro,
                                             String turmaAtual, StatusMatricula statusMatricula,
                                             int documentosPendentes, int contratosPendentes) {
    }

    /** Campos vazios são aceitos, mas é preciso ao menos um contato (regra do Model). */
    public record AtualizarContatoRequest(@Size(max = 20) String telefone, @Size(max = 255) String email) {
    }
}
