package br.undb.semear.repository.financeiro;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.undb.semear.dto.PaginaResponse;
import br.undb.semear.model.entity.financeiro.DadosCobranca;
import br.undb.semear.model.entity.financeiro.ReferenciaCora;
import br.undb.semear.model.enums.OrigemPagamento;
import br.undb.semear.model.enums.StatusCobranca;
import br.undb.semear.model.enums.TipoCobranca;
import br.undb.semear.model.valueobject.Dinheiro;

public interface CobrancaRepository {

    /** Linha da cobrança com o contexto que as telas mostram (aluno, turma, responsável). */
    record CobrancaComContexto(DadosCobranca dados, UUID alunoId, long numeroCadastro, String aluno, int ano,
                               String turma, String responsavel, String telefone) {
    }

    /**
     * Filtros da listagem. Status é o real na data (PENDENTE que passou do vencimento conta como VENCIDO).
     * responsavelId restringe ao pagador (portal).
     */
    record Filtro(Integer ano, Integer mes, TipoCobranca tipo, StatusCobranca status, String busca, UUID matriculaId,
                  UUID responsavelId, int pagina, int tamanho) {
    }

    record ResumoMensal(LocalDate competencia, TipoCobranca tipo, int quantidade, Dinheiro previsto, Dinheiro descontos,
                        Dinheiro recebido, Dinheiro emAtraso) {
    }

    PaginaResponse<CobrancaComContexto> pesquisar(Filtro filtro, LocalDate hoje);

    Optional<CobrancaComContexto> buscar(UUID id);

    /** Em aberto com vencimento antes de hoje (inadimplência). */
    List<CobrancaComContexto> vencidas(LocalDate hoje);

    /**
     * Em aberto e ainda sem boleto no Cora, com as linhas travadas (FOR UPDATE SKIP LOCKED) até o fim da transação:
     * duas emissões ao mesmo tempo nunca pegam a mesma cobrança (evita boleto duplicado no banco).
     */
    List<CobrancaComContexto> travarSemBoleto(int limite);

    /** Devolve false se a cobrança já tinha boleto. */
    boolean vincularBoleto(UUID cobrancaId, ReferenciaCora boleto);

    Optional<UUID> buscarIdPorCora(String coraId);

    /** fn_registrar_pagamento com o valor calculado pelo Model (RN08/RN09). */
    void registrarPagamento(UUID cobrancaId, OrigemPagamento origem, Dinheiro valorPago, OffsetDateTime pagoEm);

    /** fn_gerar_cobrancas_periodo: devolve quantas cobranças novas foram criadas (RN01 evita duplicar). */
    int gerarPeriodo(int ano, int mesInicio, int mesFim);

    List<ResumoMensal> resumo(int ano);

    /** Feriados do calendário letivo (todos os anos), para os dias úteis da carência (RN09). */
    List<LocalDate> feriados();
}
