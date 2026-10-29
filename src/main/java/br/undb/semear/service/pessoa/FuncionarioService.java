package br.undb.semear.service.pessoa;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.undb.semear.model.entity.pessoa.DadosFuncionario;
import br.undb.semear.model.entity.pessoa.Funcionario;
import br.undb.semear.model.entity.pessoa.FuncionarioFactory;
import br.undb.semear.model.enums.TipoFuncionario;
import br.undb.semear.model.exception.RecursoNaoEncontradoException;
import br.undb.semear.model.valueobject.Cpf;
import br.undb.semear.model.valueobject.Email;
import br.undb.semear.model.valueobject.Telefone;
import br.undb.semear.repository.pessoa.FuncionarioRepository;

/** RF07: cadastro de funcionários. A subclasse certa vem da FuncionarioFactory. */
@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarios;

    public FuncionarioService(FuncionarioRepository funcionarios) {
        this.funcionarios = funcionarios;
    }

    public List<Funcionario> pesquisar(String busca, TipoFuncionario tipo) {
        return funcionarios.pesquisar(busca, tipo);
    }

    public Funcionario buscar(UUID id) {
        return funcionarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário"));
    }

    @Transactional
    public Funcionario criar(String nome, String cpf, TipoFuncionario tipo, String telefone, String email,
                             LocalDate dataAdmissao) {
        Funcionario novo = FuncionarioFactory.criar(tipo, new DadosFuncionario(null, nome, new Cpf(cpf),
                Telefone.opcional(telefone), Email.opcional(email), dataAdmissao, true, null));
        return funcionarios.salvar(novo);
    }

    @Transactional
    public Funcionario atualizar(UUID id, String nome, String telefone, String email, LocalDate dataAdmissao) {
        Funcionario funcionario = buscar(id);
        funcionario.atualizarDados(nome, Telefone.opcional(telefone), Email.opcional(email), dataAdmissao);
        return funcionarios.salvar(funcionario);
    }

    @Transactional
    public Funcionario alterarAtivo(UUID id, boolean ativo) {
        Funcionario funcionario = buscar(id);
        if (ativo) {
            funcionario.ativar();
        } else {
            funcionario.desativar();
        }
        return funcionarios.salvar(funcionario);
    }
}
