package br.undb.semear.model.entity.controle;

/**
 * Confere a trilha de auditoria registro a registro, em ordem de id (pode ser alimentada em páginas).
 * Cada registro precisa (1) apontar para o hash do anterior e (2) ter o hash que bate com o próprio conteúdo.
 * Se alguém alterar uma linha, o item 2 falha; se apagar ou inserir uma linha no meio, o item 1 falha.
 */
public final class CadeiaAuditoria {

    public static final String HASH_INICIAL = "0".repeat(64);

    /** Onde a cadeia quebrou. */
    public record Falha(long registroId, String tipo, String mensagem) {
    }

    public record Resultado(boolean integra, long verificados, long ultimoId, String ultimoHash, Falha falha) {
    }

    private String ultimoHash = HASH_INICIAL;
    private long ultimoId;
    private long verificados;
    private Falha falha;

    /** Confere o próximo registro. Depois da primeira falha, não confere mais nada (devolve false). */
    public boolean conferir(RegistroAuditoria registro) {
        if (falha != null) {
            return false;
        }
        if (!registro.hashAnterior().equals(ultimoHash)) {
            falha = new Falha(registro.id(), "ELO_QUEBRADO", "O registro #" + registro.id()
                    + " não aponta para o anterior: algum registro foi apagado ou inserido fora da ordem");
        } else if (!registro.hashCalculado().equals(registro.hash())) {
            falha = new Falha(registro.id(), "CONTEUDO_ALTERADO", "O conteúdo do registro #" + registro.id()
                    + " não confere com o hash: foi alterado depois de gravado");
        } else {
            ultimoHash = registro.hash();
            ultimoId = registro.id();
            verificados++;
        }
        return falha == null;
    }

    /**
     * Âncora guardada numa revisão anterior (último id e hash naquele dia). Se o registro sumiu ou o hash mudou,
     * a trilha foi cortada ou reescrita a partir dali — o que a cadeia sozinha não percebe quando o fim é apagado.
     */
    public boolean conferirAncora(long id, String hashGuardado, String hashAtual) {
        if (falha != null) {
            return false;
        }
        if (hashAtual == null || !hashAtual.equals(hashGuardado)) {
            falha = new Falha(id, "ANCORA_DIVERGENTE", "O registro #" + id
                    + ", conferido em uma revisão anterior, sumiu ou mudou: a trilha foi cortada ou reescrita");
        }
        return falha == null;
    }

    public Resultado resultado() {
        return new Resultado(falha == null, verificados, ultimoId, ultimoHash, falha);
    }
}
