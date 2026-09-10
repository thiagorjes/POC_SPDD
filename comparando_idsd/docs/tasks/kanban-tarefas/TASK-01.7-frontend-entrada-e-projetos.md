# TASK-01.7 — Frontend: entrada autenticada, lista de projetos e novo projeto

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-01.5, TASK-01.6, TASK-01.8
- **Cenários cobertos:** SCN-001.1, SCN-001.2
- **Origem:** RF-001, RF-002, RF-022, RN-036, RN-037, RN-038, DDR-004, DDR-005, telas TL-01, TL-02 e TL-11

#### Contexto

As duas primeiras telas do produto e o esqueleto do frontend: App Router,
cliente REST tipado, autenticação por authorization code com PKCE em client
público. Os dois cenários desta task são de ponta a ponta, então é aqui que a
fatia vertical do épico fecha.

#### O que deve ser feito

- [ ] Criar o projeto Next.js com App Router e a árvore `app/`, `components/`,
      `lib/api/`.
- [ ] Adotar o design system da coleção `frontend/nextjs` como fonte de verdade
      visual — tokens de cor, tipografia e espaçamento vêm dela.
- [ ] Implementar a entrada autenticada em `/entrar`, com authorization code e
      PKCE, client público, sem segredo no cliente.
- [ ] Implementar o cliente REST tipado com anexo do token e tratamento de
      `problem+json`.
- [ ] Implementar `/projetos` consumindo `GET /v1/projetos`, exibindo nome,
      descrição e as permissões de cada projeto.
- [ ] Exibir o bloco "nenhum projeto" quando a lista vem vazia — e não uma tela
      de erro.
- [ ] Exibir a marca de acesso por administração global no item cujo
      `acessoPorAdministracaoGlobal` é verdadeiro.
- [ ] Tratar a falha de autenticação e a indisponibilidade do provedor sem
      oferecer caminho alternativo de entrada.
- [ ] Implementar o painel de novo projeto (TL-11) sobre `/projetos`, chamando
      `POST /v1/projetos` com nome, descrição e a conta que será a primeira
      `project_admin` — os três num envio só.
- [ ] Exibir a ação **Novo projeto** apenas para quem tem administração global,
      e o estado vazio "ainda não existe projeto neste sistema" com a saída para
      o painel.
- [ ] No sucesso, nomear o segundo passo obrigatório e oferecer a ida à
      configuração do fluxo; marcar no cartão do projeto que ele não aceita
      tarefa enquanto não houver etapa.
- [ ] Garantir conformidade WCAG 2.1 AA nas três telas.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/package.json` | criar | Next.js App Router, React Query, cliente de mensageria para uso posterior |
| `frontend/app/entrar/page.tsx` | criar | tela TL-01 |
| `frontend/app/projetos/page.tsx` | criar | tela TL-02 |
| `frontend/lib/api/cliente.ts` | criar | cliente REST tipado, anexo do token, erro em problem+json |
| `frontend/lib/api/sessao.ts` | criar | chamada de `GET /v1/sessao` |
| `frontend/lib/api/projetos.ts` | criar | chamadas de `GET /v1/projetos` e `POST /v1/projetos` |
| `frontend/app/projetos/novo/page.tsx` | criar | tela TL-11, painel sobre `/projetos` |
| `frontend/components/` | criar | componentes compartilhados das telas |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/` (protótipo é insumo,
não destino), e todo arquivo de verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Protótipos de referência, a serem materializados em componentes reais:
`docs/design/kanban-tarefas/prototypes/TL-01-entrada-autenticada.html` e
`TL-02-meus-projetos.html` e `TL-11-novo-projeto.html`. Eles trazem os estados
que as telas precisam ter: carregando, vazio, erro de autenticação, validação e
sucesso.

Rotas do produto: `/entrar` (TL-01), `/projetos` (TL-02) e o painel de novo
projeto sobre `/projetos` (TL-11).

Corpo enviado na criação:

```
{ "nome", "descricao", "primeiroAdministradorId" }
```

Respostas: `201` com o projeto criado, `403` para quem não tem administração
global, `422` para nome ausente ou conta inexistente.

Forma da resposta consumida:

```
{ "conteudo": [ { "id", "nome", "descricao", "papeis": [], "permissoes": [] } ],
  "totalElements": 0, "totalPages": 0 }
```

`permissoes` serve **apenas** para não apresentar ação que a pessoa não pode
executar. A recusa real acontece no serviço.

#### Guia técnico — pontos de atenção

- **Lista vazia é sucesso, não erro.** Ela tem bloco próprio na tela; tratar
  `conteudo: []` como falha reprova cenário congelado.
- **Não há segredo de cliente no frontend.** Client público com PKCE.
- **Sem fallback de autenticação.** Provedor fora do ar mostra a recusa e o
  convite a tentar de novo, e não uma segunda forma de entrar.
- **Acessibilidade AA é obrigatória**, não recomendação: contraste, foco
  visível, navegação por teclado e rótulo acessível em todo controle. Nenhuma
  informação pode ser transmitida **só** por cor.
- Esconder ação na interface não é autorização; a permissão real é a do
  serviço.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Pessoa autenticada com sucesso chega à lista de projetos | percurso de ponta a ponta em navegador |
| 2 | Pessoa sem participação vê a lista vazia com o bloco próprio | percurso de ponta a ponta com usuário recém-provisionado |
| 3 | Falha de autenticação não oferece caminho alternativo | inspeção da tela de recusa |
| 4 | O item acessado por administração global é identificado como tal | marca visível no item, não só no console |
| 5 | As duas telas passam em auditoria de acessibilidade AA | verificação automatizada de acessibilidade sem violação de nível AA |
| 6 | Nenhuma informação é transmitida apenas por cor | inspeção dos estados em modo monocromático |
| 7 | A ação de criar projeto não aparece para quem não tem administração global, e a recusa continua sendo do serviço | inspeção da lista com conta comum, mais requisição direta ao serviço |
| 8 | O sucesso da criação nomeia a configuração do fluxo como segundo passo obrigatório e oferece a ida a ela | inspeção do estado de sucesso |
| 9 | O projeto sem fluxo é marcado como tal na lista, a partir de `fluxoConfigurado` da resposta e nunca de inferência do cliente | inspeção do cartão do projeto recém-criado, mais um projeto já configurado sem marca na mesma lista |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-10 | revisão | TL-11 incorporada ao escopo desta task ao fechar INC-18. Não virou task própria porque o épico já está no limite de 8 e porque a tela é painel sobre `/projetos`, que é justamente o que esta task entrega. Passa a depender de TASK-01.8, que expõe a rota |
