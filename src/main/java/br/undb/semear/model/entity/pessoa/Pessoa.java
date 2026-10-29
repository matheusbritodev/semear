package br.undb.semear.model.entity.pessoa;

import java.util.Optional;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.valueobject.Cpf;

/**
 * Superclasse abstrata de quem a escola atende ou emprega (decisão D04:
 * herança só no Java; no banco são três tabelas: aluno, responsavel, funcionario).
 */
public abstract class Pessoa extends Entidade {

    private String nome;
    private final Cpf cpf;

    protected Pessoa(UUID id, String nome, Cpf cpf) {
        super(id);
        this.nome = normalizarNome(nome);
        this.cpf = cpf;
    }

    /** Papel da pessoa na escola (polimórfico): "Aluno(a)", "Responsável", "Professor(a)"... */
    public abstract String descreverPapel();

    public void renomear(String novoNome) {
        this.nome = normalizarNome(novoNome);
    }

    public String getNome() {
        return nome;
    }

    public Optional<Cpf> getCpf() {
        return Optional.ofNullable(cpf);
    }

    public String getPrimeiroNome() {
        return nome.split(" ")[0];
    }

    private static String normalizarNome(String nome) {
        String limpo = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (limpo.length() < 3 || limpo.length() > 150) {
            throw new DadoInvalidoException("nome", "Informe o nome completo (3 a 150 caracteres)");
        }
        return limpo;
    }

    /** Sem dados pessoais (LGPD): só o papel e o id. */
    @Override
    public String toString() {
        return descreverPapel() + "[id=" + getId() + "]";
    }
}
