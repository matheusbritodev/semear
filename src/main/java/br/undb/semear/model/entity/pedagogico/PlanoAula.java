package br.undb.semear.model.entity.pedagogico;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.enums.TipoPlano;
import br.undb.semear.model.exception.AcessoNegadoException;
import br.undb.semear.model.exception.DadoInvalidoException;

/** Plano de aula ou planejamento diário (RF08.2). Só quem escreveu altera ou apaga. */
public class PlanoAula extends Entidade {

    private final UUID turmaId;
    private final UUID professorId;
    private TipoPlano tipo;
    private LocalDate dataPrevista;
    private String objetivo;
    private String conteudo;

    public PlanoAula(UUID id, UUID turmaId, UUID professorId, TipoPlano tipo, LocalDate dataPrevista, String objetivo,
                     String conteudo) {
        super(id);
        this.turmaId = Objects.requireNonNull(turmaId, "turmaId");
        this.professorId = Objects.requireNonNull(professorId, "professorId");
        this.tipo = tipo;
        this.dataPrevista = dataPrevista;
        this.objetivo = objetivo;
        this.conteudo = conteudo;
    }

    public static PlanoAula novo(UUID turmaId, UUID professorId, TipoPlano tipo, LocalDate data, String objetivo,
                                 String conteudo, AnoLetivo ano) {
        PlanoAula plano = new PlanoAula(null, turmaId, professorId, tipo, data, objetivo, conteudo);
        plano.preencher(tipo, data, objetivo, conteudo, ano);
        return plano;
    }

    public void alterar(UUID quem, TipoPlano tipo, LocalDate data, String objetivo, String conteudo, AnoLetivo ano) {
        exigirAutor(quem);
        preencher(tipo, data, objetivo, conteudo, ano);
    }

    public void exigirAutor(UUID quem) {
        if (!professorId.equals(quem)) {
            throw new AcessoNegadoException("PLANO_DE_OUTRO_PROFESSOR", "Só quem escreveu o plano pode alterá-lo");
        }
    }

    private void preencher(TipoPlano tipo, LocalDate data, String objetivo, String conteudo, AnoLetivo ano) {
        if (tipo == null) {
            throw new DadoInvalidoException("tipo", "Escolha o tipo de plano");
        }
        if (data == null || !ano.contem(data)) {
            throw new DadoInvalidoException("dataPrevista", "A data precisa estar dentro do ano letivo " + ano.getAno());
        }
        String obj = objetivo == null ? "" : objetivo.trim();
        if (obj.isEmpty() || obj.length() > 255) {
            throw new DadoInvalidoException("objetivo", "Informe o objetivo (até 255 caracteres)");
        }
        String texto = conteudo == null ? "" : conteudo.trim();
        if (texto.isEmpty() || texto.length() > 4000) {
            throw new DadoInvalidoException("conteudo", "Descreva as atividades (até 4.000 caracteres)");
        }
        this.tipo = tipo;
        this.dataPrevista = data;
        this.objetivo = obj;
        this.conteudo = texto;
    }

    public UUID getTurmaId() {
        return turmaId;
    }

    public UUID getProfessorId() {
        return professorId;
    }

    public TipoPlano getTipo() {
        return tipo;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public String getConteudo() {
        return conteudo;
    }
}
