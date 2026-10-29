package br.undb.semear.model.entity.academico;

import java.util.Objects;
import java.util.UUID;

import br.undb.semear.model.entity.Entidade;
import br.undb.semear.model.exception.DadoInvalidoException;
import br.undb.semear.model.exception.RegraNegocioException;
import br.undb.semear.model.exception.TurmaCheiaException;

/**
 * Uma série em um ano letivo. RN04 é regra da própria turma: nenhuma outra classe conta alunos.
 * "ocupadas" são as matrículas que ocupam vaga (aguardando pagamento ou ativas).
 * Sob concorrência, a garantia final é o trigger do banco (trava de linha).
 */
public class Turma extends Entidade {

    public static final int CAPACIDADE_MAXIMA = 30;

    private final Serie serie;
    private final int anoLetivo;
    private final String nome;
    private int capacidadeMax;
    private final int ocupadas;

    public Turma(UUID id, Serie serie, int anoLetivo, String nome, int capacidadeMax, int ocupadas) {
        super(id);
        this.serie = Objects.requireNonNull(serie, "serie");
        this.anoLetivo = anoLetivo;
        this.nome = Objects.requireNonNull(nome, "nome");
        this.capacidadeMax = validarCapacidade(capacidadeMax);
        this.ocupadas = ocupadas;
    }

    private static int validarCapacidade(int capacidade) {
        if (capacidade < 1 || capacidade > CAPACIDADE_MAXIMA) {
            throw new DadoInvalidoException("capacidade", "A capacidade deve ficar entre 1 e " + CAPACIDADE_MAXIMA + " alunos");
        }
        return capacidade;
    }

    public boolean temVaga() {
        return ocupadas < capacidadeMax;
    }

    public boolean estaLotada() {
        return !temVaga();
    }

    public int vagasRestantes() {
        return Math.max(0, capacidadeMax - ocupadas);
    }

    public int percentualOcupacao() {
        return Math.round(100f * ocupadas / capacidadeMax);
    }

    /** RN04: impede nova matrícula em turma cheia. */
    public void garantirVaga() {
        if (estaLotada()) {
            throw new TurmaCheiaException(getNomeCompleto(), capacidadeMax);
        }
    }

    /** RN04: entre 1 e 30, e nunca abaixo dos alunos já matriculados. */
    public void alterarCapacidade(int novaCapacidade) {
        validarCapacidade(novaCapacidade);
        if (novaCapacidade < ocupadas) {
            throw new RegraNegocioException("CAPACIDADE_ABAIXO_OCUPACAO",
                    "A turma já tem " + ocupadas + " alunos; a capacidade não pode ser menor que isso");
        }
        this.capacidadeMax = novaCapacidade;
    }

    /** Ex.: "1º Ano A". */
    public String getNomeCompleto() {
        return serie.getNome() + " " + nome;
    }

    public Serie getSerie() {
        return serie;
    }

    public int getAnoLetivo() {
        return anoLetivo;
    }

    public String getNome() {
        return nome;
    }

    public int getCapacidadeMax() {
        return capacidadeMax;
    }

    public int getOcupadas() {
        return ocupadas;
    }
}
