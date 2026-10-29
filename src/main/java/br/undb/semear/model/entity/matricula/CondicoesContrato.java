package br.undb.semear.model.entity.matricula;

import java.time.LocalDate;

import br.undb.semear.model.valueobject.Dinheiro;

/**
 * Dados que entram no texto do contrato. Os valores vêm sempre da tabela de preços da série e do ano
 * (nunca digitados no contrato — MODELO.md, ponto de atenção 2).
 */
public record CondicoesContrato(String aluno, long numeroCadastro, String serie, String turma, int ano,
                                LocalDate inicioAno, LocalDate fimAno, Dinheiro valorMensal, Dinheiro valorMatricula,
                                int diaVencimento, String responsavelFinanceiro) {
}
