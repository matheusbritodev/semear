package br.undb.semear.model.exception;


/** RN04: a turma atingiu a capacidade. Mesmo código que o banco devolve (TURMA_LOTADA). */
public class TurmaCheiaException extends ConflitoException {

    public TurmaCheiaException(String turma, int capacidade) {
        super("TURMA_LOTADA", "A turma " + turma + " atingiu o limite de " + capacidade + " alunos");
    }
}
