# 07 — Plano de trabalho e convenções

---

## 1. Ordem de implementação

Cada etapa termina com algo funcionando de ponta a ponta. Marque conforme avançar.

### Etapa 0 — Fundação
- [x] Projeto Supabase conectado; migrations de esquema, segurança e storage aplicadas ([06](06-supabase-conexao.md))
- [x] Estrutura de pastas MVC (`src/main/java`, `src/main/view`, `src/test`), `application.yml`, `.env.example`
- [x] Instalar JDK 21 + Maven
- [x] `pom.xml` (Spring Boot 3.5, Java 21), `SemearApplication`, `GET /api/saude`
- [x] `package.json`, `vite.config.ts`, `tsconfig.json`: View com React 19 + TS, Tailwind 4, `config/env.ts`, cliente Supabase
- [x] Login: tela de login → JWT (validado por JWKS/ES256) → `GET /api/me` → área e menu por perfil
- [x] Base do Model: `Entidade`, value objects (`Dinheiro`, `Cpf`, `Email`, `Telefone`, `Permissao`), hierarquia de exceções, `Repository<T,ID>`, `JdbcRepository<T>`, `TradutorErroBanco`, `GlobalExceptionHandler`
- [x] Testes: 32 em Java (value objects, `Usuario`, tradutor de erros, segurança dos Controllers) e 5 na View
- [x] Remover `usuario.senha_hash`; seed realista aplicado (`supabase/seed.sql`)
- [x] `.env` completo; `/api/saude` responde `UP`
- [x] Primeiro ADMIN e contas de demonstração; login testado de ponta a ponta (Supabase Auth → JWT → `/api/me`)

### Etapa 1 — Domínio e cadastros base
- [x] Enums usados até aqui (`PerfilUsuario`, `TipoFuncionario`, `NivelEnsino`, `StatusMatricula`, `TipoMatricula`, `TipoCobranca`, `TipoContrato`); os demais entram com os módulos
- [x] `Pessoa` (abstrata) → `Aluno`, `Responsavel`, `Funcionario` (abstrata) → `Professor`, `Administrativo`, `Gestor`, `PrestadorServico`; `VinculoResponsavel`; `FuncionarioFactory`
- [x] `AnoLetivo`, `Serie`, `Turma` (RN04 na própria classe)
- [x] Repositórios JDBC (aluno, responsável, funcionário, turma, ano letivo, alertas) com `JdbcRepository` (Template Method)
- [x] Escopo do professor: só as próprias turmas e alunos; cadastro de responsáveis bloqueado (LGPD)
- [x] Telas: Alunos (lista + ficha + troca do responsável financeiro), Responsáveis (lista + ficha + contato), Turmas (ocupação, alertas, capacidade), Funcionários (lista + cadastro + edição + ativar/desativar), painel inicial
- [x] Testes: RD01, RN02, RN04, RN01/RN07 (nível), polimorfismo de `Funcionario`, permissões por perfil (58 testes Java)
- [ ] Pendentes para etapas seguintes: `TabelaPreco`, `Disciplina`, telas de anos letivos, preços e usuários (convite)

### Etapa 2 — Matrícula
- [x] Portal: completar cadastro no primeiro acesso, pré-matrícula, acompanhar status e motivo da recusa
- [x] Escola: fila, análise, aprovar (com escolha de turma e vagas) / rejeitar com motivo
- [x] Matrículas: listagem, detalhe (contrato, fardas, cobrança de matrícula), cancelamento, baixa presencial da matrícula
- [x] Rematrícula (portal e secretaria) com `Rematricula.avaliar` (RN03, RN04, RN13)
- [x] Alertas de turma lotada (Etapa 1)
- [x] Testes: RN04, RN05, RN06, RD04, RN13, transições de `Matricula` (74 testes Java) + fluxos testados contra o banco real
- [x] **Etapa 2b:** criar conta, recuperar e redefinir senha pelo portal; documentos no Storage privado (`ArmazenamentoArquivos` → `SupabaseStorage`), links temporários de 5 min, checklist RN12 (`ChecklistDocumentos`) na escola, na matrícula e no portal; validação do arquivo pelo conteúdo (`ArquivoEnviado`); 80 testes Java
- [x] **Orientação do prof. Gusmão (28/10/2026):** matrícula pode ser concluída com documentos pendentes; alerta na ficha do aluno, na lista de alunos e no portal; aba **Pendências** com card no painel e atalho de cobrança por WhatsApp; **perfis de acesso** editáveis com "Restaurar padrão" (D11); conta do responsável criada pela aplicação Java (D10); seed de documentos (`supabase/scripts/SeedDocumentos.java`); 87 testes Java

### Etapa 3 — Financeiro
- [x] `Cobranca` + subclasses + `CobrancaFactory`, `RegraEncargos` (`RegraEncargosContrato`, `SemEncargos`), `Bolsa`, `ConcessaoBolsa`, `TipoDesconto`, `CalendarioDiasUteis`
- [x] Geração de cobranças, listagem, baixa presencial, valor a pagar na data (com composição)
- [x] Bolsas: cadastro, concessão, revogação (recalcula as cobranças em aberto)
- [x] Inadimplência e resumo mensal; portal com cobranças e boleto
- [x] `CoraGatewayFalso` + emissão agendada (com trava `SKIP LOCKED`) + webhook HMAC + pagamento simulado no portal (D12)
- [x] Fila de notificações com `NotificadorLog` (via `NotificadorFactory`) e rotina diária agendada
- [x] Testes: RN08, RN09 (bate com o seed: R$ 375 → R$ 383,86 com 11 dias), RN11 (bate com `fn_desconto_bolsa`), RD05, RD06, assinatura do webhook; 104 testes Java. RN01/RD02 seguem garantidas pelo banco e foram testadas contra ele

### Etapa 4 — Portal completo
- [x] Contratos: `Contrato` (Template Method das cláusulas) + `ContratoMatricula`/`FichaSocio` + `ContratoFactory`; aceite eletrônico no portal e registro presencial na escola (RD03); termo para imprimir
- [x] Materiais: `ListaMaterial` (sem item repetido), edição na escola, cópia do ano anterior, lista do filho no portal (RF06)
- [x] Boletim no portal: `Boletim` (média parcial por disciplina, frequência); Infantil só frequência
- [x] Página do filho no portal e aviso de documento para assinar; 114 testes Java
- Observação: o texto do contrato é um resumo das cláusulas citadas no MODELO.md (o CONTRATO.docx não está no repositório)

### Etapa 5 — Pedagógico (RF08)
- [x] Minhas turmas, diário + chamada (`DiarioClasse`, `CalendarioLetivo`), planos (`PlanoAula`, só o autor altera), notas (`LancamentoNotas`, `ValorNota`, `ProfessoresDaTurma`), calendário (`EventoCalendario`), formação continuada (`FormacaoContinuada`)
- [x] Frequência por aluno com alerta abaixo de 75% (LDB, art. 24); bimestre atual = ano letivo dividido em 4 partes iguais (a escola não tem datas de bimestre cadastradas)
- [x] Testes: RD07, chamada completa, feriado/fim de semana/futuro, faixa da nota, especialista × polivalente, bimestre; 122 testes Java

### Etapa 6 — Institucional (RF09) e fechamento
- [x] Obrigações (`ObrigacaoInstitucional`, `ComprovanteObrigacao`): situação pelo prazo e antecedência, comprovante obrigatório para cumprir (D14), renovação das periódicas, card no painel
- [x] Rotinas automáticas na aplicação em vez do `pg_cron` (D15), incluindo a geração mensal no dia 25; encerramento de ano letivo (só depois do último dia letivo)
- [x] Diagrama de classes, relatório de entrega e revisão de LGPD em [08-entrega.md](08-entrega.md); 128 testes Java

### Depois (fora da entrega atual)
Integração real com Cora · envio real de e-mail/SMS/WhatsApp · PDF de contrato · correção INPC (RN10) · lacunas de dados ([03 §14](03-banco-de-dados.md#14-lacunas-entre-o-domínio-e-o-banco)) · auditoria · atividades extras.

### Divisão sugerida por frentes

Cada frente cuida do seu módulo nas três camadas (Model, Controller e View), de ponta a ponta:

| Frente | Model + Controller (Java) | View (TypeScript) |
|---|---|---|
| A — Pessoas e acesso | módulo `pessoa`, `security/` | login, usuários, alunos, responsáveis, funcionários |
| B — Acadêmico e matrícula | módulos `academico`, `matricula` | turmas, pré-matrícula, matrículas, rematrícula |
| C — Financeiro | módulo `financeiro`, `integration/cora`, `integration/notificacao` | cobranças, bolsas, inadimplência, portal financeiro |
| D — Pedagógico e institucional | módulos `pedagogico`, `institucional` | diário, planos, notas, obrigações |

---

## 2. Decisões pendentes

Levar ao professor. Ao decidir, atualizar esta tabela e o documento afetado.

| # | Pergunta | Impacto | Provisório |
|---|---|---|---|
| P01 | Mensalidades: 11 ou 12 parcelas? Anuidade de R$ 3.720 bate com 12 × 310 | Geração de cobranças | Meses dentro do ano letivo |
| P02 | Arredondamento do desconto: R$ 337,50 ou R$ 337,00? | `Dinheiro` | `HALF_EVEN` (337,50) |
| P03 | Desconto de 10% acumula com bolsa? Incide sobre o valor com bolsa? | `Cobranca.valorAPagar` | Incide sobre `valor_final` (já com bolsa) |
| P04 | Desconto de 10% vale para Taxa de Associado? | `TaxaAssociado` | Não |
| P05 | Valor da cobrança de matrícula (arras) | `tabela_preco.valor_matricula` | Igual à mensalidade |
| P06 | Carência até o 5º dia útil: conta feriados? Quais? | `CalendarioDiasUteis` | Só fins de semana + feriados do `calendario_letivo` |
| P07 | "Módulo" do `MODELO.md` = disciplina? | Pedagógico | Sim, usa `disciplina` |
| P08 | Notas estão no escopo? | Tabela `nota`, portal | Sim (o portal exibe) |
| P09 | Datas reais de Censo, SMTT e prazo do Cora | Seed de obrigações | Valores do script |
| P10 | INPC (RN10) entra nesta etapa? | `RegraEncargos` | Não |
| P11 | Responsável pode se cadastrar sozinho ou só via escola? | Fluxo de cadastro | Pode; no projeto acadêmico a conta já nasce confirmada (D10) |
| ~~P12~~ | ✅ **Decidido pelo prof. Gusmão (28/10/2026):** não bloqueia. A matrícula é concluída com sinalização na ficha do aluno e uma aba geral de Pendências no painel, visível por padrão para gestão, secretaria e o próprio responsável | `PendenciaService`, `PerfisAcessoPage` | — |

---

## 3. Convenções

### 3.1 Idioma e nomes
- **Arquitetura em inglês** (decisão D09): camadas e pastas (`model`, `view`, `controller`, `service`, `repository`, `dto`, `pages`, `components`) e sufixos técnicos.
- **Domínio em português**, sem acento nos identificadores: `Matricula`, `valorAPagar`, `responsavelFinanceiro`.
- Sufixos Java: `*Controller`, `*Service`, `*Repository` (interface), `*JdbcRepository`, `*InMemoryRepository` (testes), `*Request` / `*Response` (DTOs), `*Exception`, `*Factory`.
- Sufixos TypeScript: `*Service` (cliente de um Controller), `*Layout`, `*Page` opcional; hooks começam com `use`.
- Java: classes `PascalCase`, métodos/atributos `camelCase`, constantes `MAIUSCULO_COM_UNDERSCORE`, pacotes minúsculos.
- Banco: `snake_case` (já definido). TypeScript: componentes e classes `PascalCase.tsx/.ts`, funções/variáveis `camelCase`.
- O mesmo assunto tem o mesmo nome nas três camadas: `PreMatricula` (Model) · `PreMatriculaController` · `PreMatriculaService.ts` (View).

### 3.2 Código Java
- Uma classe pública por arquivo; atributos `private`; construtores validam estado.
- Sem setters genéricos: métodos com intenção (`ativar()`, `cancelar(motivo)`).
- Imutabilidade por padrão em value objects e DTOs (`record`).
- `Optional` só em retorno, nunca em atributo ou parâmetro.
- Dinheiro sempre `Dinheiro`/`BigDecimal`; nunca `double`.
- Datas: `LocalDate` (calendário), `YearMonth` (competência), `Instant`/`OffsetDateTime` (instantes).
- Nada de `System.out`; usar SLF4J. Nada de dado pessoal em log.

### 3.3 Testes
- Cada regra RN/RD com pelo menos um teste, com o ID no nome: `rn04_naoMatriculaAcimaDe30`.
- Dados **sempre sintéticos** (CPF `00000000191`, nomes fictícios).

### 3.4 Git
- `.env` nunca é commitado (já no `.gitignore`); só `.env.example`.
- Branches: `main` (estável) e `feat/<frente>-<assunto>` (ex.: `feat/matricula-aprovacao`).
- Commits pequenos, no imperativo: `Adiciona regra de capacidade da turma`.
- Mudança no banco = nova migration + atualização de [03-banco-de-dados.md](03-banco-de-dados.md) no mesmo commit.

### 3.5 Documentação
- Mudou uma regra, endpoint ou tela? Atualize o documento correspondente nesta pasta no mesmo PR.
- Decisão de arquitetura nova → linha em [02-arquitetura.md §7](02-arquitetura.md#7-decisões-de-arquitetura-registro).
