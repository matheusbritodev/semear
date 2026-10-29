package br.undb.semear.dto.pessoa;

import java.time.LocalDate;
import java.util.UUID;

import br.undb.semear.model.entity.pessoa.Funcionario;
import br.undb.semear.model.enums.TipoFuncionario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Entrada e saída dos endpoints de funcionários (/api/funcionarios). */
public final class FuncionarioDtos {

    private FuncionarioDtos() {
    }

    public record FuncionarioResponse(UUID id, String nome, String cpf, TipoFuncionario tipo, String funcao,
                                      boolean podeLecionar, String telefone, String email, LocalDate dataAdmissao,
                                      boolean ativo, boolean possuiLogin) {

        /** A função vem do polimorfismo: cada subclasse responde getFuncao() do seu jeito. */
        public static FuncionarioResponse de(Funcionario f) {
            return new FuncionarioResponse(f.getId(), f.getNome(),
                    f.getCpf().map(c -> c.formatado()).orElse(null),
                    f.getTipo(), f.getFuncao(), f.podeLecionar(),
                    f.getTelefone().map(t -> t.formatado()).orElse(null),
                    f.getEmail().map(e -> e.getEndereco()).orElse(null),
                    f.getDataAdmissao().orElse(null), f.isAtivo(), f.possuiLogin());
        }
    }

    /** Criação: tipo e CPF são obrigatórios e não mudam depois. */
    public record NovoFuncionarioRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o CPF") String cpf,
            @NotNull(message = "Escolha o tipo") TipoFuncionario tipo,
            @Size(max = 20) String telefone,
            @Size(max = 255) String email,
            @PastOrPresent(message = "A admissão não pode ser no futuro") LocalDate dataAdmissao) {
    }

    public record AtualizarFuncionarioRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @Size(max = 20) String telefone,
            @Size(max = 255) String email,
            @PastOrPresent(message = "A admissão não pode ser no futuro") LocalDate dataAdmissao) {
    }

    public record AlterarAtivoRequest(@NotNull Boolean ativo) {
    }
}
