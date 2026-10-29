package br.undb.semear.service.academico;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.academico.AnoLetivoRepository;

@Service
public class AnoLetivoService {

    private final AnoLetivoRepository anos;
    private final Clock relogio;

    public AnoLetivoService(AnoLetivoRepository anos, Clock relogio) {
        this.anos = anos;
        this.relogio = relogio;
    }

    public List<AnoLetivo> listar() {
        return anos.listar();
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    /**
     * Ano de referência das telas: o que está em andamento; nas férias, o último que já começou;
     * se nenhum começou ainda, o primeiro cadastrado.
     */
    public AnoLetivo corrente() {
        List<AnoLetivo> todos = anos.listar();
        LocalDate hoje = hoje();
        return todos.stream().filter(a -> a.emAndamento(hoje)).findFirst()
                .or(() -> todos.stream().filter(a -> !a.getDataInicio().isAfter(hoje))
                        .max(Comparator.comparingInt(AnoLetivo::getAno)))
                .or(() -> todos.stream().findFirst())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ano letivo"));
    }

    /** Virada do ano: depois do último dia letivo, as matrículas ATIVAS passam a ENCERRADAS. */
    @org.springframework.transaction.annotation.Transactional
    public int encerrar(int ano) {
        AnoLetivo alvo = anos.buscarPorAno(ano).orElseThrow(() -> new RecursoNaoEncontradoException("Ano letivo " + ano));
        alvo.exigirEncerravel(hoje());
        return anos.encerrar(alvo.getId());
    }

    /** Ano informado ou, se nulo, o corrente. */
    public int anoOuCorrente(Integer ano) {
        return ano != null ? ano : corrente().getAno();
    }
}
