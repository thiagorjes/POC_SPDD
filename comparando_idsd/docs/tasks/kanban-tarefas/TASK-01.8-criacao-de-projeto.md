# TASK-01.8 — Criação de projeto pelo administrador global

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.5
- **Cenários cobertos:** SCN-022.1, SCN-022.2
- **Origem:** RF-022, RN-036, RN-037, RN-038, RN-035, ADR-010, BDR-001

#### Contexto

Sem esta rota o sistema recém-instalado não sai do zero: nenhum contrato criava
projeto, e a primeira participação não podia ser concedida porque não há
ninguém dentro do projeto para concedê-la. É a única rota do sistema cuja
autorização **não** consulta participação — não existe participação a consultar
antes de o projeto existir.

#### O que deve ser feito

- [ ] Implementar `POST /v1/projetos`, exigindo alcance de administração global.
- [ ] Gravar `projeto` (com `seq_atual = 0`) e a primeira `participacao` com
      papel `project_admin` **na mesma transação**.
- [ ] **Não** inserir participação para quem cria.
- [ ] Devolver `201` com `Location` e corpo com `etapas: []`.
- [ ] Recusar com `403` quem não é administrador global, e com `422` nome em
      branco ou `primeiroAdministradorId` sem `usuario` correspondente.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoController.java` | alterar | acrescenta a rota de escrita às duas de leitura |
| `backend/src/main/java/<pkg>/internal/projeto/CriacaoDeProjeto.java` | criar | registro de entrada |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoServico.java` | criar | a transação única de projeto + participação |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/projetos` — entrada:

```
{ "nome": "", "descricao": "", "primeiroAdministradorId": "" }
```

Saída `201`, com `Location: /v1/projetos/{id}`:

```
{ "id": "", "nome": "", "descricao": "", "etapas": [] }
```

- `primeiroAdministradorId` é o `id` de um `usuario` **já existente** — ele
  passa a existir na primeira entrada da pessoa, pelo autoprovisionamento da
  sessão.
- `etapas: []` é a resposta correta, não um estado transitório: o projeto nasce
  sem fluxo.

#### Guia técnico — pontos de atenção

- **Uma transação, não duas.** Se a participação for gravada fora da transação
  do projeto, a falha dela reproduz exatamente o projeto inalcançável que esta
  task existe para eliminar.
- **Quem cria não vira participante.** Inserir a participação do administrador
  global apagaria a marca `acessoPorAdministracaoGlobal` para ele nesse projeto,
  contra SCN-021.2, e confundiria alcance com participação.
- **`403`, não `404`.** Aqui a coleção é conhecida do chamador e não há
  existência a ocultar. O `404` do detalhe de projeto protege outro caso.
- **Sem broadcast.** Não há canal a que a criação pudesse ser publicada: a
  inscrição pressupõe o projeto.
- **O projeto criado ainda não aceita tarefa** (RN-038), e isso é decidido, não
  esquecido. A recusa é verificada em TASK-02.5, por SCN-022.3.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Administrador global cria o projeto e a pessoa nomeada consta como `project_admin` | consulta de participações do projeto criado traz exatamente uma linha |
| 2 | Quem criou não consta como participante | a mesma consulta não traz o criador |
| 3 | Projeto nasce sem etapa | corpo da resposta com `etapas: []` |
| 4 | `project_admin` de outro projeto recebe `403` ao tentar criar | nenhum projeto novo no banco após a tentativa |
| 5 | Falha na gravação da participação não deixa projeto órfão | forçar erro após o insert do projeto e verificar que nada foi persistido |
| 6 | `422` com razão explícita para nome em branco e para pessoa inexistente | corpo em `application/problem+json` com `detail` |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-10 | criação | Task derivada da emenda do PRD v1.3 (RF-022), que fechou a lacuna encontrada pela `/tests` |
