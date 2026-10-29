package br.undb.semear.model.enums;

/** Espelha o enum perfil_usuario do banco (mesmos nomes de constantes). */
public enum PerfilUsuario {
    ADMIN,
    SECRETARIA,
    FINANCEIRO,
    PROFESSOR,
    RESPONSAVEL,
    ALUNO;

    /** RESPONSAVEL e ALUNO usam o Portal; os demais, a Área da escola. */
    public boolean usaPortal() {
        return this == RESPONSAVEL || this == ALUNO;
    }
}
