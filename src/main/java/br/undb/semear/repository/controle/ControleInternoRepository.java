package br.undb.semear.repository.controle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de apoio ao conflito de interesse: CPF de quem age e CPFs da família do aluno. */
public interface ControleInternoRepository {

    /** CPF do cadastro de funcionário ligado ao login (vazio se não houver cadastro). */
    Optional<String> cpfDoUsuario(UUID usuarioId);

    /** CPFs de todos os responsáveis do aluno da matrícula. */
    List<String> cpfsDaFamiliaDaMatricula(UUID matriculaId);

    /** CPFs de todos os responsáveis do aluno da cobrança. */
    List<String> cpfsDaFamiliaDaCobranca(UUID cobrancaId);

    /** "Nome do aluno (Turma, ano)", para o resumo dos pedidos. */
    Optional<String> descricaoMatricula(UUID matriculaId);

    /** "Série ano", para o resumo dos pedidos de preço. */
    Optional<String> nomeSerie(UUID serieId);
}
