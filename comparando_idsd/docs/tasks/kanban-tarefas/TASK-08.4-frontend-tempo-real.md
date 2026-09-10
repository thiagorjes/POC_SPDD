# TASK-08.4 — Frontend: cliente do canal, tolerância a lacuna e resincronização

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** G
- **Depende de:** TASK-07.6, TASK-08.3
- **Cenários cobertos:** SCN-020.1, SCN-020.2, SCN-020.3
- **Origem:** RF-020, RNF-001, telas TL-03 e TL-06, DDR-003, DDR-005

#### Contexto

É a última task da entrega, e a que fecha a promessa: quem está com o board
aberto vê a mudança sem recarregar, e quem espera por uma tarefa é avisado. A
entrega do canal não é garantida por construção, então o cliente é quem
transforma um mecanismo aproximado em uma tela confiável — percebendo a lacuna e
buscando o estado de novo.

#### O que deve ser feito

- [ ] Conectar ao canal com a credencial da sessão, inscrevendo-se no tópico do
      projeto aberto e na fila pessoal.
- [ ] Aplicar o evento recebido ao board sem recarregar a página.
- [ ] Detectar lacuna na sequência e ressincronizar buscando o estado completo.
- [ ] Espalhar a ressincronização no tempo, para que muitas sessões não busquem
      juntas.
- [ ] Ressincronizar **sempre** após reconexão, sem exceção.
- [ ] Anunciar a atualização a tecnologias assistivas sem roubar o foco.
- [ ] Reagir à revogação de inscrição sem deixar a tela mostrando dado obsoleto.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `frontend/lib/tempo-real/cliente.ts` | criar | conexão, inscrição e reconexão |
| `frontend/lib/tempo-real/sequencia.ts` | criar | detecção de lacuna e agendamento do resync |
| `frontend/components/board/Board.tsx` | alterar | aplicação do evento ao estado |
| `frontend/app/minha-fila/page.tsx` | alterar | atualização da fila pessoal |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `docs/design/kanban-tarefas/prototypes/`, e todo arquivo de
verificação já produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Conexão em `/ws`, com a credencial no quadro de conexão. Destinos:
`/topic/board/{projetoId}` enquanto um board está aberto, e `/user/queue/fila`
enquanto houver sessão.

O evento recebido tem cinco campos: projeto, tarefa, tipo, número de sequência e
instante. Ele **diz o que mudou e não traz o conteúdo** — o detalhe vem da
leitura normal (`GET /v1/projetos/{projetoId}/board` para o estado completo).

**Detecção de lacuna.** O cliente guarda a última sequência aplicada por projeto.
Se chega um número maior que o esperado, houve perda: buscar o board completo,
que devolve o `seq` corrente, e adotá-lo como nova base. Número menor ou igual ao
já aplicado é descartado — é reentrega, não novidade.

**Espalhamento.** A busca de ressincronização não é imediata: aguarda uma janela
curta, com atraso aleatório dentro dela, para que cinquenta sessões que perderam
o mesmo evento não peçam o board no mesmo instante.

**Reconexão sempre ressincroniza**, sem tentar deduzir o que se perdeu enquanto a
conexão estava caída. É mais barato buscar o estado do que raciocinar sobre o que
faltou.

**Revogação.** Quando a inscrição é invalidada ou a sessão derrubada pelo
servidor, a tela não fica exibindo o board antigo: ou a reinscrição é aceita e o
estado é rebuscado, ou o usuário é levado de volta à lista de projetos.

**Acessibilidade.** A chegada de uma mudança é anunciada por região de aviso
educada, que **não move o foco** de quem está no meio de uma ação. O cartão que
mudou recebe uma indicação que não depende só de cor.

#### Guia técnico — pontos de atenção

- **Não recarregue a página inteira a cada evento.** Isso derruba arraste em
  curso e formulário aberto.
- **Não confie na chegada de todos os eventos.** O mecanismo é aproximado por
  decisão; a sequência existe para isso.
- **Não ressincronize imediatamente na lacuna** — cinquenta sessões pedindo o
  board junto é a forma de transformar uma perda pequena em pico de carga.
- **Não pule a ressincronização na reconexão**, mesmo que a queda tenha sido de
  segundos.
- **Não roube o foco** ao anunciar mudança: quem está preenchendo motivo de
  impedimento perde o que escreveu.
- **Não indique o cartão alterado só por cor.**
- **Não aplique evento com sequência já vista**, ou a tela pisca sem motivo.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Movimento feito em uma sessão aparece na outra sem recarregar | percurso com duas sessões no mesmo board |
| 2 | A tarefa que passa a aguardar tomada aparece na fila de quem espera | percurso com duas sessões e handoff |
| 3 | Lacuna na sequência dispara busca do estado completo | evento descartado artificialmente |
| 4 | A ressincronização é espalhada no tempo entre sessões | observação de várias sessões após a mesma perda |
| 5 | Reconexão ressincroniza sempre | queda e volta da conexão |
| 6 | Evento repetido não altera a tela | reentrega do mesmo número de sequência |
| 7 | A atualização é anunciada sem mover o foco | percurso com leitor de tela durante preenchimento |
| 8 | O cartão alterado é identificável sem depender de cor | inspeção em modo monocromático |
| 9 | A tela passa em auditoria de acessibilidade AA | verificação automatizada sem violação de nível AA |
| 10 | Após revogação, a tela não segue exibindo o board perdido | remoção de participação com board aberto |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
