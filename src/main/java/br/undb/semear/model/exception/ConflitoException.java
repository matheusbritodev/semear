package br.undb.semear.model.exception;

/** Operação conflita com o estado atual (turma lotada, cobrança já paga...). HTTP 409. */
public class ConflitoException extends SemearException {

    public ConflitoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }

    public ConflitoException(String codigo, String mensagem, Throwable causa) {
        super(codigo, mensagem, causa);
    }

    @Override
    public int getStatusHttp() {
        return 409;
    }
}
