package br.undb.semear.model.entity.pessoa;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Cpf;

/**
 * Cadastro único do aluno (RN03): a renovação anual é uma nova matrícula, nunca um novo aluno.
 * O aluno compõe seus vínculos com responsáveis e garante que exista um único responsável financeiro (RD01).
 */
public class Aluno extends Pessoa {

    private final Long numeroCadastro;
    private final LocalDate dataNascimento;
    private boolean ativo;
    private final List<VinculoResponsavel> vinculos = new ArrayList<>();

    public Aluno(UUID id, Long numeroCadastro, String nome, LocalDate dataNascimento, Cpf cpf, boolean ativo) {
        super(id, nome, cpf);
        this.numeroCadastro = numeroCadastro;
        this.dataNascimento = validarNascimento(dataNascimento);
        this.ativo = ativo;
    }

    private static LocalDate validarNascimento(LocalDate data) {
        if (data == null || data.isAfter(LocalDate.now())) {
            throw new DadoInvalidoException("dataNascimento", "Data de nascimento inválida");
        }
        return data;
    }

    @Override
    public String descreverPapel() {
        return "Aluno(a)";
    }

    /** RN02: vários responsáveis por aluno. RD01: só um pode ser o financeiro. */
    public void vincularResponsavel(Responsavel responsavel, String parentesco, boolean financeiro) {
        if (estaVinculado(responsavel.getId())) {
            throw new RegraNegocioException("RESPONSAVEL_JA_VINCULADO", "Este responsável já está vinculado ao aluno");
        }
        if (financeiro) {
            vinculos.replaceAll(v -> v.comFinanceiro(false));
        }
        vinculos.add(new VinculoResponsavel(responsavel, parentesco, financeiro));
    }

    /** RD01: torna o responsável indicado o único financeiro do aluno. */
    public void definirResponsavelFinanceiro(UUID responsavelId) {
        if (!estaVinculado(responsavelId)) {
            throw new RegraNegocioException("RESPONSAVEL_NAO_VINCULADO",
                    "Só um responsável vinculado ao aluno pode ser o responsável financeiro");
        }
        vinculos.replaceAll(v -> v.comFinanceiro(v.responsavel().getId().equals(responsavelId)));
    }

    public boolean estaVinculado(UUID responsavelId) {
        return vinculos.stream().anyMatch(v -> Objects.equals(v.responsavel().getId(), responsavelId));
    }

    public Optional<Responsavel> getResponsavelFinanceiro() {
        return vinculos.stream().filter(VinculoResponsavel::financeiro).map(VinculoResponsavel::responsavel).findFirst();
    }

    public int idadeEm(LocalDate data) {
        return Period.between(dataNascimento, data).getYears();
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public Long getNumeroCadastro() {
        return numeroCadastro;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public List<VinculoResponsavel> getVinculos() {
        return List.copyOf(vinculos);
    }
}
