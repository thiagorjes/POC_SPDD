# TASK-08.2 — Publicação após o commit, escuta e varredura de retomada

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-08.1
- **Cenários cobertos:** SCN-020.1, SCN-020.2
- **Origem:** RF-020, RNF-001, RNF-002, ADR-004, SDR-004

#### Contexto

É aqui que o registro vira aviso. O mecanismo é o mais barato possível — o
próprio banco distribui a notificação entre as instâncias, sem intermediário —, e
o preço dessa escolha é que a entrega não é garantida. A rede de segurança é a
sequência: o cliente percebe a lacuna e ressincroniza. Duplicar número de
sequência destrói essa rede, e é por isso que ele nasce no banco.

#### O que deve ser feito

- [ ] Obter o número de sequência **dentro da transação de escrita**, do banco.
- [ ] Publicar a notificação somente **depois do commit**, nunca durante.
- [ ] Usar um canal único para todos os projetos, com distribuição feita por
      quem escuta.
- [ ] Manter o corpo da notificação dentro do limite de tamanho, com envelope
      mínimo.
- [ ] Escutar o canal em cada instância e redistribuir aos assinantes locais,
      inclusive às filas pessoais.
- [ ] Ao assumir ou reassumir a escuta, varrer o log por sequências acima da
      última publicada e emitir o que ficou para trás.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tempo_real/PublicadorDeEventos.java` | criar | publicação após o commit |
| `backend/src/main/java/<pkg>/internal/tempo_real/OuvinteDeEventos.java` | criar | escuta do canal e redistribuição |
| `backend/src/main/java/<pkg>/internal/tempo_real/VarreduraDeRetomada.java` | criar | recuperação por sequência |
| `backend/src/main/java/<pkg>/internal/escrita/NucleoDeEscrita.java` | alterar | obtenção do número de sequência |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

**Sequência.** Dentro da mesma transação da escrita:
`UPDATE projeto SET seq_atual = seq_atual + 1 WHERE id = ? RETURNING seq_atual`.
O número é por projeto, monotônico e sem repetição. Atribuí-lo na aplicação
produziria duplicata em execução com várias instâncias, e duplicata torna a
lacuna indetectável — que é justamente o sinal do qual o cliente depende.

**Publicação.** A notificação sai **depois do commit** (gancho de after-commit),
nunca dentro dele: publicar antes entrega um evento que a transação ainda pode
desfazer. A contrapartida — a janela entre o commit e a publicação, em que o
processo pode cair — é fechada pela varredura de retomada, e não por tentar
publicar dentro da transação.

**Canal.** Um só, `board_events`, para todos os projetos. Um canal por projeto
exigiria reinscrição a cada projeto novo e um mapa de canais vivos por
instância. Quem escuta descarta o que não interessa aos seus assinantes.

**Envelope.** `{ projetoId, tarefaId, tipo, seq, ocorridoEm }`. Cinco campos
porque o corpo da notificação tem limite de 8 KB — o evento diz **o que mudou**,
e quem recebe busca o detalhe pela leitura normal. Nunca inclua o cartão inteiro.

**Redistribuição.** Cada instância entrega aos assinantes locais do tópico do
projeto e às filas pessoais que o evento afeta. Nenhuma instância fala com outra
diretamente; o banco é o meio.

**Varredura de retomada.** Ao assumir a escuta — na subida e após reconexão —
o servidor lê o log em busca de sequências acima da última que publicou naquele
projeto e emite o que faltou. Sem isso, a queda de uma instância deixaria as
sessões das outras em silêncio até a próxima escrita.

**Envelope de desempenho:** com 100 tarefas no board e 50 sessões conectadas, o
percentil 95 do intervalo entre a escrita aceita e a chegada do evento fica em
até 2 segundos.

#### Guia técnico — pontos de atenção

- **Não gere o número de sequência na aplicação.** É a decisão que a arquitetura
  inverteu de propósito; reverter parece inofensivo e cega a detecção de lacuna.
- **Não publique dentro da transação.** Evento de escrita desfeita é pior que
  evento atrasado.
- **Não engorde o envelope.** Passar do limite faz a notificação falhar em tempo
  de execução, e o teste do caminho feliz não pega.
- **Não abra um canal por projeto.**
- **Não trate a varredura como opcional.** Ela é a única coisa entre uma queda de
  instância e um board mudo.
- Não há intermediário de mensagens neste sistema, e não é aqui que se introduz
  um.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Duas escritas concorrentes em instâncias diferentes recebem sequências distintas e crescentes | teste com três instâncias simultâneas |
| 2 | Escrita revertida não produz evento | transação forçada a falhar após a gravação |
| 3 | O evento chega às sessões conectadas em outra instância | teste com duas instâncias e um assinante em cada |
| 4 | O envelope tem os cinco campos e não carrega o cartão | inspeção do corpo publicado |
| 5 | Após queda e retomada da escuta, os eventos perdidos são emitidos | derrubada do ouvinte com escritas no intervalo |
| 6 | O percentil 95 fica em até 2 segundos com 100 tarefas e 50 sessões | medição sob a carga declarada |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
