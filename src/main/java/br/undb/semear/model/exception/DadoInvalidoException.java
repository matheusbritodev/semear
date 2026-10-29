package br.undb.semear.model.exception;

/** Valor inválido para um campo (CPF, e-mail, telefone...). HTTP 422. */
public class DadoInvalidoException extends RegraNegocioException {

    private final String campo;

    public DadoInvalidoException(String campo, String mensagem) {
        super("DADO_INVALIDO", mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
