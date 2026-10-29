package br.undb.semear.model.enums;

/**
 * Os 7 documentos da matrícula (RN12, MODELO.md §3). Gravado como texto em documento_aluno.tipo.
 * A declaração de adimplência só é exigida de quem vem de outra escola particular, por isso não é obrigatória.
 */
public enum TipoDocumentoAluno {

    IDENTIDADE_RESPONSAVEIS("Identidade e CPF dos pais/responsáveis", true),
    CERTIDAO_NASCIMENTO("Certidão de nascimento (ou RG e CPF) da criança", true),
    CARTEIRA_VACINACAO("Carteira de vacinação", true),
    COMPROVANTE_RESIDENCIA("Comprovante de residência", true),
    DECLARACAO_ADIMPLENCIA("Declaração de adimplência (se vier de escola particular)", false),
    FOTOS_3X4("2 fotos 3x4", true),
    AUTORIZADOS_BUSCAR("Documento das pessoas autorizadas a buscar a criança", true);

    private final String rotulo;
    private final boolean obrigatorio;

    TipoDocumentoAluno(String rotulo, boolean obrigatorio) {
        this.rotulo = rotulo;
        this.obrigatorio = obrigatorio;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean isObrigatorio() {
        return obrigatorio;
    }
}
