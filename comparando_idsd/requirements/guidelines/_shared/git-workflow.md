# Git Workflow (transversal)

## Conventional Commits

Formato: `tipo(escopo opcional): descrição no imperativo`

```
feat(extrato): adicionar filtro por tipo de lançamento
fix(remessa): corrigir cálculo de data de crédito em feriados
refactor(extrato): extrair validação de conta para método privado
test(pessoa): adicionar testes de integração do FavorecidoController
chore: atualizar springdoc para 2.5.0
docs(api): documentar depreciação do endpoint /v1/legado
perf(consulta): usar projeção em vez de entidade completa
```

Tipos: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`, `perf`, `build`, `ci`.

- Breaking change: sufixo `!` (`feat(api)!: remover campo obsoleto`) + rodapé `BREAKING CHANGE:`.
- Descrição em minúsculas, sem ponto final, ≤ 72 caracteres.
- Corpo (opcional) explica o **porquê**, não o **o quê**.

## Branches

- `main` sempre deployável.
- Trabalho em branch curto: `feat/<slug>`, `fix/<slug>`, `chore/<slug>`.
- Rebase para manter histórico linear; sem merge commit de branch pessoal.

## Pull Request

- PR pequeno, com uma responsabilidade.
- Descrição liga à task/issue e resume a mudança.
- CI verde obrigatório: build, testes, SCA e análise estática (ver `vulnerable-and-proprietary-libs.md`).
- Sem `--no-verify`, sem pular hooks, sem commitar segredo.

## Versão e tag

- Release marcada com tag SemVer `vMAJOR.MINOR.PATCH` (ver [`api-standards.md`](api-standards.md) §1).
- Changelog gerado a partir dos Conventional Commits.
