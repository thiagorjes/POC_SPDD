# TASK-01.5 — Listagem e detalhe de projeto com alcance global

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.4
- **Cenários cobertos:** SCN-002.1, SCN-002.2, SCN-002.3, SCN-002.4, SCN-021.2, SCN-021.3
- **Origem:** RF-002, RF-021, RN-015, RN-035, RN-038, BDR-001

#### Contexto

A visibilidade de projeto é o primeiro ponto em que a autorização por
participação aparece na resposta, e é também onde o alcance de administração
global fica visível. O alcance é **de escopo, não de imunidade**: ele amplia o
que a pessoa enxerga, e não dispensa nenhuma das garantias estruturais.

#### O que deve ser feito

- [ ] Implementar `GET /v1/projetos` devolvendo somente projetos com
      participação da pessoa.
- [ ] Derivar `permissoes` dos papéis **no servidor**.
- [ ] Devolver lista vazia com `200` quando não há participação alguma.
- [ ] Implementar `GET /v1/projetos/{projetoId}` devolvendo `404` quando não há
      participação.
- [ ] Fazer o administrador global receber todos os projetos, cada um com
      `acessoPorAdministracaoGlobal: true`.
- [ ] Propagar a mesma marca no detalhe do projeto.
- [ ] Devolver `fluxoConfigurado` em cada item da lista, `false` enquanto o
      projeto não tiver etapa alguma.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoController.java` | criar | duas rotas de leitura |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoConsulta.java` | criar | projeção para registro de consulta, com join explícito |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoResumo.java` | criar | registro de saída da lista |
| `backend/src/main/java/<pkg>/internal/projeto/ProjetoDetalhe.java` | criar | registro de saída do detalhe |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/projetos` — saída `200`:

```
{
  "conteudo": [ { "id", "nome", "descricao", "papeis": [], "permissoes": [], "fluxoConfigurado": true } ],
  "totalElements": 0,
  "totalPages": 0
}
```

- Ausência de participação devolve `200` com `conteudo: []` — **nunca** `404`, e
  nunca a relação completa do sistema.
- `permissoes` existe para que o cliente não apresente ação que não pode
  executar. A recusa real acontece no serviço.
- Administrador global recebe todos os projetos, cada item acrescido de
  `acessoPorAdministracaoGlobal: true`. O alcance é visível na resposta, não
  implícito.
- `fluxoConfigurado` é `false` enquanto não existir etapa alguma no projeto.
  Derive por existência no banco, dentro da mesma consulta — **não** devolva a
  coleção de etapas para o cliente concluir sozinho, e **não** faça uma segunda
  ida ao banco por item.

`GET /v1/projetos/{projetoId}`:

- `404` quando não há participação. **Não** `403`: revelar a existência do
  projeto já é vazamento.
- Para administrador global, o detalhe vem acompanhado da mesma marca de acesso.

#### Guia técnico — pontos de atenção

- **`404`, não `403`, quando revelar a existência já é vazamento.** Trocar o
  código por `403` reprova cenário congelado.
- **O alcance global não é imunidade.** Ele não contorna a proibição de agregar
  tempo por pessoa — que não tem o que contornar, porque a coluna de pessoa não
  existe no esquema; não contorna a imutabilidade do log, garantida na role de
  banco; e não existe rota de alteração de tempo já contado para ele nem para
  qualquer outro perfil. As três garantias são estruturais de propósito: se
  dependessem de verificação por papel, o administrador global seria justamente
  o papel que as dispensaria. **Não acrescente exceção por papel em lugar
  nenhum.**
- **Consulta única, não navegação de associação.** Montar cada item navegando
  associação produz o problema de consultas em cascata. Declare a leitura como
  projeção para registro de consulta, com join explícito.
- Permissão é sempre resolvida sobre a participação real, nunca sobre papel
  vindo do cliente.
- **A marca de fluxo não configurado é requisito, não enfeite.** Ela existe
  porque o projeto nasce sem fluxo por decisão registrada, e o caminho de
  partida tem dois passos obrigatórios sem que nada no primeiro lembre o
  segundo. Sem essa marca, quem tem de configurar o fluxo não descobre que
  falta — descobre quando a criação de tarefa é recusada.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A lista traz apenas projetos com participação, com papéis e permissões | pessoa participante de dois de três projetos recebe dois itens |
| 2 | Sem participação, a resposta é `200` com lista vazia | requisição de pessoa recém-provisionada |
| 3 | Projeto de terceiro devolve `404` e nenhum dado do projeto | corpo de erro sem nome, descrição ou qualquer atributo do projeto |
| 4 | Administrador global vê projeto em que não participa, com a marca de alcance | campo `acessoPorAdministracaoGlobal` verdadeiro na lista e no detalhe |
| 5 | Nenhuma rota desta task aceita recorte por pessoa | inspeção das assinaturas: não há parâmetro de pessoa |
| 6 | Nenhuma consulta desta task emite mais de uma ida ao banco por requisição | contagem de comandos executados no teste de integração |
| 7 | Projeto sem etapa alguma vem com `fluxoConfigurado` falso, e projeto com etapa vem verdadeiro | lista com os dois projetos numa mesma resposta |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-10 | emenda | SCN-002.4 e `fluxoConfigurado` acrescentados pela emenda do PRD v1.5, que fechou INC-22 instituindo a sinalização do fluxo não configurado como comportamento requerido |
