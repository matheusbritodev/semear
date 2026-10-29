# 03 — Banco de dados (Supabase / PostgreSQL)

> Como o banco funciona e como a aplicação Java deve usá-lo. O detalhamento coluna a coluna está no Apêndice A de `infosystem/Semear_Documentacao_Banco_de_Dados.pdf`; aqui fica o que é preciso para programar.

---

## 1. Princípios

1. **O banco é a última linha de defesa.** Regras de integridade e fluxos críticos (limite de 30, tipo de cobrança, ativação da matrícula, imutabilidade de cobrança paga) estão em triggers e valem para qualquer cliente.
2. **Fluxos de negócio são chamados, não reimplementados.** A aplicação Java chama `SELECT fn_aprovar_pre_matricula(...)` em vez de fazer os `INSERT`s na mão.
3. **Consultas de tela usam views** (`vw_*`), que já fazem os `JOIN`s e calculam atrasos pela data.
4. **O banco não é exposto à internet.** A API pública do Supabase (PostgREST) fica bloqueada; só a aplicação Java acessa, via conexão Postgres.
5. **O script original não é editado.** Toda mudança é uma nova migration em `supabase/migrations/`.

**Em números:** 32 tabelas · 20 enums · 48 FKs · 31 triggers · 36 funções · 15 views · 23 CHECKs.

**Convenções:** tabelas no singular, `snake_case`, sem acento · PK `id uuid` (`gen_random_uuid()`) · FK `<entidade>_id` · prefixos `ck_`, `uq_`, `ix_`, `trg_`, `fn_`, `vw_` · `criado_em`/`atualizado_em` (`timestamptz`) mantidos por trigger · dinheiro em `numeric(10,2)` (nunca `float`) · CPF em `char(11)` só com dígitos.

---

## 2. Tabelas por módulo

### 2.1 Acesso e pessoas

| Tabela | Para quê | Pontos-chave |
|---|---|---|
| `usuario` | Login e perfil | Após as migrations de integração: `id` = `auth.users.id`; sem `senha_hash` |
| `responsavel` | Pais/responsáveis | `cpf` único; `usuario_id` opcional (perfil `RESPONSAVEL`) |
| `aluno` | Cadastro único do aluno (RN03) | `numero_cadastro` sequencial; `cpf` opcional; `ativo` |
| `aluno_responsavel` | Vínculo N:N (RN02) | PK composta; `parentesco`; `resp_financeiro` (um por aluno, RD01) |
| `funcionario` | RH (RF07) | `tipo` = ADMINISTRATIVO, PROFESSOR, GESTOR, PRESTADOR_SERVICO |
| `perfil_permissao` | Matriz perfil × módulo | `pode_ler`, `pode_gravar` (ver [01-negocio.md §2](01-negocio.md#2-perfis-de-usuário)) |

### 2.2 Estrutura escolar

| Tabela | Para quê | Pontos-chave |
|---|---|---|
| `ano_letivo` | Ano e período | `ano` único; `data_inicio < data_fim` |
| `serie` | Maternal 1 … 5º Ano (carga inicial) | `nivel` define cobrança e contrato (RN01, RN07) |
| `turma` | Série em um ano letivo | `capacidade_max` 1–30 (RN04); único por (ano, série, nome) |
| `tabela_preco` | Valores por série e ano | `valor_matricula`, `valor_mensal`, `dia_vencimento` (1–28), `dias_prazo_matricula` |
| `lista_material` / `item_material` | Lista de materiais por série/ano (RF06) | Uma lista por série/ano |

### 2.3 Matrícula

| Tabela | Para quê | Pontos-chave |
|---|---|---|
| `pre_matricula` | Pedido do responsável (RF01.1) | Dados da criança ficam aqui até a aprovação; rejeitar exige motivo |
| `matricula` | Aluno em uma turma em um ano | Entidade central; `UNIQUE(aluno_id, ano_letivo_id)`; `tipo` e `ano_letivo_id` preenchidos por trigger |
| `farda_matricula` | Tamanhos de farda (RF01.3) | Uma linha por tipo (UNIFORME, EDUCACAO_FISICA) |
| `contrato` | Contrato ou Ficha de Sócio (RF01.4) | Criado por trigger; só responsável vinculado assina |
| `documento_aluno` | Documentos enviados | `tipo` é texto livre (ver lacunas §11) |

### 2.4 Financeiro

| Tabela | Para quê | Pontos-chave |
|---|---|---|
| `bolsa` | Tipos de bolsa | `PERCENTUAL` (≤100) ou `VALOR_FIXO` |
| `bolsa_matricula` | Bolsa concedida a uma matrícula | Vigência início/fim; mudanças recalculam cobranças em aberto |
| `cobranca` | Matrícula, Taxa de Associado ou Mensalidade | `valor_final = valor_bruto − desconto_bolsa` (CHECK); `competencia` sempre dia 1; campos do Cora; paga é imutável |
| `notificacao_cobranca` | Fila de avisos (RF05) | Uma por cobrança/tipo/canal/dia; a aplicação Java envia as `PENDENTE` |
| `alerta_secretaria` | Avisos internos | Hoje só `TURMA_LOTADA` |

### 2.5 Pedagógico

| Tabela | Para quê | Pontos-chave |
|---|---|---|
| `disciplina` | Componentes curriculares (carga inicial) | Equivale ao "Módulo" do `MODELO.md` |
| `turma_professor` | Quais turmas cada professor acessa | `disciplina_id` nulo = polivalente; só `tipo = PROFESSOR` |
| `diario_classe` | Aula registrada | Um por turma/professor/dia |
| `frequencia` | Presença por aluno | Só matrícula `ATIVA` da turma do diário |
| `plano_aula` | Plano de aula / planejamento diário | Professor precisa estar vinculado à turma |
| `calendario_letivo` | Dias de aula, feriados, recessos, eventos | Data dentro do ano letivo |
| `formacao_continuada` | Cursos do funcionário (RF08.4) | — |
| `nota` | Nota por disciplina/bimestre | Escopo a validar (sem RF próprio) |

### 2.6 Institucional

| Tabela | Para quê | Pontos-chave |
|---|---|---|
| `obrigacao_institucional` | Censo/INEP, SMTT, Sistema Presença, Cora (RF09) | Periodicidade em meses; alerta por antecedência |
| `documento_obrigacao` | Comprovantes enviados | — |

---

## 3. Tipos enumerados

Os enums do Java têm **exatamente os mesmos nomes de constantes** que os do banco, para mapear com `Enum.valueOf(rs.getString(...))`.

| Enum no banco | Valores | Enum Java |
|---|---|---|
| `perfil_usuario` | ADMIN, SECRETARIA, FINANCEIRO, PROFESSOR, RESPONSAVEL, ALUNO | `PerfilUsuario` |
| `tipo_funcionario` | ADMINISTRATIVO, PROFESSOR, GESTOR, PRESTADOR_SERVICO | `TipoFuncionario` |
| `nivel_ensino` | EDUCACAO_INFANTIL, ENSINO_FUNDAMENTAL | `NivelEnsino` |
| `status_pre_matricula` | PENDENTE, APROVADA, REJEITADA | `StatusPreMatricula` |
| `tipo_matricula` | NOVA, REMATRICULA | `TipoMatricula` |
| `status_matricula` | AGUARDANDO_PAGAMENTO, ATIVA, CANCELADA, ENCERRADA | `StatusMatricula` |
| `tipo_farda` | UNIFORME, EDUCACAO_FISICA | `TipoFarda` |
| `tipo_contrato` | CONTRATO_MATRICULA, FICHA_SOCIO | `TipoContrato` |
| `status_contrato` | PENDENTE, ASSINADO, CANCELADO | `StatusContrato` |
| `tipo_desconto` | PERCENTUAL, VALOR_FIXO | `TipoDesconto` |
| `tipo_cobranca` | MATRICULA, TAXA_ASSOCIADO, MENSALIDADE | `TipoCobranca` |
| `status_cobranca` | PENDENTE, PAGO, VENCIDO, CANCELADO | `StatusCobranca` |
| `origem_pagamento` | SITE, PRESENCIAL | `OrigemPagamento` |
| `tipo_notificacao` | VENCIMENTO_PROXIMO, INADIMPLENCIA | `TipoNotificacao` |
| `canal_notificacao` | EMAIL, SMS, WHATSAPP | `CanalNotificacao` |
| `status_notificacao` | PENDENTE, ENVIADA, FALHA | `StatusNotificacao` |
| `tipo_plano` | PLANO_AULA, PLANEJAMENTO_DIARIO | `TipoPlano` |
| `tipo_calendario` | AULA, FERIADO, RECESSO, EVENTO | `TipoCalendario` |
| `orgao_externo` | CENSO_INEP, SMTT, SISTEMA_PRESENCA, CORA | `OrgaoExterno` |
| `status_obrigacao` | PENDENTE, CUMPRIDA, ATRASADA | `StatusObrigacao` |

> Para gravar um enum via JDBC, a URL de conexão usa `stringtype=unspecified` (o Postgres converte o texto para o enum). Sem isso, seria preciso `?::status_cobranca` em todo `INSERT`.
>
> Novo valor: `ALTER TYPE nome ADD VALUE 'NOVO'` em uma nova migration **e** a constante correspondente no enum Java.

---

## 4. Regras automáticas (triggers)

A aplicação Java precisa saber o que acontece "sozinho" para não duplicar nem brigar com isso.

| Quando | O banco faz | Consequência para a aplicação Java |
|---|---|---|
| Qualquer `UPDATE` em tabela operacional | Atualiza `atualizado_em` | Não setar manualmente |
| Vincular `usuario_id` a responsável/aluno/funcionário | Exige perfil compatível | Criar `usuario` com o perfil certo antes |
| `INSERT` em `matricula` | Trava a turma; preenche `ano_letivo_id`; define `NOVA`/`REMATRICULA`; bloqueia a 31ª; cria contrato; cria cobrança de matrícula (ou ativa direto se valor = 0); cria alerta se lotou | Recarregar a matrícula depois do insert; tratar "Turma lotada" |
| Reduzir `turma.capacidade_max` | Bloqueia se ficar abaixo dos ocupados | Tratar erro na edição de turma |
| Atualizar `pre_matricula` já decidida | Bloqueia mudança de status | Tratar como 409 |
| Assinar `contrato` | Exige responsável vinculado ao aluno | 403 se não for |
| `INSERT` em `cobranca` | Valida tipo × nível (RN01), valida vínculo do responsável, calcula `desconto_bolsa` e `valor_final` | Enviar `valor_final = valor_bruto`; o trigger corrige |
| `UPDATE` em `cobranca` paga | Bloqueia | Nunca editar cobrança paga; criar ajuste |
| Marcar cobrança como `PAGO` | Exige origem; preenche `pago_em` e `valor_pago` | Usar `fn_registrar_pagamento` |
| Cobrança de **matrícula** paga | Muda a matrícula para `ATIVA` | Recarregar a matrícula |
| Mudar `bolsa` ou `bolsa_matricula` | Recalcula cobranças `PENDENTE`/`VENCIDO` | Pagas não mudam |
| `INSERT` em `turma_professor` | Só funcionário `PROFESSOR` | — |
| `INSERT` em `diario_classe`/`plano_aula` | Professor precisa estar na turma | 403 se não estiver |
| `INSERT` em `frequencia` | Matrícula precisa estar `ATIVA` na turma | 422 |
| `INSERT` em `calendario_letivo` | Data dentro do ano letivo | 422 |

---

## 5. Funções de negócio (a "API" do banco)

Chamadas pela aplicação Java com `SELECT fn_x(...)`. Cada uma roda em uma transação.

| Função | Retorno | Usada por (endpoint) |
|---|---|---|
| `fn_aprovar_pre_matricula(pre_id, funcionario_id, turma_id)` | `uuid` da matrícula | `POST /api/pre-matriculas/{id}/aprovar` |
| `fn_rejeitar_pre_matricula(pre_id, funcionario_id, motivo)` | `void` | `POST /api/pre-matriculas/{id}/rejeitar` |
| `fn_rematricular(aluno_id, turma_id, farda_uniforme, farda_ed_fisica)` | `uuid` da matrícula | `POST /api/matriculas/rematricula` |
| `fn_gerar_cobranca_matricula(matricula_id)` | `uuid` ou `NULL` | (chamada pelo trigger) |
| `fn_gerar_cobrancas_mensais(ano, mes)` | qtd criada | `POST /api/cobrancas/geracao` e `pg_cron` |
| `fn_gerar_cobrancas_periodo(ano, mes_ini, mes_fim)` | qtd criada | `POST /api/cobrancas/geracao` |
| `fn_registrar_pagamento(cobranca_id, origem, valor, data)` | `void` | `POST /api/cobrancas/{id}/pagamento` |
| `fn_registrar_pagamento_cora(cora_id, valor, data)` | `uuid` | `POST /api/webhooks/cora` |
| `fn_atualizar_cobrancas_vencidas()` | qtd | rotina diária |
| `fn_gerar_notificacoes_cobranca(dias, canal)` | qtd | rotina diária |
| `fn_desconto_bolsa(matricula_id, competencia, valor)` | `numeric` | simulação de valores |
| `fn_recalcular_cobrancas_abertas(matricula_id)` | `void` | (chamada por trigger) |
| `fn_atualizar_obrigacoes()` | qtd | rotina diária |
| `fn_cumprir_obrigacao(obrigacao_id)` | próxima data | `POST /api/obrigacoes/{id}/cumprir` |
| `fn_encerrar_ano_letivo(ano_letivo_id)` | qtd | `POST /api/anos-letivos/{id}/encerrar` |
| `fn_rotina_diaria()` | `void` | `pg_cron` às 06:00 |
| `fn_responsavel_financeiro(aluno_id)` | `uuid` | apoio |
| `fn_proxima_ocorrencia(mes, dia)` | `date` | apoio |

---

## 6. Integração com o Supabase Auth

Migration `20261028182449_integracao_supabase.sql`:

- `usuario.id` deixa de ter default e passa a ser **FK para `auth.users(id)`** (`ON DELETE CASCADE`). O id do usuário no Auth é o id na tabela `usuario`.
- A coluna `senha_hash` é **removida** (migration `20261028190000_remover_senha_hash.sql`): senha, recuperação e sessão ficam com o Supabase Auth.
- O e-mail continua em `usuario.email` (minúsculo, CHECK do script) para consultas e joins.

Como um usuário passa a existir:

| Quem | Como |
|---|---|
| Primeiro `ADMIN` | Criar no painel (Authentication → Add user) e depois `INSERT INTO usuario (id, email, perfil) VALUES ('<id do auth>', '<email>', 'ADMIN')` — passo a passo em [06-supabase-conexao.md](06-supabase-conexao.md#7-criar-o-primeiro-administrador) |
| Funcionário | `ADMIN` cadastra na tela de usuários → aplicação Java cria no Auth (Admin API, convite por e-mail) → insere `usuario` com o mesmo id → liga `funcionario.usuario_id` |
| Responsável | Cria conta no portal (`signUp`) → primeiro acesso chama `POST /api/portal/cadastro` → aplicação Java cria `usuario` (`RESPONSAVEL`) e o `responsavel` (ou liga a um já existente pelo CPF) |

---

## 7. Segurança no Supabase

Ainda na migration `integracao_supabase`:

1. **RLS ligado em todas as tabelas** do schema `public`, **sem políticas**. Resultado: as chaves públicas (`anon`/`publishable`) e usuários logados não leem nem gravam nada pela API REST do Supabase.
2. **Privilégios revogados** de `anon` e `authenticated` em tabelas, sequências e funções do `public` (inclusive para objetos criados no futuro).
3. **Views com `security_invoker = true`**: por padrão, view no Postgres roda com permissão do dono e ignoraria o RLS.

A aplicação Java conecta como `postgres` (dono das tabelas), por isso **não é afetada pelo RLS** e continua com acesso total. Toda autorização de negócio fica na aplicação Java.

> Se no futuro alguma tela precisar ler direto do Supabase (ex.: tempo real), crie uma política RLS específica para aquela tabela em uma nova migration. Não desligue o RLS.

---

## 8. Storage (arquivos)

Migration `20261028181952_storage_buckets.sql` cria buckets **privados**:

| Bucket | Conteúdo | Coluna que guarda o caminho |
|---|---|---|
| `documentos-alunos` | Os 7 documentos da matrícula | `documento_aluno.arquivo_url` |
| `contratos` | PDF do contrato / ficha de sócio | `contrato.arquivo_url` |
| `obrigacoes` | Comprovantes de obrigações | `documento_obrigacao.arquivo_url` |
| `formacao` | Certificados de formação continuada | `formacao_continuada.certificado_url` |

- As colunas `*_url` guardam o **caminho no bucket** (ex.: `documentos-alunos/<aluno_id>/certidao.pdf`), não uma URL pública.
- Upload e download passam pela aplicação Java, que usa a **chave secreta** e gera **URL assinada** com validade curta (ex.: 5 min).
- Sem políticas no `storage.objects`: o navegador não acessa os buckets diretamente.

---

## 9. Rotinas agendadas (`pg_cron`)

Ativar a extensão em *Database → Extensions → pg_cron* e executar uma vez no SQL Editor:

```sql
SELECT cron.schedule('semear-rotina-diaria', '0 6 * * *', 'SELECT fn_rotina_diaria()');

SELECT cron.schedule('semear-cobrancas-mes', '0 5 25 * *',
  $cron$ SELECT fn_gerar_cobrancas_mensais(
           extract(year  FROM current_date + interval '1 month')::int,
           extract(month FROM current_date + interval '1 month')::int) $cron$);
```

Horários em UTC (06:00 UTC = 03:00 em Brasília). Os jobs de **envio** (boletos no Cora, e-mails) rodam na aplicação Java com `@Scheduled` ([04-model-controller.md §9](04-model-controller.md#9-jobs-e-integrações)).

---

## 10. Views (consultas prontas)

| View | Tela que alimenta | Filtro obrigatório na aplicação Java |
|---|---|---|
| `vw_vagas_turma` | Turmas, escolha de turma na aprovação | — |
| `vw_pre_matriculas_pendentes` | Fila da secretaria | — |
| `vw_inadimplencia` | Inadimplência, checagem RN13 | — |
| `vw_resumo_financeiro_mensal` | Painel financeiro | — |
| `vw_bolsistas` | Bolsas | — |
| `vw_alertas_obrigacoes` | Painel / obrigações | — |
| `vw_turmas_do_professor` | Minhas turmas | `usuario_id = login` (se perfil PROFESSOR) |
| `vw_frequencia_aluno` | Frequência | professor: só suas turmas |
| `vw_portal_acesso` | Base do portal | `usuario_id = login` |
| `vw_portal_alunos`, `vw_portal_cobrancas`, `vw_portal_notas`, `vw_portal_materiais`, `vw_portal_contratos`, `vw_portal_documentos` | Portal | `aluno_id IN (SELECT aluno_id FROM vw_portal_acesso WHERE usuario_id = :login)` |

> Status "vencido" para telas: use as views, que calculam pela data. A coluna `cobranca.status` só vira `VENCIDO` quando a rotina diária roda.

---

## 11. Erros que o banco devolve

As funções e triggers lançam `RAISE EXCEPTION` com mensagem em português (SQLSTATE `P0001`). A aplicação Java traduz pelo trecho da mensagem para um **código estável** e um status HTTP ([04-model-controller.md §7](04-model-controller.md#7-exceções-e-tratamento-de-erros)).

| Trecho da mensagem | Código da API | HTTP |
|---|---|---|
| `lotada: limite de` | `TURMA_LOTADA` | 409 |
| `não possui responsável financeiro` | `SEM_RESPONSAVEL_FINANCEIRO` | 422 |
| `Cadastre a tabela_preco` | `TABELA_PRECO_AUSENTE` | 422 |
| `Pré-matrícula já está` / `já foi` / `já analisada` | `PRE_MATRICULA_JA_ANALISADA` | 409 |
| `Informe o motivo da rejeição` | `MOTIVO_OBRIGATORIO` | 422 |
| `não corresponde à série/ano letivo` | `TURMA_INCOMPATIVEL` | 422 |
| `Aluno sem matrícula anterior` | `SEM_MATRICULA_ANTERIOR` | 422 |
| `Cobrança já está paga` | `COBRANCA_JA_PAGA` | 409 |
| `Cobrança cancelada não pode ser paga` | `COBRANCA_CANCELADA` | 409 |
| `Cobrança paga não pode ser` | `COBRANCA_IMUTAVEL` | 409 |
| `Informe a origem do pagamento` | `ORIGEM_PAGAMENTO_OBRIGATORIA` | 422 |
| `só pode ser cobrada` / `só pode ser cobrado` | `TIPO_COBRANCA_INCOMPATIVEL` | 422 |
| `não está vinculado ao aluno` | `RESPONSAVEL_NAO_VINCULADO` | 422 |
| `assinante não é responsável` | `ASSINANTE_INVALIDO` | 403 |
| `Professor não está vinculado` | `PROFESSOR_FORA_DA_TURMA` | 403 |
| `não está ativa na turma deste diário` | `MATRICULA_INATIVA_NA_TURMA` | 422 |
| `fora do período do ano letivo` | `DATA_FORA_ANO_LETIVO` | 422 |
| `não encontrad` | `NAO_ENCONTRADO` | 404 |
| violação de `UNIQUE` (SQLSTATE `23505`) | `DUPLICADO` | 409 |
| violação de `CHECK` (`23514`) / FK (`23503`) | `DADO_INVALIDO` | 422 |

> Melhoria futura: trocar as mensagens por `RAISE EXCEPTION USING ERRCODE = 'SM001'` (códigos próprios) em uma migration, eliminando a tradução por texto.

---

## 12. Pontos de atenção

| Situação | O que acontece |
|---|---|
| Matrícula sem `tabela_preco` da série/ano | O insert falha e desfaz tudo → cadastrar preços antes de abrir matrículas |
| Aluno sem responsável financeiro | Matrícula falha; geração mensal pula o aluno sem erro |
| `valor_matricula = 0` | Matrícula ativada na hora, sem cobrança |
| Matrícula inserida já como `ATIVA` | Não gera cobrança de matrícula |
| Vigência de bolsa | Vale no mês se houver qualquer interseção; bolsas somam, limitadas ao bruto |
| Troca de turma entre níveis | Não há função; revisar cobranças em aberto manualmente |
| Script nunca executado antes | A doc original avisa: rodar em um projeto de teste primeiro e corrigir o que aparecer |

---

## 13. Migrations: como evoluir o esquema

- Arquivos em `supabase/migrations/` no formato `AAAAMMDDHHMMSS_descricao.sql`, aplicados em ordem.
- **Nunca editar uma migration já aplicada.** Criar outra.
- Criar uma nova: `supabase migration new descricao` → escrever o SQL → `supabase db push`.
- Novo valor de enum, nova coluna ou nova tabela: migration + atualização do domínio Java + atualização deste documento.
- Antes de mudar um trigger: ler a ordem de disparo (doc original §6.1) e rodar o roteiro de teste (`seed.sql`).

---

## 14. Lacunas entre o domínio e o banco

O `MODELO.md` e as fichas da escola pedem dados que o script ainda não tem. **Não bloqueiam o início**, mas devem virar migrations quando o módulo for implementado:

| Lacuna | Proposta | Prioridade |
|---|---|---|
| Endereço (aluno, responsável, funcionário) | Colunas de endereço ou tabela `endereco` | Alta |
| Aluno: sexo, autoriza uso de imagem, pessoas autorizadas a buscar | Colunas em `aluno` + tabela `pessoa_autorizada` | Alta |
| Ficha de saúde do aluno (sensível) | Tabela separada `aluno_saude` (acesso restrito) | Média |
| Responsável: estado civil, profissão, RG | Colunas em `responsavel` | Média |
| Ficha de Sócio: tipo de sócio, nº de filhos, datas de associação/afastamento | Tabela `socio` ligada ao `responsavel`; enum `tipo_socio` (FUNDADOR, EFETIVO, MANTENEDOR_C1..C3) | Média |
| Funcionário: RG, filiação, documentos de admissão, autodeclaração (sensível) | Colunas + tabela `documento_funcionario` | Média |
| Tipo de documento do aluno como texto livre | Enum `tipo_documento_aluno` com os 7 documentos (RN12) | Alta |
| Desconto de pontualidade, multa e juros fixos no código | Tabela `parametro_financeiro` (10%, 2%, 0,033%/dia, 5º dia útil) para a escola ajustar sem deploy | Média |
| Atividades extras (Jiu Jitsu / Ballet, R$ 50) | Tabela `atividade_extra` + matrícula na atividade + tipo de cobrança próprio | Baixa |
| Dados da escola (nome, endereço, CNPJ) | Tabela `configuracao_escola` | Baixa |
| Auditoria (quem mudou bolsa/cobrança) | Tabela `auditoria` + trigger genérico | Média |

---

## 15. Dados de desenvolvimento

- `supabase/seed.sql` cria ano letivo 2027, turmas, preços da tabela oficial, uma secretária, um responsável e uma pré-matrícula aprovada — **todos fictícios**.
- Roda automaticamente em `supabase db reset` (ambiente local). Em projeto remoto, colar no SQL Editor.
- Depois do seed, confirmar: matrícula existe, contrato do tipo certo, cobrança de matrícula `PENDENTE`.
