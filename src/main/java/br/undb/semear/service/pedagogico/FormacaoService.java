package br.undb.semear.service.pedagogico;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.dto.pedagogico.PedagogicoDtos.FormacaoRequest;
import br.undb.semear.dto.pedagogico.PedagogicoDtos.FormacaoResponse;
import br.undb.semear.model.entity.pedagogico.FormacaoContinuada;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.repository.pedagogico.PedagogicoRepository;
import br.undb.semear.service.academico.AnoLetivoService;
import br.undb.semear.service.pessoa.FuncionarioService;

/** Formação continuada dos funcionários (RF08.4), mantida pelo RH. */
@Service
public class FormacaoService {

    private final PedagogicoRepository pedagogico;
    private final FuncionarioService funcionarios;
    private final AnoLetivoService anosLetivos;

    public FormacaoService(PedagogicoRepository pedagogico, FuncionarioService funcionarios, AnoLetivoService anosLetivos) {
        this.pedagogico = pedagogico;
        this.funcionarios = funcionarios;
        this.anosLetivos = anosLetivos;
    }

    public List<FormacaoResponse> doFuncionario(UUID funcionarioId) {
        funcionarios.buscar(funcionarioId);
        return pedagogico.formacoes(funcionarioId).stream().map(FormacaoService::resposta).toList();
    }

    @Transactional
    public List<FormacaoResponse> registrar(UUID funcionarioId, FormacaoRequest r) {
        funcionarios.buscar(funcionarioId);
        pedagogico.inserirFormacao(FormacaoContinuada.nova(funcionarioId, r.curso(), r.cargaHoraria(), r.concluidaEm(),
                anosLetivos.hoje()));
        return doFuncionario(funcionarioId);
    }

    @Transactional
    public List<FormacaoResponse> remover(UUID funcionarioId, UUID formacaoId) {
        pedagogico.formacao(formacaoId).filter(f -> f.getFuncionarioId().equals(funcionarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Formação"));
        pedagogico.removerFormacao(formacaoId);
        return doFuncionario(funcionarioId);
    }

    private static FormacaoResponse resposta(FormacaoContinuada f) {
        return new FormacaoResponse(f.getId(), f.getCurso(), f.getCargaHoraria(), f.getConcluidaEm(), f.concluida());
    }
}
