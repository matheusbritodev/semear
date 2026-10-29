package br.undb.semear.controller.advice;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de erro da API. A View escolhe a mensagem pelo "codigo";
 * "campos" traz os erros de validação de cada campo do formulário.
 */
public record ErroResponse(int status, String codigo, String mensagem, List<CampoErro> campos, Instant instante) {

    public record CampoErro(String campo, String mensagem) {
    }

    public static ErroResponse de(int status, String codigo, String mensagem) {
        return new ErroResponse(status, codigo, mensagem, List.of(), Instant.now());
    }

    public static ErroResponse de(int status, String codigo, String mensagem, List<CampoErro> campos) {
        return new ErroResponse(status, codigo, mensagem, campos, Instant.now());
    }
}
