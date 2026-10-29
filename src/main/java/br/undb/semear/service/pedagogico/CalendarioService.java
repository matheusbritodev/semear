package br.undb.semear.service.pedagogico;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.pedagogico.PedagogicoDtos.EventoRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.EventoResponse;
import br.undb.semear.model.entity.academico.AnoLetivo;
import br.undb.semear.model.entity.pedagogico.EventoCalendario;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.pedagogico.PedagogicoRepository;
import br.undb.semear.service.academico.AnoLetivoService;

/** Calendário letivo (RF08.3): feriados e recessos bloqueiam a chamada; eventos aparecem para todos. */
@Service
public class CalendarioService {

    private final PedagogicoRepository pedagogico;
    private final AnoLetivoService anosLetivos;

    public CalendarioService(PedagogicoRepository pedagogico, AnoLetivoService anosLetivos) {
        this.pedagogico = pedagogico;
        this.anosLetivos = anosLetivos;
    }

    public List<EventoResponse> eventos(Integer ano) {
        return pedagogico.eventos(anosLetivos.anoOuCorrente(ano)).stream().map(CalendarioService::resposta).toList();
    }

    @Transactional
    public List<EventoResponse> criar(EventoRequest r) {
        AnoLetivo ano = anosLetivos.listar().stream().filter(a -> a.getAno() == r.ano()).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ano letivo " + r.ano()));
        pedagogico.inserirEvento(EventoCalendario.novo(ano, r.data(), r.tipo(), r.descricao()));
        return eventos(r.ano());
    }

    @Transactional
    public List<EventoResponse> remover(UUID id) {
        EventoCalendario evento = pedagogico.evento(id).orElseThrow(() -> new RecursoNaoEncontradoException("Dia do calendário"));
        pedagogico.removerEvento(id);
        return eventos(evento.getAno());
    }

    private static EventoResponse resposta(EventoCalendario e) {
        return new EventoResponse(e.getId(), e.getData(), e.getTipo(), e.getTipo().getRotulo(), e.getDescricao(),
                e.getTipo().temAula());
    }
}
