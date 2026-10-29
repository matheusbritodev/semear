package br.undb.semear.repository.matricula;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.undb.semear.dto.matricula.MatriculaDtos.PreMatriculaResumoResponse;
import br.undb.semear.model.entity.academico.Serie;
import br.undb.semear.model.entity.matricula.DadosCrianca;
import br.undb.semear.model.entity.matricula.PreMatricula;
import br.undb.semear.model.enums.NivelEnsino;
import br.undb.semear.model.enums.StatusPreMatricula;
import br.undb.semear.model.enums.TipoFarda;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.valueobject.Telefone;
import br.undb.semear.repository.base.Colunas;
import br.undb.semear.repository.base.JdbcRepository;
import br.undb.semear.repository.base.TradutorErroBanco;

@Repository
public class PreMatriculaJdbcRepository extends JdbcRepository<PreMatricula> implements PreMatriculaRepository {

    private static final String SELECT_RESUMO = """
            SELECT p.id, p.criado_em, al.ano, s.nome AS serie, s.nivel, p.aluno_nome, p.aluno_nascimento,
                   r.nome AS responsavel, r.telefone, p.status, p.motivo_rejeicao,
                   (SELECT m.id FROM matricula m WHERE m.pre_matricula_id = p.id) AS matricula_id
              FROM pre_matricula p
              JOIN serie s       ON s.id = p.serie_id
              JOIN ano_letivo al ON al.id = p.ano_letivo_id
              JOIN responsavel r ON r.id = p.responsavel_id
            """;

    private final Clock relogio;

    public PreMatriculaJdbcRepository(JdbcClient jdbc, TradutorErroBanco tradutor, Clock relogio) {
        super(jdbc, tradutor, "pre_matricula");
        this.relogio = relogio;
    }

    @Override
    protected String selectBase() {
        return """
                SELECT p.*, al.ano, s.nome AS serie_nome, s.nivel, s.ordem
                  FROM pre_matricula p
                  JOIN serie s       ON s.id = p.serie_id
                  JOIN ano_letivo al ON al.id = p.ano_letivo_id
                """;
    }

    @Override
    protected String colunaId() {
        return "p.id";
    }

    @Override
    protected String ordenacao() {
        return "ORDER BY p.criado_em";
    }

    @Override
    protected PreMatricula mapear(ResultSet linha, int numeroLinha) throws SQLException {
        Map<TipoFarda, String> fardas = new EnumMap<>(TipoFarda.class);
        fardas.put(TipoFarda.UNIFORME, linha.getString("farda_uniforme_tam"));
        fardas.put(TipoFarda.EDUCACAO_FISICA, linha.getString("farda_ed_fisica_tam"));
        return new PreMatricula(
                Colunas.uuid(linha, "id"),
                linha.getInt("ano"),
                new Serie(Colunas.uuid(linha, "serie_id"), linha.getString("serie_nome"),
                        NivelEnsino.valueOf(linha.getString("nivel")), linha.getInt("ordem")),
                Colunas.uuid(linha, "responsavel_id"),
                linha.getString("parentesco"),
                new DadosCrianca(linha.getString("aluno_nome"), Colunas.data(linha, "aluno_nascimento"),
                        Colunas.cpf(linha, "aluno_cpf")),
                fardas,
                StatusPreMatricula.valueOf(linha.getString("status")),
                linha.getString("motivo_rejeicao"),
                Colunas.uuid(linha, "aluno_id"),
                linha.getObject("criado_em", OffsetDateTime.class));
    }

    @Override
    public PreMatricula salvar(PreMatricula pre) {
        if (pre.estaPersistida()) {
            throw new RegraNegocioException("PRE_MATRICULA_IMUTAVEL",
                    "Pré-matrículas não são editadas; a decisão é feita pela aprovação ou rejeição");
        }
        UUID id = executar(() -> jdbc.sql("""
                        INSERT INTO pre_matricula (ano_letivo_id, serie_id, responsavel_id, parentesco,
                                                   aluno_nome, aluno_nascimento, aluno_cpf,
                                                   farda_uniforme_tam, farda_ed_fisica_tam)
                        VALUES ((SELECT id FROM ano_letivo WHERE ano = :ano), :serie, :responsavel, :parentesco,
                                :nome, :nascimento, :cpf, :uniforme, :edFisica)
                        RETURNING id
                        """)
                .param("ano", pre.getAnoLetivo())
                .param("serie", pre.getSerie().getId())
                .param("responsavel", pre.getResponsavelId())
                .param("parentesco", pre.getParentesco())
                .param("nome", pre.getCrianca().nome())
                .param("nascimento", pre.getCrianca().dataNascimento())
                .param("cpf", pre.getCrianca().cpf() == null ? null : pre.getCrianca().cpf().getDigitos())
                .param("uniforme", pre.getFardas().get(TipoFarda.UNIFORME))
                .param("edFisica", pre.getFardas().get(TipoFarda.EDUCACAO_FISICA))
                .query(UUID.class).single());
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public List<PreMatriculaResumoResponse> listar(StatusPreMatricula status, Integer ano) {
        return executar(() -> jdbc.sql(SELECT_RESUMO + """
                         WHERE (CAST(:status AS text) IS NULL OR p.status = CAST(:status AS status_pre_matricula))
                           AND (CAST(:ano AS integer) IS NULL OR al.ano = CAST(:ano AS integer))
                         ORDER BY CASE WHEN p.status = 'PENDENTE' THEN p.criado_em END ASC,
                                  p.analisado_em DESC NULLS LAST
                         LIMIT 200
                        """)
                .param("status", status == null ? null : status.name())
                .param("ano", ano)
                .query(this::mapearResumo)
                .list());
    }

    @Override
    public List<PreMatriculaResumoResponse> listarDoResponsavel(UUID responsavelId) {
        return executar(() -> jdbc.sql(SELECT_RESUMO + " WHERE p.responsavel_id = :responsavel ORDER BY p.criado_em DESC")
                .param("responsavel", responsavelId)
                .query(this::mapearResumo)
                .list());
    }

    @Override
    public Optional<PreMatriculaResumoResponse> buscarResumo(UUID id) {
        return executar(() -> jdbc.sql(SELECT_RESUMO + " WHERE p.id = :id")
                .param("id", id).query(this::mapearResumo).optional());
    }

    private PreMatriculaResumoResponse mapearResumo(ResultSet rs, int n) throws SQLException {
        LocalDate nascimento = Colunas.data(rs, "aluno_nascimento");
        return new PreMatriculaResumoResponse(
                Colunas.uuid(rs, "id"), rs.getObject("criado_em", OffsetDateTime.class), rs.getInt("ano"),
                rs.getString("serie"), NivelEnsino.valueOf(rs.getString("nivel")), rs.getString("aluno_nome"),
                nascimento, Period.between(nascimento, LocalDate.now(relogio)).getYears(),
                rs.getString("responsavel"), Telefone.formatar(rs.getString("telefone")),
                StatusPreMatricula.valueOf(rs.getString("status")), rs.getString("motivo_rejeicao"),
                Colunas.uuid(rs, "matricula_id"));
    }
}
