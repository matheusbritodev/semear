package br.undb.semear.integration.notificacao;

import java.time.format.DateTimeFormatter;

import br.undb.semear.model.entity.financeiro.NotificacaoCobranca;

/** Texto do aviso ao responsável, igual para qualquer canal. */
public final class MensagemCobranca {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private MensagemCobranca() {
    }

    public static String para(NotificacaoCobranca n) {
        String primeiroNome = n.responsavel() == null ? "" : n.responsavel().split(" ")[0];
        String vencimento = n.vencimento().format(DATA);
        if (n.ehInadimplencia()) {
            return "Olá, " + primeiroNome + ". A cobrança de " + n.aluno() + " (" + n.valorFinal() + ") venceu em "
                    + vencimento + " e ainda não consta como paga. Acesse o Portal do Responsável para pagar "
                    + "ou fale com a secretaria. Centro Educacional Semeando.";
        }
        return "Olá, " + primeiroNome + ". Lembrete: a cobrança de " + n.aluno() + " (" + n.valorFinal() + ") vence em "
                + vencimento + ". Pagando até o vencimento, a mensalidade tem 10% de desconto. Centro Educacional Semeando.";
    }
}
