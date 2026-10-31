package br.undb.semear.model.enums;

/** Módulos da matriz de permissões (coluna perfil_permissao.modulo). */
public enum Modulo {
    CONFIGURACOES,
    USUARIOS,
    MATRICULA,
    ALUNOS,
    TURMAS,
    DOCUMENTOS,
    FINANCEIRO,
    BOLSAS,
    RH,
    PEDAGOGICO,
    OBRIGACOES,
    PORTAL,
    /** Painel geral de pendências dos alunos (documentos), pedido pelo professor em 28/10/2026. */
    PENDENCIAS,
    /** Caixa de aprovações: decidir pedidos que exigem uma segunda pessoa (SoD, 31/10/2026). */
    APROVACOES,
    /** Trilha de auditoria e verificação de integridade (SOX). Somente leitura. */
    AUDITORIA
}
