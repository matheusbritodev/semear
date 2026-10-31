# 10 — Controles internos (SoD e SOX)

Orientação do professor em 31/10/2026: implementar **segregação de funções (SoD)** e controles no estilo **SOX**.
A Sarbanes-Oxley é uma lei americana para empresas de capital aberto; uma escola não está sujeita a ela.
O que se aplica aqui são as práticas que ela popularizou para proteger o dinheiro e os registros:
ninguém faz sozinho uma operação sensível, toda mudança deixa rastro, o rastro não pode ser apagado e os acessos são revistos periodicamente.

## 1. Matriz de riscos e controles

| # | Risco | Controle | Tipo | Onde está | Evidência |
|---|---|---|---|---|---|
| C1 | Funcionário concede desconto indevido (bolsa para conhecido) | **Dupla aprovação**: conceder bolsa vira pedido; outra pessoa com o módulo Aprovações decide | Preventivo | `SolicitacaoAprovacao`, `AprovacaoService`, tela **Aprovações** | Tabela `solicitacao_aprovacao` (quem pediu, quem decidiu, quando, motivo) |
| C2 | Aumentar o percentual de uma bolsa já concedida a muitos alunos | Mudar tipo/valor do desconto ou reativar bolsa também vai para aprovação (`Bolsa.alteracaoExigeAprovacao`) | Preventivo | `SolicitacaoAlteracaoBolsa` | idem |
| C3 | Mudar preço para favorecer alguém ou desviar a diferença | Toda mudança de preço (uma série ou cópia do ano) vai para aprovação | Preventivo | `SolicitacaoPreco`, `SolicitacaoCopiaPrecos` | idem |
| C4 | Aprovar o próprio pedido | Quem pediu não decide: regra no Model (`AUTOAPROVACAO`) **e** no banco (`ck_solicitacao_sod`) | Preventivo | `SolicitacaoAprovacao.aprovar` | Tentativa recusada com 403 |
| C5 | Funcionário que é pai/tia do aluno decide sobre a própria família | **Conflito de interesse** por CPF: não pede bolsa, não aprova/recusa pedido e não dá baixa presencial em cobrança dessa família | Preventivo | `ConflitoInteresse`, `PedidosAprovacao`, `CobrancaService.registrarPagamento` | Recusa com `CONFLITO_DE_INTERESSE` |
| C6 | Um perfil acumular funções incompatíveis (ex.: matricular e dar baixa) | **Matriz de funções incompatíveis**: a tela de perfis recusa a combinação (`CONFLITO_SOD`) | Preventivo | `PoliticaSegregacao`, `MatrizPermissoes.alterar` | Painel em **Perfis de acesso** |
| C7 | Alteração sem rastro em dinheiro, acessos ou contratos | **Trilha de auditoria** automática (trigger `fn_auditar`): quem, quando, operação, antes e depois | Detectivo | Banco + tela **Auditoria** | Tabela `auditoria` |
| C8 | Apagar ou editar a trilha para esconder algo | Trilha **imutável** (trigger recusa UPDATE/DELETE/TRUNCATE) e **hash encadeado** (SHA-256 de cada registro inclui o hash do anterior); o Java recalcula a cadeia | Detectivo | `CadeiaAuditoria`, `RegistroAuditoria`, botão **Verificar integridade** | Resultado da verificação |
| C9 | Cortar o fim da trilha (apagar os últimos registros) | Cada revisão de acessos guarda uma **âncora** (último id e hash); a verificação confere se ela continua lá | Detectivo | `CadeiaAuditoria.conferirAncora` | `revisao_acesso.resultado.ancora` |
| C10 | Ex-funcionário com login ativo, contas sem dono, acesso total desnecessário | **Revisão trimestral de acessos**: relatório com alertas e registro de quem revisou | Detectivo | `RevisaoAcesso`, tela **Revisão de acessos** | Tabela `revisao_acesso` (foto das contas) |
| C11 | Gestor se trancar fora ou tirar o último administrador | `AdministracaoContas` (sem mudar o próprio acesso, último admin protegido) e `MatrizPermissoes` (ADMIN mantém Configurações e Usuários) | Preventivo | já existentes | — |

## 2. Segregação de funções

### 2.1 Funções incompatíveis (por perfil)

Um mesmo perfil não pode **alterar** os dois módulos de uma linha:

| Código | Módulos | Por quê |
|---|---|---|
| SOD-01 | Usuários × Financeiro | Quem cria logins poderia criar um acesso só para dar baixa |
| SOD-02 | Usuários × Bolsas | Quem cria logins poderia criar um segundo acesso para aprovar o próprio pedido |
| SOD-03 | Configurações × Financeiro | Quem define preços poderia baixar o preço, receber o valor cheio e ficar com a diferença |
| SOD-04 | Matrículas × Financeiro | Quem matricula e escolhe o responsável financeiro não deve dar baixa nas cobranças dessa família |
| SOD-05 | Configurações × Aprovações | Quem altera preços e perfis não deve ser quem aprova alterações de preço |

Consultar (só ler) não conta. O padrão de fábrica já respeita a matriz.

**Exceção documentada:** o perfil Administração (gestor) acumula funções porque precisa administrar a escola inteira.
O risco é coberto por controles compensatórios: o gestor também passa pela dupla aprovação (não aprova os próprios pedidos de preço),
tudo o que ele faz fica na trilha de auditoria, e a revisão de acessos aponta o acesso total como alerta a confirmar.

### 2.2 Quem pede × quem aprova (padrão)

| Pedido | Quem pede | Quem aprova |
|---|---|---|
| Conceder bolsa / mudar desconto | Financeiro ou gestor (módulo Bolsas) | Outra pessoa com Aprovações (gestor ou outro do Financeiro) |
| Mudar preços | Gestor (módulo Configurações) | Outra pessoa com Aprovações (Financeiro) |

Revogar bolsa, desativar bolsa e mudar nome/descrição valem na hora: só reduzem desconto ou não mexem em valores.

## 3. Trilha de auditoria

- **Tabelas auditadas:** `cobranca`, `bolsa`, `bolsa_matricula`, `tabela_preco`, `solicitacao_aprovacao`, `matricula`, `contrato`, `usuario`, `perfil_permissao`, `revisao_acesso`, `ano_letivo`.
- **Autoria:** a cada conexão tirada do pool a aplicação informa o login (`set_config('semear.usuario_id', ...)`, em `DataSourceComUsuario`); o trigger lê esse valor. Sem login (rotinas automáticas, webhook do Cora ou acesso direto ao banco) aparece como "Sistema". A coluna `papel_banco` guarda o usuário do PostgreSQL.
- **Hash:** `sha256(id | data UTC | login | papel | tabela | operação | registro | antes | depois | hash anterior)`. O banco grava; o Java (`RegistroAuditoria.hashCalculado`) recalcula sem confiar no valor gravado.
- **Uma gravação por vez na cadeia:** `pg_advisory_xact_lock` dentro do trigger garante a ordem.
- **Limite conhecido:** quem tem a senha do dono do banco pode desligar o trigger. A cadeia e as âncoras **detectam** a adulteração, mas não a impedem; em produção a senha do dono fica só com a TI, e a aplicação usaria um usuário sem esse poder.

## 4. Revisão de acessos

Relatório em **Revisão de acessos** (módulo Usuários). Alertas por conta da equipe (contas desativadas não geram alerta):

| Alerta | Quando |
|---|---|
| Funcionário desligado com login ativo | Cadastro de funcionário inativo e login ativo |
| Sem vínculo | Login da equipe sem cadastro de funcionário |
| Funções incompatíveis | O perfil fere a matriz de SoD (fora da exceção do gestor) |
| Nunca acessou / sem acesso há 90 dias | Último login no Supabase Auth |
| Acesso total | Perfil Administração: confirmar que ainda é necessário |

Registrar a revisão guarda a foto das contas e alertas e a âncora da trilha. Próxima revisão: 90 dias depois; sem revisão, aparece como vencida.

## 5. Dados de demonstração

- **Conflito de interesse:** a funcionária do Financeiro (**Rosângela Martins Bogéa**) também é **tia** (responsável não financeira, fictícia) de **Ágatha Nascimento Viana**. Logada como Financeiro, ela não consegue pedir bolsa para a Ágatha nem dar baixa nas cobranças dela.
- **Pedido aguardando aprovação:** Bolsa Funcionário (50%) para Rafael Diniz Pinheiro (2027), pedida pelo Financeiro. O gestor aprova ao vivo; o Financeiro, se tentar, recebe "quem fez o pedido não pode decidir".

## 6. Classes (POO)

| Classe | Papel |
|---|---|
| `SolicitacaoAprovacao` (abstrata) | **Template Method**: `aprovar`/`rejeitar`/`cancelar` finais com as mesmas regras; subclasses dizem o que é pedido |
| `SolicitacaoConcessaoBolsa`, `SolicitacaoAlteracaoBolsa`, `SolicitacaoPreco`, `SolicitacaoCopiaPrecos` | Subclasses (herança e polimorfismo); validam os dados já no pedido reaproveitando `Bolsa` e `TabelaPreco` |
| `EfeitosAprovacao` + `EfeitosAprovacaoServicos` | **Despacho duplo**: cada pedido chama o seu efeito; o Model decide qual, o service grava |
| `SolicitacaoFactory` | **Factory**: reconstrói a subclasse a partir do tipo e do jsonb |
| `Decisor` | Value object de quem decide (login, permissão, CPF) |
| `PoliticaSegregacao` | Regras de SoD (matriz de funções incompatíveis) |
| `ConflitoInteresse` | Regra de conflito por CPF |
| `RegistroAuditoria`, `CadeiaAuditoria`, `AlteracaoCampo` | Recalcular e conferir a cadeia de hashes; mostrar só o que mudou |
| `RevisaoAcesso` | Alertas da revisão trimestral |

Testes: `SegregacaoFuncoesTest`, `SolicitacaoAprovacaoTest`, `TrilhaAuditoriaTest` (24 casos).
