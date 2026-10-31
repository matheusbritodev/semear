package br.undb.semear.model.entity.controle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Um campo que mudou num registro da auditoria. Em inclusão só há "depois"; em exclusão, só "antes".
 * atualizado_em fica de fora (muda sempre e não diz nada).
 */
public record AlteracaoCampo(String campo, String antes, String depois) {

    private static final int LIMITE = 160;

    public static List<AlteracaoCampo> entre(Map<String, Object> antes, Map<String, Object> depois) {
        Map<String, Object> a = antes == null ? Map.of() : antes;
        Map<String, Object> d = depois == null ? Map.of() : depois;
        TreeSet<String> campos = new TreeSet<>(a.keySet());
        campos.addAll(d.keySet());
        campos.remove("atualizado_em");
        List<AlteracaoCampo> mudancas = new ArrayList<>();
        for (String campo : campos) {
            Object va = a.get(campo);
            Object vd = d.get(campo);
            boolean inclusaoOuExclusao = antes == null || depois == null;
            if (inclusaoOuExclusao || !Objects.equals(va, vd)) {
                mudancas.add(new AlteracaoCampo(campo, texto(va, antes != null), texto(vd, depois != null)));
            }
        }
        return mudancas;
    }

    private static String texto(Object valor, boolean existe) {
        if (!existe) {
            return null;
        }
        String t = valor == null ? "—" : valor.toString();
        return t.length() > LIMITE ? t.substring(0, LIMITE) + "…" : t;
    }
}
