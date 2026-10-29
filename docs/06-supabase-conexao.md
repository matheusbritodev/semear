# 06 — Conexão com o projeto Supabase

> Projeto: **`bvbjltcvwrrnsqekpgfq`** — `https://bvbjltcvwrrnsqekpgfq.supabase.co`
> O banco já foi criado pelo Claude Code via MCP (ver §1). Este guia cobre o que falta e serve de referência para recriar o ambiente.

---

## 1. Situação atual

| Item | Status |
|---|---|
| Conexão do Claude Code com o Supabase (MCP) | ✅ `.mcp.json` na raiz |
| Migration `schema_inicial` (32 tabelas, 15 views, 37 funções, 31 triggers, dados iniciais) | ✅ aplicada |
| Migration `storage_buckets` (4 buckets privados) | ✅ aplicada |
| Migration `integracao_supabase` (usuario ↔ auth.users, RLS, bloqueio da API pública) | ✅ aplicada |
| Migration `remover_senha_hash` | ✅ executada no SQL Editor |
| Verificador de segurança do Supabase | ✅ só avisos informativos "RLS sem políticas" (intencional, ver [03 §7](03-banco-de-dados.md#7-segurança-no-supabase)) |
| Dados de desenvolvimento (`seed.sql`) | ✅ aplicado em 28/10/2026 (cenário realista, ver §4) |
| `.env` da raiz | ✅ preenchido; conexão testada (`/api/saude` = UP) |
| Primeiro ADMIN e contas de demonstração (`demo_usuarios.sql`) | ✅ login testado de ponta a ponta |
| URLs do Auth (Site URL / Redirect URLs) | ⏳ necessário só para e-mails de confirmação e recuperação de senha (§6) |
| `pg_cron` | ⏳ Etapa 3 (§8) |

### Contas de acesso (desenvolvimento)

| Perfil | E-mail | Pessoa no sistema |
|---|---|---|
| ADMIN | `matheusbritodev1@gmail.com` (senha própria) | Matheus Brito (Gestor) |
| SECRETARIA | `secretaria.semear@example.com` | Ana Paula Costa Diniz |
| FINANCEIRO | `financeiro.semear@example.com` | Rosângela Martins Bogéa |
| PROFESSOR | `professora.semear@example.com` | Márcia Regina Castro (1º Ano A) |
| RESPONSAVEL | `responsavel.semear@example.com` | Mãe de 2 alunos, pagamentos em dia |

As contas de demonstração compartilham uma senha provisória, informada fora da documentação. Troque-a antes de mostrar o sistema a terceiros.

---

## 2. Onde achar cada valor no painel

| Valor | Onde fica | Vai para |
|---|---|---|
| Project URL | Botão **Connect** ou *Project Settings → Data API* | `SUPABASE_URL`, `VITE_SUPABASE_URL` |
| Publishable key (`sb_publishable_...`) | *Project Settings → API Keys* | `VITE_SUPABASE_PUBLISHABLE_KEY` |
| Secret key (`sb_secret_...`) | *Project Settings → API Keys* | `SUPABASE_SECRET_KEY` (**só servidor**) |
| Session pooler (host, usuário) | Botão **Connect → Session pooler** | `SUPABASE_DB_URL`, `SUPABASE_DB_USER` |
| Senha do banco | Definida na criação (redefinir em *Database Settings*) | `SUPABASE_DB_PASSWORD` |
| JWT signing keys | *Project Settings → JWT Keys* | validação do login (JWKS) |

> ⚠️ A **secret key** e a **senha do banco** dão acesso total. Nunca em variável `VITE_`, no Git ou em prints.

---

## 3. Migrations

Arquivos em `supabase/migrations/`, com a mesma versão registrada no servidor:

| Versão | Arquivo | Estado |
|---|---|---|
| 20261028181937 | `schema_inicial.sql` | aplicada |
| 20261028181952 | `storage_buckets.sql` | aplicada |
| 20261028182449 | `integracao_supabase.sql` | aplicada |
| 20261028190000 | `remover_senha_hash.sql` | executada manualmente no SQL Editor (não consta no histórico do servidor; `IF EXISTS` a torna inofensiva se reaplicada) |

### Mudanças futuras no banco

- Pelo Claude Code (MCP): peça a mudança; ele aplica e cria o arquivo correspondente em `supabase/migrations/`.
- Pelo Supabase CLI: `npx supabase migration new nome` → escrever o SQL → `npx supabase db push`.
- **Nunca** editar uma migration já aplicada; criar outra.

---

## 4. Dados de desenvolvimento

`supabase/seed.sql` monta um cenário realista e **totalmente fictício** do Centro Educacional Semeando (São Luís/MA) em 28/10/2026:

| Item | Conteúdo |
|---|---|
| Anos letivos | 2025 (encerrado), 2026 (em andamento), 2027 (rematrícula aberta) |
| Turmas 2026 | uma por série; capacidades de 15 (Maternal 1) a 30 (Fundamental); **Infantil 2 lotada (20/20)** com alerta à secretaria |
| Pessoas | 160 alunos em 2026, ~140 famílias (irmãos, pai/mãe/avó), 16 funcionários (gestão, secretaria, financeiro, 11 professores, 2 prestadores) |
| Financeiro | preços do anexo didático; mensalidades/taxas de fev a dez; famílias pontuais (desconto de 10%), atrasadas (multa 2% + juros 0,033%/dia) e inadimplentes; bolsas (irmãos, social, funcionário, convênio); boletos "Cora" simulados |
| Pedagógico | calendário 2026 com feriados de São Luís, diário e frequência de ago a out, notas do 1º ao 3º bimestre (Fundamental), planos de novembro, formação continuada |
| 2027 | 52 rematrículas (só adimplentes, RN13), 10 pré-matrículas (7 pendentes, 2 aprovadas, 1 rejeitada), preços com reajuste |

CPFs são inventados (com dígito verificador válido) e e-mails usam `example.com`. Rode **apenas** em desenvolvimento; depois, `supabase/demo_usuarios.sql` vincula os logins aos perfis (criar os usuários no Auth antes).

**Cora e tarefas agendadas:** com `CORA_MODO=falso`, preencha `CORA_WEBHOOK_SEGREDO` com um valor aleatório longo (o webhook recusa tudo sem ele). `SEMEAR_JOBS_ATIVOS=true` liga a emissão de boletos (15 min), o envio de avisos (10 min, só em log) e a rotina diária das 06h; use `false` para rodar sem escrever no banco sozinho.

**Documentos (RN12):** o seed SQL não envia arquivos. Depois dele, rode na raiz do projeto:

```bash
java -cp ~/.m2/repository/org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar supabase/scripts/SeedDocumentos.java .env
```

Ele envia um PDF fictício por documento ao bucket `documentos-alunos` e grava `documento_aluno`: veteranos quase todos completos, parte dos novos de 2026 com 1 a 3 faltando e os aprovados para 2027 com quase tudo pendente (resultado em 28/10/2026: 936 documentos, 23 de 162 alunos com pendência). Pode rodar de novo: alunos que já têm documentos são ignorados.

---

## 5. Configurar a aplicação Java

1. Copie `.env.example` (raiz) para `.env` e preencha (§2).
2. A conexão usa o **Session pooler** (porta 5432), que funciona em redes só com IPv4:

```properties
SUPABASE_URL=https://bvbjltcvwrrnsqekpgfq.supabase.co
SUPABASE_DB_URL=jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require&stringtype=unspecified
SUPABASE_DB_USER=postgres.bvbjltcvwrrnsqekpgfq
SUPABASE_DB_PASSWORD=<senha do banco>
SUPABASE_SECRET_KEY=sb_secret_...
```

- Copie o **host** exatamente como aparece em *Connect* (prefixo `aws-0`/`aws-1` e região variam).
- `stringtype=unspecified` permite gravar os `ENUM`s do Postgres como texto.
- Pool pequeno (`DB_POOL_MAX=5`): o plano gratuito limita conexões.
- Transaction pooler (porta 6543) exige `prepareThreshold=0` na URL.

### Validação do login (JWT)

```
JWKS: https://bvbjltcvwrrnsqekpgfq.supabase.co/auth/v1/.well-known/jwks.json
iss:  https://bvbjltcvwrrnsqekpgfq.supabase.co/auth/v1
aud:  authenticated
sub:  id do usuário = usuario.id
```

Se *Project Settings → JWT Keys* oferecer migração para chaves assimétricas, migre. No segredo legado (HS256), a configuração de segurança precisa usar `NimbusJwtDecoder.withSecretKey(...)`.

### Pré-requisitos na máquina

- **JDK 21** e **Maven 3.9+** (ainda não instalados nesta máquina): `winget install EclipseAdoptium.Temurin.21.JDK` e `winget install Apache.Maven`.
- Node.js 20+ (já instalado: v24).

Teste quando a Etapa 0 estiver pronta: `mvn spring-boot:run` → `http://localhost:8080/api/saude` responde `{"status":"UP","banco":"OK"}`.

---

## 6. Configurar a View e o Auth

1. No mesmo `.env` da raiz: `VITE_SUPABASE_URL`, `VITE_SUPABASE_PUBLISHABLE_KEY`, `VITE_API_URL=/api`.
2. *Authentication → URL Configuration*: **Site URL** `http://localhost:5173`; **Redirect URLs** `http://localhost:5173/**` (e depois o domínio de produção).
3. *Authentication → Sign In / Providers → Email*: manter **Confirm email** ligado. A tela *Criar conta* não depende disso: a aplicação Java cria a conta já confirmada (decisão D10). Só a **recuperação de senha** precisa de e-mail; sem SMTP próprio, o Supabase só entrega para membros da equipe do projeto.
4. *Authentication → Emails*: traduzir os modelos (confirmação, convite, recuperação) para português.

---

## 7. Criar o primeiro administrador

Depois de rodar a remoção da `senha_hash` (§3):

1. *Authentication → Users → Add user → Create new user*: e-mail e senha, marque **Auto Confirm User**.
2. Copie o **User UID**.
3. No SQL Editor:

```sql
INSERT INTO usuario (id, email, perfil)
VALUES ('<USER UID>', 'admin@escola.exemplo', 'ADMIN');

INSERT INTO funcionario (usuario_id, nome, cpf, tipo)
VALUES ('<USER UID>', 'Administrador do Sistema', '00000000191', 'GESTOR');
```

(CPF fictício válido; e-mail em minúsculas.) Os demais usuários da escola são convidados pela tela **Configurações → Usuários**.

---

## 8. Agendamentos (`pg_cron`)

1. *Database → Extensions* → ativar **pg_cron**.
2. Criar os dois jobs de [03-banco-de-dados.md §9](03-banco-de-dados.md#9-rotinas-agendadas-pg_cron).
3. Conferir: `SELECT jobname, schedule FROM cron.job;`

---

## 9. Checklist

- [x] Migrations de esquema, segurança e storage aplicadas
- [ ] `senha_hash` removida (§3)
- [ ] Seed aplicado (só em desenvolvimento)
- [ ] `.env` preenchido; nenhum `.env` aparece em `git status`
- [ ] JWT Keys assimétricas ativas
- [ ] Site URL e Redirect URLs configurados
- [ ] Primeiro ADMIN criado
- [ ] `pg_cron` ativo com os 2 jobs
- [ ] JDK 21 + Maven instalados; `/api/saude` responde `UP`

---

## 10. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `UnknownHostException` / timeout em `db.<ref>.supabase.co` | Rede sem IPv6 | Usar o Session pooler |
| `password authentication failed` | Usuário sem o sufixo `.<ref>` | `SUPABASE_DB_USER=postgres.bvbjltcvwrrnsqekpgfq` |
| `column "status" is of type status_cobranca but expression is of type character varying` | Faltou `stringtype=unspecified` | Acrescentar na URL JDBC |
| `prepared statement "S_1" already exists` | Transaction pooler (6543) | Usar 5432 ou `prepareThreshold=0` |
| `null value in column "senha_hash"` | Migration pendente | Rodar §3 |
| 401 em todas as chamadas | JWKS/issuer errados ou projeto em HS256 | Conferir §5 |
| 403 logo após login | Usuário existe no Auth mas não em `usuario` | Inserir em `usuario` (§7) ou completar cadastro |
| `permission denied for table ...` vindo do navegador | Esperado: a API pública está fechada | Chamar os Controllers |
