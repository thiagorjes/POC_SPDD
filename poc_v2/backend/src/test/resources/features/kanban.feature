# language: pt
Funcionalidade: Kanban de tarefas configuravel
  Um cenario executavel por criterio de aceite do PRD (bloco 19.3). O nome de cada cenario
  carrega o identificador do requisito, de modo que a rastreabilidade RF -> teste seja direta.

  Cenario: RF-001 Board com colunas configuraveis por projeto
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    Quando abro o board do projeto
    Entao o board exibe as colunas na ordem "A Fazer, Fazendo, Concluido"
    E a tarefa aparece na coluna "A Fazer"

  Cenario: RF-002 Workflows com transicoes configuraveis entre etapas
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    Quando movo a tarefa para "Concluido"
    Entao a acao e recusada com a mensagem "Nao existe transicao configurada de \"A Fazer\" para \"Concluido\"."

  Cenario: RF-003 CRUD de tarefas com trava de edicao pos-iniciada
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    E que a tarefa foi movida para "Fazendo"
    E que o toggle "DEV_PODE_EDITAR_TAREFA_INICIADA" esta desabilitado
    E que o usuario "po@kanban.test" possui o papel "product_owner" no projeto
    E que estou autenticado como "po@kanban.test"
    Quando edito a descricao da tarefa
    Entao a acao e recusada com a mensagem "A tarefa ja foi iniciada: descricao e tipo nao podem ser alterados."
    Quando edito o titulo da tarefa para "Titulo revisado"
    Entao a acao e concluida com sucesso
    E o titulo da tarefa e "Titulo revisado"

  Cenario: RF-004 Sinalizacao de impedimento
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    E que a tarefa foi movida para "Fazendo"
    Quando marco a tarefa como impedida
    Entao a tarefa esta impedida
    E existe um periodo de impedimento aberto para a tarefa

  Cenario: RF-005 Notificacao de transicoes aos observadores
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    E que o usuario "po@kanban.test" possui o papel "product_owner" no projeto
    E que estou autenticado como "po@kanban.test"
    Quando movo a tarefa para "Fazendo"
    Entao o usuario "admin@kanban.test" possui uma notificacao nao lida

  Cenario: RF-006 Calculo de lead-time por etapa
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    E que a tarefa foi movida para "Fazendo"
    Quando abro o detalhe da tarefa
    Entao o detalhe exibe o lead-time de cada etapa e o total de impedimento

  Cenario: RF-007 Dashboard de gestao com lead-time medio
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    E que a tarefa foi movida para "Fazendo"
    Quando abro o dashboard do projeto
    Entao o dashboard exibe o lead-time medio de cada etapa

  Cenario: RF-008 CRUD de projetos incluindo finalizar e reabrir
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    Quando finalizo o projeto
    E movo a tarefa para "Fazendo"
    Entao a acao e recusada com a mensagem "O projeto esta finalizado e nao aceita alteracoes."
    Quando reabro o projeto
    E movo a tarefa para "Fazendo"
    Entao a acao e concluida com sucesso

  Cenario: RF-009 CRUD de workflows por projeto
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    Quando excluo o workflow ativo
    Entao a acao e recusada com a mensagem "O workflow possui tarefas ativas e nao pode ser excluido."

  Cenario: RF-010 CRUD de colunas no board
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe a etapa "Revisao" sem transicao de saida
    Quando valido a configuracao do workflow
    Entao a acao e recusada com a mensagem "A etapa \"Revisao\" nao possui nenhuma transicao de saida."

  Cenario: RF-011 CRUD de raias no board
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe a raia "Suporte"
    E que existe uma tarefa na raia "Suporte"
    Quando abro o board do projeto
    Entao o board expoe as raias "Geral, Suporte" e a tarefa esta agrupada na raia "Suporte"

  Cenario: RF-012 Etapa final com opcao de reabertura
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    E que a tarefa foi movida para "Fazendo"
    E que a tarefa foi movida para "Concluido"
    Quando movo a tarefa para "Fazendo"
    Entao a acao e concluida com sucesso
    E a tarefa esta na etapa "Fazendo"

  Cenario: RF-013 Controle de acesso por papeis escopados por projeto
    Dado que existe um projeto ativo com workflow de tres etapas
    E que o usuario "gestor@kanban.test" possui o papel "gestor" no projeto
    E que estou autenticado como "gestor@kanban.test"
    Quando renomeio o projeto para "Projeto Renomeado"
    Entao a acao e recusada com a mensagem "Voce nao possui a permissao necessaria para esta acao neste projeto."

  Cenario: RF-014 Login via SSO
    Dado que existe um projeto ativo com workflow de tres etapas
    Quando encerro a sessao e consulto o usuario autenticado
    Entao a acao e recusada com a mensagem "Requisicao sem usuario autenticado."
    Quando estou autenticado como "novo@kanban.test"
    Entao o usuario autenticado e "novo@kanban.test" e foi provisionado sem senha local

  Cenario: RF-015 Associacao de usuario a projetos
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    Quando associo "dev@kanban.test" ao projeto com o papel "dev"
    E estou autenticado como "dev@kanban.test"
    Entao "dev@kanban.test" visualiza o projeto na sua lista
    Quando movo a tarefa para "Fazendo"
    Entao a acao e concluida com sucesso

  Cenario: RF-016 Configuracao de permissoes por projeto via toggles
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    E que o toggle "DEV_PODE_EXCLUIR_TAREFA" esta desabilitado
    E que o usuario "dev@kanban.test" possui o papel "dev" no projeto
    E que estou autenticado como "dev@kanban.test"
    Quando excluo a tarefa
    Entao a acao e recusada com a mensagem "Esta acao esta desabilitada para o seu papel na configuracao do projeto."

  Cenario: RF-017 Historico de auditoria da tarefa
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    Quando movo a tarefa para "Fazendo"
    Entao o historico registra a alteracao de "ETAPA" de "A Fazer" para "Fazendo" com autor e data

  Cenario: RF-018 Criar card pelo board
    Dado que existe um projeto ativo com workflow de tres etapas
    Quando crio um card com titulo "Card sem atribuicao" sem responsavel e sem raia
    Entao a tarefa nao possui responsavel
    E a tarefa esta na etapa "A Fazer"
    E a tarefa esta na raia padrao do projeto

  Cenario: RF-019 Excluir card pelo board
    Dado que existe um projeto ativo com workflow de tres etapas
    E que existe uma tarefa no board
    Quando excluo a tarefa
    Entao a acao e concluida com sucesso
    E o evento "TAREFA_EXCLUIDA" e publicado
    E a tarefa nao aparece mais no board
