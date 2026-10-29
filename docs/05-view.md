# 05 — View (TypeScript + React)

> A camada **View** do MVC: as telas que o usuário vê, em `src/main/view`. Tem duas áreas: **Área da escola** (ADMIN, SECRETARIA, FINANCEIRO, PROFESSOR) e **Portal** (RESPONSAVEL, ALUNO). Fala com o Supabase **só para login**; todo dado vem dos Controllers Java ([04-model-controller.md §10](04-model-controller.md#10-endpoints-da-api)).

---

## 1. Stack

| Item | Escolha | Uso |
|---|---|---|
| Linguagem | **TypeScript** (modo `strict`) | Tipagem em toda a View |
| Base | React 19 + Vite (Tailwind CSS 4) | SPA compilada para `target/classes/static` |
| Rotas | React Router v7 | Rotas por perfil, rotas protegidas |
| Dados do servidor | TanStack Query v5 | Cache, carregamento, revalidação após mutações |
| Formulários | React Hook Form + Zod | Validação no cliente (espelha a do Model) |
| UI | Tailwind CSS + shadcn/ui + lucide-react | Componentes acessíveis |
| Autenticação | `@supabase/supabase-js` v2 | `signInWithPassword`, `signUp`, `resetPasswordForEmail`, sessão |
| HTTP | `fetch` encapsulado na classe `HttpClient` | Injeta o token, trata erros pelo `codigo` |
| Datas | date-fns (locale `pt-BR`) | Formatação |
| Testes | Vitest + Testing Library (`src/test/view`) | Componentes e utilitários |

---

## 2. Estrutura de pastas

```
src/main/view/
├── index.html
├── main.tsx                       # ponto de entrada
├── app/
│   ├── App.tsx
│   ├── routes.tsx                 # todas as rotas
│   └── providers.tsx              # QueryClient, AuthProvider, tema
├── config/
│   └── env.ts                     # lê e valida import.meta.env (VITE_*)
├── lib/
│   ├── supabase.ts                # createClient (só Auth)
│   ├── HttpClient.ts              # classe: fetch + token + erros
│   ├── ApiError.ts                # erro tipado + mensagens por código
│   └── formatters.ts              # cpf, moeda, data, telefone
├── auth/
│   ├── AuthProvider.tsx           # sessão Supabase + GET /api/me
│   ├── useAuth.ts
│   ├── ProtectedRoute.tsx         # exige login e (opcional) perfil/permissão
│   └── permissions.ts             # pode(modulo, acao)
├── models/                        # tipos TS espelhando os DTOs Java
│   ├── enums.ts                   # mesmos valores dos enums do banco + rótulos em português
│   └── aluno.ts, matricula.ts, cobranca.ts, ...
├── services/                      # uma classe por Controller Java
│   ├── ApiService.ts              # classe base abstrata
│   └── AlunoService.ts, PreMatriculaService.ts, CobrancaService.ts, ...
├── hooks/                         # useAlunos, useAprovarPreMatricula... (TanStack Query)
├── components/
│   ├── ui/                        # shadcn/ui (gerados)
│   └── comuns/                    # DataTable, StatusBadge, CpfInput, MoneyInput, ConfirmDialog, EmptyState, Loading
├── layouts/
│   ├── PublicLayout.tsx           # login, cadastro, recuperar senha
│   ├── EscolaLayout.tsx           # menu lateral montado pelas permissões
│   └── PortalLayout.tsx           # navegação simples do responsável
├── pages/
│   ├── publico/                   # Login, Cadastro, RecuperarSenha
│   ├── escola/                    # subpastas por módulo: matricula/, financeiro/, academico/...
│   └── portal/                    # Inicio, PreMatricula, Cobrancas, Contratos...
└── styles/
    └── globals.css                # Tailwind
```

**Nomes:** pastas e sufixos técnicos em inglês (`pages`, `services`, `*Service`, `*Layout`); nomes do domínio em português (`PreMatricula`, `Cobranca`, `FilaPreMatriculas`). Componentes e classes em `PascalCase.tsx/.ts`; hooks começam com `use`.

### Orientação a objetos na View

Cada Controller Java tem um *service* TypeScript correspondente, herdando de uma classe base:

```ts
// services/ApiService.ts
export abstract class ApiService<T, TInput = Partial<T>> {
  constructor(protected readonly http: HttpClient, protected readonly recurso: string) {}
  listar(params?: Record<string, string>) { return this.http.get<Pagina<T>>(`/${this.recurso}`, params); }
  buscar(id: string)                      { return this.http.get<T>(`/${this.recurso}/${id}`); }
  criar(dados: TInput)                    { return this.http.post<T>(`/${this.recurso}`, dados); }
  atualizar(id: string, dados: TInput)    { return this.http.put<T>(`/${this.recurso}/${id}`, dados); }
}

// services/PreMatriculaService.ts  ↔  controller/matricula/PreMatriculaController.java
export class PreMatriculaService extends ApiService<PreMatricula, NovaPreMatricula> {
  constructor(http: HttpClient) { super(http, 'pre-matriculas'); }
  aprovar(id: string, turmaId: string) { return this.http.post<{ matriculaId: string }>(`/pre-matriculas/${id}/aprovar`, { turmaId }); }
  rejeitar(id: string, motivo: string) { return this.http.post<void>(`/pre-matriculas/${id}/rejeitar`, { motivo }); }
}
```

Fluxo dentro da View: **page → hook → service → HttpClient → Controller**. Componentes nunca chamam `fetch` direto.

---

## 3. Autenticação e sessão

1. `lib/supabase.ts` cria o cliente com `VITE_SUPABASE_URL` e `VITE_SUPABASE_PUBLISHABLE_KEY` (chave pública, pode ficar no navegador).
2. Login: `supabase.auth.signInWithPassword({ email, password })`. Cadastro de responsável: `supabase.auth.signUp(...)` com confirmação por e-mail.
3. `AuthProvider` escuta `onAuthStateChange`; com sessão ativa, chama `GET /api/me` e guarda `{ usuario, perfil, permissoes }`.
4. `HttpClient` pega o token atual (`supabase.auth.getSession()`) e envia `Authorization: Bearer <token>` em toda requisição. O supabase-js renova o token sozinho.
5. Resposta 401 → `signOut()` e volta ao login. 403 → página "sem acesso".
6. Responsável no primeiro acesso (`/api/me` com `cadastroCompleto: false`) → **Completar cadastro** (`POST /api/portal/cadastro`).
7. Depois do login: `RESPONSAVEL`/`ALUNO` → `/portal`; demais perfis → `/escola`.

> A View **esconde** menus e botões sem permissão para melhorar a experiência, mas quem garante a regra é o Controller/Model.

---

## 4. Rotas

### 4.1 Públicas

| Rota | Página |
|---|---|
| `/login` | Login |
| `/cadastro` | Criar conta de responsável (pela aplicação Java, já confirmada — D10) e entrar |
| `/recuperar-senha` · `/redefinir-senha` | Fluxo de senha do Supabase |

### 4.2 Área da escola (`/escola`, `EscolaLayout`)

| Rota | Página | Perfis | Endpoint principal |
|---|---|---|---|
| `/escola` | Painel: pendências de documentos, pré-matrículas pendentes, turmas lotadas, inadimplência, obrigações a vencer (cada card só com permissão) | Todos da escola | vários |
| `/escola/pendencias` ✅ | Alunos com documentos faltando: filtros, responsável e atalho "Cobrar no WhatsApp" | PENDENCIAS:L (padrão: ADMIN, SECRETARIA) | `GET /pendencias/documentos` |
| `/escola/pre-matriculas` | Fila de pré-matrículas | ADMIN, SECRETARIA | `GET /pre-matriculas?status=PENDENTE` |
| `/escola/pre-matriculas/:id` | Análise: dados, escolha da turma (com vagas), aprovar/rejeitar | ADMIN, SECRETARIA | `POST .../aprovar`, `.../rejeitar` |
| `/escola/matriculas` | Matrículas por ano/turma/status | ADMIN, SECRETARIA, FINANCEIRO | `GET /matriculas` |
| `/escola/matriculas/:id` | Detalhe: aluno, contrato, fardas, documentos, cobranças, bolsas | idem | `GET /matriculas/{id}` |
| `/escola/rematricula` | Rematrícula assistida pela secretaria | ADMIN, SECRETARIA | `POST /matriculas/rematricula` |
| `/escola/alunos` · `/escola/alunos/:id` | Busca e ficha do aluno, responsáveis, documentos (selo de pendentes na lista e alerta no topo da ficha) | ADMIN, SECRETARIA (+ leitura FINANCEIRO, PROFESSOR) | `/alunos` |
| `/escola/responsaveis` | Busca de responsáveis | ADMIN, SECRETARIA | `/responsaveis` |
| `/escola/turmas` · `/escola/turmas/:id` | Turmas com barra de ocupação (x/30), professores, alunos | ADMIN, SECRETARIA | `/turmas` |
| `/escola/financeiro/cobrancas` ✅ | Cobranças com filtros, valor de hoje, detalhe com simulação de data, baixa presencial e **Gerar cobranças** (modal) | ADMIN, FINANCEIRO | `/cobrancas` |
| `/escola/financeiro/inadimplencia` ✅ | Inadimplentes por dias de atraso, com "Cobrar no WhatsApp" | ADMIN, FINANCEIRO | `/financeiro/inadimplencia` |
| `/escola/financeiro/resumo` ✅ | Previsto × recebido × em atraso por mês | ADMIN, FINANCEIRO | `/financeiro/resumo` |
| `/escola/financeiro/bolsas` ✅ | Tipos de bolsa e bolsistas; concessão fica no detalhe da matrícula | ADMIN, FINANCEIRO | `/bolsas` |
| `/escola/rh/funcionarios` | Funcionários e formação continuada | ADMIN | `/funcionarios` |
| `/escola/pedagogico` ✅ | Minhas turmas (professor) ou todas (consulta) | PROFESSOR (+ leitura ADMIN, SECRETARIA) | `/pedagogico/turmas` |
| `/escola/pedagogico/turmas/:id?aba=` ✅ | Abas: diário e chamada (por dia), diários do mês, planos, notas, frequência | PROFESSOR grava; demais consultam | `/turmas/{id}/...` |
| `/escola/pedagogico/calendario` ✅ | Calendário letivo por mês; gestão adiciona e remove dias | Todos com PEDAGOGICO; altera CONFIGURACOES:G | `/calendario` |
| `/escola/rh/funcionarios` → **Formação** ✅ | Cursos de formação continuada do funcionário | ADMIN | `/funcionarios/{id}/formacoes` |
| `/escola/obrigacoes` ✅ | Obrigações com prazo, comprovantes, cumprimento e aviso no painel; Censo mostra as pendências de documentos | ADMIN, SECRETARIA | `/obrigacoes` |
| `/escola/config/anos-letivos` | Anos letivos e encerramento | ADMIN | `/anos-letivos` |
| `/escola/config/precos` | Tabela de preços | ADMIN | `/tabela-precos` |
| `/escola/config/materiais` | Listas de materiais | ADMIN, SECRETARIA | `/listas-materiais` |
| `/escola/config/usuarios` | Usuários e convites | ADMIN | `/usuarios` |
| `/escola/config/materiais` ✅ | Listas de materiais por série e ano, com cópia do ano anterior | ADMIN, SECRETARIA | `/materiais` |
| `/escola/contratos/:id` ✅ | Termo do contrato/ficha, imprimir e registrar assinatura presencial | ADMIN, SECRETARIA | `/contratos/{id}` |
| `/escola/config/perfis` ✅ | Perfis de acesso: matriz módulo × perfil (Sem acesso / Consultar / Alterar) e "Restaurar padrão" | CONFIGURACOES (alterar: ADMIN) | `/permissoes` |

### 4.3 Portal (`/portal`, `PortalLayout`)

| Rota | Página | Endpoint |
|---|---|---|
| `/portal` | Início: cards por filho (série, turma, status, próxima cobrança) | `GET /portal/alunos` |
| `/portal/completar-cadastro` | Dados do responsável (CPF, telefone) no primeiro acesso | `POST /portal/cadastro` |
| `/portal/pre-matricula/nova` | Formulário em etapas: série → dados da criança → fardas → revisão | `POST /pre-matriculas` |
| `/portal/pre-matriculas` | Acompanhar status (pendente / aprovada / rejeitada com motivo) | `GET /portal/pre-matriculas` |
| `/portal/financeiro` ✅ | Cobranças do responsável (pagador): em aberto com valor de hoje e histórico | `GET /portal/cobrancas` |
| `/portal/cobrancas/:id` ✅ | Boleto: composição do valor e, no Cora simulado, "Simular pagamento" | `GET /portal/cobrancas/{id}` |
| `/portal/alunos/:id` ✅ | Página do filho: contratos, boletim e frequência, materiais e atalho para documentos | `.../contratos`, `.../boletim`, `.../materiais` |
| `/portal/contratos/:id` ✅ | Termo e assinatura eletrônica com "Li e concordo" | `POST /portal/contratos/{id}/assinar` |
| `/portal/alunos/:id/documentos` | Documentos | `.../documentos` |
| `/portal/rematricula` | Rematrícula dos filhos | `GET /portal/rematricula/turmas`, `POST /matriculas/rematricula` |

---

## 5. Regras de interface

### 5.1 Dados e valores
- **Nunca calcular dinheiro na View.** Valores (bruto, desconto, final, a pagar hoje) vêm prontos do Controller.
- **CPF**: máscara só na tela (`000.000.000-00`); enviar 11 dígitos. Validar dígito verificador com Zod.
- **Datas**: exibir `dd/MM/aaaa`; enviar ISO (`2027-02-10`). Competência exibida como `fev/2027`.
- **Moeda**: `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`.

### 5.2 Status (cores fixas em `StatusBadge`)

| Status | Cor |
|---|---|
| `PENDENTE`, `AGUARDANDO_PAGAMENTO` | amarelo |
| `APROVADA`, `ATIVA`, `PAGO`, `ASSINADO`, `CUMPRIDA` | verde |
| `REJEITADA`, `VENCIDO`, `ATRASADA`, `CANCELADA`/`CANCELADO` | vermelho |
| `ENCERRADA` | cinza |

Os textos exibidos vêm de `models/enums.ts` (ex.: `AGUARDANDO_PAGAMENTO` → "Aguardando pagamento").

### 5.3 Erros
- `HttpClient` converte a resposta de erro em `ApiError { status, codigo, mensagem, campos }`.
- `campos` → erros nos inputs do formulário (React Hook Form `setError`).
- `codigo` → mensagem amigável (ex.: `TURMA_LOTADA` → "Esta turma está lotada. Escolha outra turma ou ajuste a capacidade."). Código desconhecido → usa `mensagem` do Controller.
- Ações irreversíveis (aprovar, rejeitar, baixa de pagamento, cancelar matrícula, encerrar ano) pedem confirmação.

### 5.4 Experiência
- Estados de **carregando**, **vazio** e **erro** em toda lista.
- Após mutação, invalidar as queries afetadas (ex.: aprovar pré-matrícula invalida `pre-matriculas`, `turmas`, `matriculas`).
- Layout responsivo: o portal é usado principalmente no celular.
- Acessibilidade: rótulos em todos os campos, foco visível, contraste AA.

### 5.5 LGPD no navegador
- Nada de dados pessoais em `console.log`, `localStorage` ou na URL (usar ids, nunca CPF em rota).
- Só a sessão do Supabase fica no navegador (feita pela própria biblioteca).
- Documentos abertos por URL assinada de curta duração.

---

## 6. Build e variáveis de ambiente

- `vite.config.ts` (raiz) usa `root: 'src/main/view'`, `envDir` = raiz do projeto, `build.outDir: 'target/classes/static'` e, em desenvolvimento, *proxy* de `/api` para `http://localhost:8080`.
- As variáveis ficam no **`.env` único da raiz**. O Vite só entrega ao navegador as que começam com `VITE_` — **nunca** crie variável `VITE_` com segredo.

| Variável | Valor | Uso |
|---|---|---|
| `VITE_SUPABASE_URL` | `https://bvbjltcvwrrnsqekpgfq.supabase.co` | Cliente do Auth |
| `VITE_SUPABASE_PUBLISHABLE_KEY` | `sb_publishable_...` | Cliente do Auth |
| `VITE_API_URL` | `/api` | Base dos Controllers (mesma origem; proxy em dev) |

`config/env.ts` valida essas variáveis com Zod na inicialização e mostra erro claro se faltar alguma.
