# Sistema Semear — Gestão Escolar

Projeto da disciplina **Programação Orientada a Objetos** (UNDB). Sistema web de gestão escolar para o Centro Educacional Semeando (Maternal ao 5º ano, São Luís/MA).

Arquitetura **MVC** em um único projeto:

| Camada | Linguagem | Pasta |
|---|---|---|
| Model | Java 21 | `src/main/java/br/undb/semear/{model,service,repository}` |
| Controller | Java 21 (Spring MVC) | `src/main/java/br/undb/semear/controller` |
| View | TypeScript (React) | `src/main/view` |
| Banco | Supabase (PostgreSQL) | `supabase/migrations` |

**Comece pela documentação:** [docs/README.md](docs/README.md).
Situação da conexão com o Supabase: [docs/06-supabase-conexao.md](docs/06-supabase-conexao.md).

## Requisitos mínimos do professor

| Exigência | Onde está | Como foi testado |
|---|---|---|
| Tela de login segura | `/login` (Supabase Auth). A API Java confere a assinatura, o emissor e a validade do token em toda requisição. A senha fica mascarada, o erro é genérico ("E-mail ou senha incorretos") e a senha forte é exigida no cadastro | Senha errada recusada, token inventado ou adulterado recusado (401), sem login → redireciona para `/login` |
| Painel principal | `/escola` (indicadores de turmas, pendências, obrigações, inadimplência, aprovações) | Abre por perfil, sem erros |
| Cadastro completo (CRUD) no banco | **Obrigações** (`/escola/obrigacoes`): nova, lista, editar, apagar. Também Turmas e Listas de materiais | Inserir → listar → atualizar → excluir pela tela e pela API; permissões por perfil (403) |
| Relatório com os dados cadastrados | **Relatório de alunos** (`/escola/relatorios`): filtros por ano e turma, totais, **Imprimir / PDF** e **Planilha (CSV)** | Total confere com o cadastro, sem CPF (LGPD), professor só vê as próprias turmas |

## O que o sistema faz

| Área | Funcionalidades |
|---|---|
| Matrícula (RF01) | Pré-matrícula no portal, fila e aprovação pela secretaria com controle de vagas (RN04), rematrícula só para adimplentes (RN13), cancelamento |
| Documentos (RN12) | Checklist por aluno no Storage privado; matrícula conclui com pendência sinalizada; aba **Pendências** para cobrar as famílias |
| Contratos (RF01.4) | Contrato de Matrícula (Fundamental) e Ficha de Sócio (Infantil) com valores da tabela; aceite eletrônico no portal ou registro na secretaria (RD03) |
| Financeiro (RF02, RF04, RF05) | Cobranças com desconto de 10% (RN08), multa e juros (RN09), bolsas (RN11), inadimplência, resumo mensal, boleto Cora simulado com webhook, avisos de cobrança |
| Pedagógico (RF08) | Diário com chamada, planos de aula, notas por bimestre, frequência (alerta abaixo de 75%), calendário letivo, formação continuada |
| Institucional (RF09) | Obrigações (Censo/INEP, SMTT, Sistema Presença, Cora) com aviso de prazo, comprovante e renovação automática |
| Portal (RF06, RF10) | Responsável cria conta, acompanha cada filho (contrato, boletim, frequência, materiais, documentos) e paga as cobranças |
| Acesso | Perfis por cargo editáveis pelo gestor, com "Restaurar padrão"; professor só vê as próprias turmas |
| Controles internos (SoD/SOX) | Dupla aprovação de bolsas e preços (quem pede não aprova), funções incompatíveis por perfil, conflito de interesse, trilha de auditoria imutável com verificação de integridade, revisão trimestral de acessos ([docs/10](docs/10-controles-internos.md)) |
| Configuração | Usuários (logins com senha provisória), abrir ano letivo, tabela de preços com reajuste, turmas e professores por turma |

POO no código (detalhes em [docs/04-model-controller.md](docs/04-model-controller.md)): herança e polimorfismo (`Funcionario`, `Cobranca`, `Contrato`), Template Method (`Cobranca.calcular`, `Contrato.clausulas`, `SolicitacaoAprovacao`, `JdbcRepository`), Strategy (`RegraEncargos`), Null Object (`SemEncargos`), Factory (`CobrancaFactory`, `ContratoFactory`, `FuncionarioFactory`, `NotificadorFactory`), Adapter (`SupabaseStorage`, `CoraGateway`), Value Objects (`Dinheiro`, `Cpf`, `ValorNota`...).

## Como rodar

Pré-requisitos: JDK 21, Maven 3.9+, Node.js 20+ e o arquivo `.env` na raiz (copie de `.env.example` e preencha).

```bash
npm install                 # uma vez: dependências da View

# Desenvolvimento (dois terminais)
mvn spring-boot:run         # Model + Controller em http://localhost:8080
npm run dev                 # View em http://localhost:5173 (chamadas /api vão para a 8080)

# Testes
mvn test                    # Java (170 testes)
npm test                    # View
npm run typecheck           # checagem de tipos TypeScript

# Entrega: um único .jar com a View dentro
npm run build && mvn package
java -jar target/semear-0.1.0-SNAPSHOT.jar   # tudo em http://localhost:8080
```

`SEMEAR_JOBS_ATIVOS=false` desliga as tarefas agendadas (boletos, avisos, rotina diária) — útil para rodar sem escrever no banco sozinho.

Endpoints úteis: `GET /api/saude` (público) · `GET /api/me` (exige login) · Swagger em `/swagger-ui.html`.

## Contas de demonstração

| Perfil | Login | O que mostrar |
|---|---|---|
| Administração | conta do gestor do projeto | tudo, inclusive Perfis de acesso |
| Secretaria | `secretaria.semear@example.com` | pré-matrículas, matrículas, pendências, obrigações |
| Financeiro | `financeiro.semear@example.com` | cobranças, inadimplência, bolsas |
| Professora | `professora.semear@example.com` | 1º Ano A: chamada, planos, notas |
| Responsável | `responsavel.semear@example.com` | portal: filhos, boletim, cobranças, contratos |

Todos os dados são fictícios (CPFs inventados com dígito válido, e-mails `example.com`).
