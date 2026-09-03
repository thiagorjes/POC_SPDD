# language: pt
Funcionalidade: Criterios de aceite do Kanban de Tarefas Configuravel
  Um cenario executavel por criterio de aceite de RF-001 a RF-019, nomeado com o ID do RF.

  Contexto:
    Dado um projeto ativo com o workflow padrao de tres etapas
    E o usuario "dev" com o papel "dev" no projeto
    E o usuario "chefe" com o papel "project_admin" no projeto

  Cenario: RF-001 - Board exibe colunas na ordem configurada com tarefas na etapa correspondente
    Dado que "dev" criou a tarefa "Ajustar login"
    Quando "dev" envia GET para "/api/projetos/{projeto}/board"
    Entao o status da resposta e 200
    E a lista "etapas" da resposta tem 3 itens
    E o campo "etapas.0.nome" da resposta e "A fazer"
    E o campo "etapas.2.nome" da resposta e "Concluido"
    E o campo "tarefas.0.etapaId" da resposta e "{etapa:A fazer}"

  Cenario: RF-002 - Movimentacao sem transicao configurada e bloqueada e informada
    Dado o toggle "DEV_PODE_FINALIZAR_TAREFA" habilitado no projeto
    E que "dev" criou a tarefa "Ajustar login"
    Quando "dev" move a tarefa para "Concluido"
    Entao o status da resposta e 422
    E o errorCode e "TRANSICAO_NAO_PERMITIDA"
    E a mensagem de erro e "Nao existe transicao configurada de \"A fazer\" para \"Concluido\"."
    E a mensagem de erro nao expoe detalhe de infraestrutura

  Cenario: RF-003 - Campos estruturais ficam travados apos a tarefa ser iniciada
    Dado que "dev" criou a tarefa "Ajustar login"
    E que "dev" moveu a tarefa para "Fazendo"
    Quando "dev" envia PUT para "/api/tarefas/{tarefa}" com o corpo:
      """
      {"titulo": "Ajustar login", "descricao": "Nova descricao", "tipo": "BUG",
       "prioridade": "MEDIA", "versaoEsperada": {versao}}
      """
    Entao o status da resposta e 422
    E o errorCode e "TAREFA_INICIADA_CAMPO_BLOQUEADO"
    E a mensagem de erro e "A tarefa ja foi iniciada: descricao e tipo nao podem mais ser alterados."

  Cenario: RF-004 - Marcar impedimento registra o motivo e sinaliza a tarefa
    Dado que "dev" criou a tarefa "Ajustar login"
    Quando "dev" envia POST para "/api/tarefas/{tarefa}/impedimento" com o corpo:
      """
      {"motivo": "Aguardando ambiente"}
      """
    Entao o status da resposta e 200
    E o campo "impedida" da resposta e verdadeiro
    E o campo "motivoImpedimento" da resposta e "Aguardando ambiente"

  Cenario: RF-005 - Observadores recebem notificacao interna na mudanca de etapa
    Dado o usuario "outro" com o papel "dev" no projeto
    E que "dev" criou a tarefa "Ajustar login"
    Quando "outro" move a tarefa para "Fazendo"
    Entao o status da resposta e 200
    Quando "dev" envia GET para "/api/notificacoes"
    Entao o status da resposta e 200
    E a resposta e uma lista nao vazia
    E o campo "0.tipo" da resposta e "ETAPA_ALTERADA"
    E o campo "0.tarefaId" da resposta e "{tarefa}"

  Cenario: RF-006 - Detalhe da tarefa expoe tempo por etapa e total de impedimento
    Dado que "dev" criou a tarefa "Ajustar login"
    E que "dev" moveu a tarefa para "Fazendo"
    Quando "dev" envia GET para "/api/tarefas/{tarefa}"
    Entao o status da resposta e 200
    E a lista "leadTimePorEtapa" da resposta nao esta vazia
    E o campo "etapaId" da resposta e "{etapa:Fazendo}"

  Cenario: RF-007 - Dashboard expoe lead-time medio por etapa para o gestor
    Dado o usuario "gestor" com o papel "gestor" no projeto
    E que "dev" criou a tarefa "Ajustar login"
    E que "dev" moveu a tarefa para "Fazendo"
    Quando "gestor" envia GET para "/api/projetos/{projeto}/dashboard"
    Entao o status da resposta e 200
    E o campo "projetoId" da resposta e "{projeto}"
    E a lista "etapas" da resposta tem 3 itens

  Cenario: RF-008 - Projeto finalizado fica somente leitura para todos, inclusive o administrador
    Dado que "dev" criou a tarefa "Ajustar login"
    Quando "chefe" envia POST para "/api/projetos/{projeto}/finalizar"
    Entao o status da resposta e 200
    Quando "dev" envia POST para "/api/projetos/{projeto}/tarefas" com o corpo:
      """
      {"titulo": "Outra tarefa", "tipo": "FEATURE"}
      """
    Entao o status da resposta e 409
    E o errorCode e "PROJETO_FINALIZADO"
    Quando "chefe" envia POST para "/api/projetos/{projeto}/tarefas" com o corpo:
      """
      {"titulo": "Tarefa do admin", "tipo": "FEATURE"}
      """
    Entao o status da resposta e 409
    E o errorCode e "PROJETO_FINALIZADO"

  Cenario: RF-009 - Excluir workflow com tarefa ativa e bloqueado
    Dado que "dev" criou a tarefa "Ajustar login"
    Quando "chefe" envia DELETE para "/api/projetos/{projeto}/workflows/{workflow}"
    Entao o status da resposta e 409
    E o errorCode e "RECURSO_POSSUI_TAREFAS_ATIVAS"
    E a mensagem de erro nao expoe detalhe de infraestrutura

  Cenario: RF-010 - Etapa nao final exige ao menos uma transicao de saida, mas a final e isenta
    Quando "chefe" envia DELETE para "/api/workflows/{workflow}/transicoes/{transicao:Fazendo>Concluido}"
    Entao o status da resposta e 422
    E o errorCode e "WORKFLOW_INVALIDO"
    E a mensagem de erro e "A etapa \"Fazendo\" nao possui nenhuma transicao de saida configurada."
    Quando "chefe" envia GET para "/api/workflows/{workflow}/transicoes"
    Entao o status da resposta e 200
    E a resposta e uma lista com 2 itens

  Cenario: RF-011 - Tarefas sao agrupadas pelas raias configuradas no board
    Dado que "dev" criou a tarefa "Ajustar login"
    Quando "chefe" envia POST para "/api/projetos/{projeto}/raias" com o corpo:
      """
      {"nome": "Time B"}
      """
    Entao o status da resposta e 201
    Quando "dev" envia GET para "/api/projetos/{projeto}/board"
    Entao o status da resposta e 200
    E a lista "raias" da resposta tem 2 itens
    E o campo "tarefas.0.raiaId" da resposta e "{raia}"

  Cenario: RF-012 - Desfinalizar retorna a tarefa apenas para uma etapa que leva a final
    Dado o toggle "DEV_PODE_FINALIZAR_TAREFA" habilitado no projeto
    E que "dev" criou a tarefa "Ajustar login"
    E que "dev" moveu a tarefa para "Fazendo"
    E que "dev" moveu a tarefa para "Concluido"
    Quando "dev" move a tarefa para "A fazer"
    Entao o status da resposta e 422
    E o errorCode e "TRANSICAO_NAO_PERMITIDA"
    Quando "dev" move a tarefa para "Fazendo"
    Entao o status da resposta e 200
    E o campo "etapaId" da resposta e "{etapa:Fazendo}"

  Cenario: RF-013 - Permissao e validada no backend independentemente do estado da interface
    Dado o usuario "visitante" sem papel no projeto
    Quando "visitante" envia POST para "/api/projetos/{projeto}/tarefas" com o corpo:
      """
      {"titulo": "Tarefa nao autorizada", "tipo": "FEATURE"}
      """
    Entao o status da resposta e 403
    E o errorCode e "PERMISSAO_NEGADA"
    E a mensagem de erro nao expoe detalhe de infraestrutura

  Cenario: RF-014 - Sem token valido nao ha acesso: nao existe autenticacao local de fallback
    Quando uma requisicao anonima e enviada para "/api/usuarios/me"
    Entao o status da resposta e 401

  Cenario: RF-015 - Associar usuario ao projeto com papel concede o acesso correspondente
    Dado o usuario "novato" sem papel no projeto
    Quando "novato" envia GET para "/api/projetos/{projeto}/board"
    Entao o status da resposta e 403
    Quando "chefe" envia POST para "/api/projetos/{projeto}/usuarios" com o corpo:
      """
      {"usuarioId": "{usuario:novato}", "codigoPapel": "dev"}
      """
    Entao o status da resposta e 204
    Quando "novato" envia GET para "/api/projetos/{projeto}/board"
    Entao o status da resposta e 200

  Cenario: RF-016 - Toggle desabilitado bloqueia a acao mesmo com papel permissivo
    Dado o toggle "DEV_PODE_EXCLUIR_TAREFA" desabilitado no projeto
    E que "dev" criou a tarefa "Ajustar login"
    Quando "dev" envia DELETE para "/api/tarefas/{tarefa}"
    Entao o status da resposta e 403
    E o errorCode e "PERMISSAO_NEGADA"

  Cenario: RF-017 - Historico registra autor, valor anterior, valor novo e momento
    Dado que "dev" criou a tarefa "Ajustar login"
    E que "dev" moveu a tarefa para "Fazendo"
    Quando "dev" envia GET para "/api/tarefas/{tarefa}/historico"
    Entao o status da resposta e 200
    E a resposta e uma lista nao vazia
    E o campo "0.autorId" da resposta e "{usuario:dev}"
    E o campo "0.campo" da resposta e "ETAPA"
    E o campo "0.valorAnterior" da resposta e "A fazer"
    E o campo "0.valorNovo" da resposta e "Fazendo"

  Cenario: RF-018 - Card criado pelo board nasce sem responsavel, na etapa de menor ordem e na raia padrao
    Quando "dev" envia POST para "/api/projetos/{projeto}/tarefas" com o corpo:
      """
      {"titulo": "Ajustar login", "tipo": "FEATURE"}
      """
    Entao o status da resposta e 201
    E o campo "etapaId" da resposta e "{etapa:A fazer}"
    E o campo "raiaId" da resposta e "{raia}"
    E o campo "responsavelId" da resposta e nulo
    E o campo "iniciada" da resposta e falso

  Cenario: RF-019 - Excluir card emite TAREFA_EXCLUIDA e reflete no board em ate 2 segundos
    Dado o toggle "DEV_PODE_EXCLUIR_TAREFA" habilitado no projeto
    E que "dev" criou a tarefa "Ajustar login"
    E que o board esta escutando eventos
    Quando "dev" envia DELETE para "/api/tarefas/{tarefa}"
    Entao o status da resposta e 204
    E um evento "TAREFA_EXCLUIDA" chega ao board em ate 2 segundos
