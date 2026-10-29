package br.undb.semear.model.entity.matricula;

import java.time.LocalDate;
import java.time.Period;

import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Cpf;

/**
 * Dados da criança informados na pré-matrícula, antes de existir cadastro de aluno.
 * Value object: valida na criação e não muda.
 */
public record DadosCrianca(String nome, LocalDate dataNascimento, Cpf cpf) {

    /** Público da escola: Maternal 1 (2 anos) ao 5º ano (~11 anos), com folga. */
    public static final int IDADE_MINIMA = 1;
    public static final int IDADE_MAXIMA = 13;

    public DadosCrianca {
        nome = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (nome.length() < 5 || !nome.contains(" ") || nome.length() > 150) {
            throw new DadoInvalidoException("alunoNome", "Informe o nome completo da criança");
        }
        if (dataNascimento == null) {
            throw new DadoInvalidoException("alunoNascimento", "Informe a data de nascimento");
        }
    }

    /** Idade no início do ano letivo, para checar se faz sentido para a escola. */
    public void validarIdadePara(LocalDate inicioAnoLetivo) {
        int idade = Period.between(dataNascimento, inicioAnoLetivo).getYears();
        if (dataNascimento.isAfter(inicioAnoLetivo) || idade < IDADE_MINIMA || idade > IDADE_MAXIMA) {
            throw new DadoInvalidoException("alunoNascimento",
                    "A idade da criança não é compatível com as séries da escola (Maternal ao 5º ano)");
        }
    }
}
