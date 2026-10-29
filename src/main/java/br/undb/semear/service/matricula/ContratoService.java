package br.undb.semear.service.matricula;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.matricula.ContratoDtos.AssinanteResponse;
import br.undb.semear.dto.matricula.ContratoDtos.ClausulaResponse;
import br.undb.semear.dto.matricula.ContratoDtos.ContratoResumoResponse;
import br.undb.semear.dto.matricula.ContratoDtos.ContratoTermoResponse;
import br.undb.semear.model.entity.matricula.Contrato;
import br.undb.semear.model.enums.StatusContrato;
import br.undb.semear.model.enums.StatusMatricula;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.matricula.ContratoRepository;
import br.undb.semear.repository.matricula.ContratoRepository.Assinante;
import br.undb.semear.repository.matricula.ContratoRepository.ContratoComContexto;

/**
 * Contratos (RF01.4, RD03). O texto sai do Model (Contrato.clausulas) com os valores da tabela de preços;
 * a assinatura é o aceite eletrônico do responsável no portal ou o registro da secretaria.
 */
@Service
public class ContratoService {

    private static final Logger LOG = LoggerFactory.getLogger(ContratoService.class);

    private final ContratoRepository contratos;
    private final Clock relogio;

    public ContratoService(ContratoRepository contratos, Clock relogio) {
        this.contratos = contratos;
        this.relogio = relogio;
    }

    public ContratoTermoResponse termo(UUID id) {
        ContratoComContexto c = buscar(id);
        Contrato k = c.contrato();
        return new ContratoTermoResponse(k.getId(), k.getTipo(), k.getTitulo(), k.getStatus(), k.getMatriculaId(),
                c.alunoId(), c.condicoes().aluno(), c.condicoes().ano(), c.condicoes().serie(), c.condicoes().turma(),
                c.assinante(), k.getAssinadoEm(), podeAssinar(c),
                k.clausulas(c.condicoes()).stream().map(x -> new ClausulaResponse(x.titulo(), x.texto())).toList(),
                contratos.responsaveisDoAluno(c.alunoId()).stream()
                        .map(a -> new AssinanteResponse(a.id(), a.nome(), a.parentesco(), a.financeiro())).toList());
    }

    public List<ContratoResumoResponse> doAluno(UUID alunoId) {
        return contratos.doAluno(alunoId).stream().map(c -> new ContratoResumoResponse(c.contrato().getId(),
                c.contrato().getTipo(), c.contrato().getTitulo(), c.contrato().getStatus(), c.condicoes().ano(),
                c.condicoes().serie(), c.assinante(), c.contrato().getAssinadoEm(), podeAssinar(c))).toList();
    }

    public UUID alunoDoContrato(UUID id) {
        return buscar(id).alunoId();
    }

    /** RD03: o Model confere vínculo, status do documento e da matrícula; o banco confere o vínculo de novo. */
    @Transactional
    public ContratoTermoResponse assinar(UUID id, UUID responsavelId, String canal) {
        ContratoComContexto c = buscar(id);
        List<UUID> vinculados = contratos.responsaveisDoAluno(c.alunoId()).stream().map(Assinante::id).toList();
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        c.contrato().assinar(responsavelId, vinculados, c.statusMatricula(), agora);
        contratos.registrarAssinatura(id, responsavelId, agora);
        LOG.info("{} {} assinado ({})", c.contrato().getTitulo(), id, canal);
        return termo(id);
    }

    private static boolean podeAssinar(ContratoComContexto c) {
        return c.contrato().getStatus() == StatusContrato.PENDENTE
                && (c.statusMatricula() == StatusMatricula.ATIVA || c.statusMatricula() == StatusMatricula.AGUARDANDO_PAGAMENTO);
    }

    private ContratoComContexto buscar(UUID id) {
        return contratos.buscar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Contrato"));
    }
}
