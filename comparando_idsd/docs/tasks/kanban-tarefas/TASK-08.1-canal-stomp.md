# TASK-08.1 — Canal STOMP com autenticação e autorização de inscrição

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-020.1
- **Origem:** RF-020, RNF-004, ADR-003, ADR-006

#### Contexto

O canal é a única superfície do sistema em que a autorização não é reavaliada a
cada requisição: a inscrição é feita uma vez e a entrega continua enquanto a
sessão viver. Por isso a autorização precisa ser instantânea **e** revogável —
tratá-la como as rotas de leitura deixaria um assinante recebendo o board de um
projeto do qual já foi removido.

#### O que deve ser feito

- [ ] Expor o ponto de entrada STOMP em `/ws`, sobre WebSocket.
- [ ] Exigir credencial verificada no `CONNECT`; sem ela, recusar o handshake.
- [ ] Não expor nenhum canal anônimo, nem para leitura.
- [ ] Autorizar cada `SUBSCRIBE` contra a participação vigente no instante.
- [ ] Endereçar a fila pessoal pelo identificador do principal do servidor,
      nunca por identificador vindo do cliente.
- [ ] Encerrar a sessão quando a credencial expira.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tempo_real/ConfiguracaoWebSocket.java` | criar | registro do ponto de entrada e do broker |
| `backend/src/main/java/<pkg>/internal/tempo_real/InterceptadorDeCanal.java` | criar | autorização de `CONNECT` e `SUBSCRIBE` |
| `backend/src/main/java/<pkg>/internal/tempo_real/Topicos.java` | criar | montagem dos destinos |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Ponto de entrada: `/ws`, protocolo STOMP sobre WebSocket. A credencial vai no
quadro `CONNECT`; handshake sem credencial válida é recusado, e **não existe
canal anônimo**.

Destinos:

- `/topic/board/{projetoId}` — eventos do board de um projeto. Só quem participa
  do projeto pode se inscrever.
- `/user/queue/fila` — fila pessoal de quem está conectado. O destino é resolvido
  pelo identificador do principal **do servidor**; identificador enviado pelo
  cliente é ignorado, sob pena de qualquer um ler a fila alheia.

A autorização de inscrição é **instantânea** e obedece a três regras:

1. A sessão termina quando a credencial expira — não sobrevive ao prazo.
2. Mudança de participação invalida a inscrição na hora, sem esperar
   reconexão. (A revogação em si é a TASK-08.3; aqui fica o ponto de extensão
   que ela usa.)
3. A fila pessoal é endereçada pelo principal do servidor.

Quem não participa do projeto recebe recusa da inscrição, e a recusa não revela
se o projeto existe.

#### Guia técnico — pontos de atenção

- **Autorizar apenas no handshake é o defeito clássico desta task.** A conexão
  vive por horas; a participação muda em segundos.
- **Não aceite identificador de usuário vindo do quadro.** É o caminho direto
  para ler a fila de outra pessoa.
- **Não crie um canal aberto "só para leitura do board"** — o board é o dado que
  a autorização por projeto existe para proteger.
- **A recusa de inscrição não deve distinguir projeto inexistente de projeto sem
  acesso.**
- Não há autenticação local de emergência: sem o provedor de identidade, não há
  sessão. Isso vale também aqui.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Handshake sem credencial válida é recusado | tentativa de conexão sem credencial |
| 2 | Participante do projeto consegue se inscrever no tópico do board | conexão com sujeito participante |
| 3 | Não participante tem a inscrição recusada | conexão com sujeito de fora do projeto |
| 4 | A fila pessoal entregue corresponde ao principal do servidor, não ao que o cliente pediu | tentativa de inscrição na fila com outro identificador |
| 5 | A sessão é encerrada quando a credencial expira | conexão com credencial de validade curta |
| 6 | Não existe destino acessível sem credencial | varredura dos destinos registrados |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
