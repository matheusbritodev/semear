# Documentação do Sistema Semear

Base de referência para construir o **Sistema Semear — Gestão Escolar** (Centro Educacional Semeando, Maternal ao 5º ano).
A aplicação segue o padrão **MVC**: **Model** e **Controller** em **Java**, **View** em **TypeScript**, banco no **Supabase**.

| # | Arquivo | Função |
|---|---|---|
| 01 | [01-negocio.md](01-negocio.md) | O que o sistema faz: perfis, requisitos (RF), regras (RN/RD), fluxos, glossário, LGPD, inconsistências |
| 02 | [02-arquitetura.md](02-arquitetura.md) | Padrão MVC, stack, estrutura de pastas, fluxo de requisição, decisões |
| 03 | [03-banco-de-dados.md](03-banco-de-dados.md) | Funcionamento do banco no Supabase: tabelas, triggers, funções, views, segurança, lacunas |
| 04 | [04-model-controller.md](04-model-controller.md) | Model e Controller em Java: classes do domínio, POO, padrões, pacotes, endpoints, erros, testes |
| 05 | [05-view.md](05-view.md) | View em TypeScript: rotas por perfil, telas, componentes, comunicação com os Controllers |
| 06 | [06-supabase-conexao.md](06-supabase-conexao.md) | Situação da conexão com o projeto Supabase e o que falta configurar |
| 07 | [07-plano-e-convencoes.md](07-plano-e-convencoes.md) | Ordem de implementação, decisões pendentes, convenções de código |
| 09 | [09-roteiro-demonstracao.md](09-roteiro-demonstracao.md) | Roteiro da apresentação, perfil por perfil |
| 08 | [08-entrega.md](08-entrega.md) | **Entrega:** requisitos atendidos, diagrama de classes, decisões, testes, LGPD e limitações |

## Fontes originais

Ficam em [../infosystem/](../infosystem/) e não devem ser editadas:

- `Requisitos.pdf`: requisitos validados (RF01–RF10, RN01–RN04).
- `MODELO.md`: modelo de domínio orientado a objetos e regras do contrato e do anexo didático (RN05–RN13).
- `semear banco completo.txt`: script SQL completo (aplicado como a primeira migration em [../supabase/migrations/](../supabase/migrations/)).
- `Semear_Documentacao_Banco_de_Dados.pdf`: documentação técnica do banco (v1.0).

## Fonte da verdade

1. **Estrutura de dados e regras já implementadas no SQL** → vale o banco ([03](03-banco-de-dados.md)).
2. **Regras de cálculo que o banco não faz** (desconto de pontualidade, multa, juros) → vale o Model ([04](04-model-controller.md)).
3. **Dúvida de negócio** → registrar em [07 §2](07-plano-e-convencoes.md#2-decisões-pendentes) e decidir com o professor.

## Estrutura do projeto

```
semear/
├── docs/            ← documentação
├── infosystem/      ← fontes originais (somente leitura)
├── supabase/        ← migrations e seed do banco
└── src/
    ├── main/
    │   ├── java/br/undb/semear/   ← Model (model, service, repository) e Controller (controller)
    │   ├── resources/             ← application.yml
    │   └── view/                  ← View (TypeScript)
    └── test/
        ├── java/                  ← testes do Model e dos Controllers
        └── view/                  ← testes da View
```

Detalhes em [02-arquitetura.md §3](02-arquitetura.md#3-estrutura-do-projeto).
