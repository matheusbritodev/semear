# 09 — Roteiro da demonstração

Sequência de cerca de 15 minutos que mostra o sistema por perfil, com os dados de demonstração já preparados.
Contas em [README](../README.md#contas-de-demonstração). Use duas janelas (uma anônima) para trocar de perfil sem sair.

## Antes de começar

```bash
npm run build && mvn package
SEMEAR_JOBS_ATIVOS=false java -jar target/semear-0.1.0-SNAPSHOT.jar   # tudo em http://localhost:8080
```

Com `SEMEAR_JOBS_ATIVOS=false` o sistema não gera boletos nem avisos sozinho durante a apresentação.
Confira `http://localhost:8080/api/saude`: deve mostrar `"banco":"OK"` e `"fusoBanco":"America/Fortaleza"`.

**Preparado para a demonstração:** um pedido de bolsa do Financeiro (Rafael Diniz Pinheiro, 2027) aguardando aprovação, e o Contrato de Matrícula 2027 de **Heitor Martins Cardoso** (filho da conta responsável) está **pendente**, para ser assinado ao vivo.

## 1. Responsável (portal) — 4 min

1. Entrar como `responsavel.semear@example.com`. O início mostra os dois filhos: Davi (faltam 3 documentos) e Heitor (**há um documento para assinar**).
2. Clicar no aviso do Heitor → página do filho: contrato, **boletim de 2026** (notas por bimestre, média parcial, 96,6% de presença) e **lista de materiais** de 2027.
3. **Ler e assinar** o contrato: mostrar as cláusulas com os valores da tabela, marcar "Li e concordo" e assinar. O termo passa a mostrar "assinado pelo Portal" e o **código de verificação** do texto congelado.
4. Aba **Financeiro** → abrir uma cobrança em aberto → composição do valor (desconto de 10% até o vencimento) → **Simular pagamento do boleto** (Cora simulado).

## 2. Secretaria — 4 min

1. Entrar como `secretaria.semear@example.com`. No painel: aviso de **turma lotada** (Infantil 2 A) e card de **23 alunos com documentos pendentes**.
2. **Pendências** → filtro por turma → botão **Cobrar no WhatsApp** (mensagem já escrita).
3. Abrir um aluno (ex.: Helena Viana Barbosa): alerta de pendência no topo e checklist de documentos.
4. **Pré-matrículas** → abrir uma pendente → escolher turma (com vagas) e **aprovar**. Mostrar que a turma lotada não aceita.
5. **Obrigações** → cartão do Censo mostra os alunos com documentos pendentes; anexar um comprovante e marcar como cumprida (a próxima data é agendada sozinha).

## 3. Financeiro — 3 min

1. Entrar como `financeiro.semear@example.com`. **Cobranças** abre no mês atual. Abrir uma vencida → multa de 2% e juros de 0,033% ao dia, e **simular** o valor em outra data.
2. **Inadimplência**: 36 alunos, maior atraso primeiro; quem está aqui não consegue rematricular (RN13).
3. **Resumo mensal**: previsto × recebido × em atraso.
4. **Aprovações**: o pedido de bolsa do Rafael aparece como "aguardando outra pessoa". Mostrar que o Financeiro não consegue aprová-lo (segregação de funções).
5. **Conflito de interesse**: abrir a matrícula de **Ágatha Nascimento Viana** (a funcionária é tia dela) → pedir bolsa → o sistema recusa. O mesmo vale para dar baixa numa cobrança dela.

## 4. Professora — 2 min

1. Entrar como `professora.semear@example.com` → **1º Ano A**.
2. **Diário e chamada** do dia: marcar uma falta com justificativa e salvar. Tentar um feriado (12/10) para mostrar o bloqueio.
3. **Notas**: Língua Inglesa e Educação Física aparecem só para consulta (são dos especialistas).
4. **Frequência**: destaque para quem estiver abaixo de 75%.

## 5. Gestão — 2 min

1. Entrar com a conta de administração → o início mostra **1 pedido aguardando aprovação** → **Aprovações** → aprovar a bolsa do Rafael (as cobranças em aberto são recalculadas).
2. **Ano letivo e preços** → editar um preço: vira pedido; o gestor não consegue aprovar o próprio pedido (o Financeiro aprova).
3. **Perfis de acesso**: tentar dar "Alterar" no Financeiro para a Secretaria → recusado (SOD-04). Mostrar o painel de funções incompatíveis. Desligar "Pendências" da Secretaria e **Restaurar padrão**.
4. **Auditoria**: a aprovação de agora aparece com autor, antes e depois → **Verificar integridade** (cadeia de hashes).
5. **Revisão de acessos**: alertas por conta → **Registrar revisão**.
6. **Calendário letivo**: feriados e recessos que bloqueiam a chamada.

## Pontos para comentar (POO)

- `Cobranca` com três subclasses e o cálculo em **Template Method**; multa e juros como **Strategy** (`RegraEncargos`); a matrícula usa um **Null Object** (`SemEncargos`).
- `Contrato` → `ContratoMatricula` / `FichaSocio`: mesmo roteiro de cláusulas, valores diferentes por nível.
- `Turma.garantirVaga` (RN04) e `Rematricula.avaliar` (RN13): a regra mora no objeto, não na tela.
- `SolicitacaoAprovacao` (Template Method) com quatro subclasses e despacho duplo (`EfeitosAprovacao`); `CadeiaAuditoria` recalcula o hash de cada registro.
- Detalhes em [08-entrega.md](08-entrega.md) e [10-controles-internos.md](10-controles-internos.md).
