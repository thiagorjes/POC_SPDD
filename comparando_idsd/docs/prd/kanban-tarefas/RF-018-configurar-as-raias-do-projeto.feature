# language: pt
Funcionalidade: RF-018 — Configurar as raias do projeto

  Cenário: SCN-018.1 — Raias no board
    Dado que tenho permissão de configuração no projeto
    Quando defino duas raias para o projeto
    Então o board passa a agrupar as tarefas por essas raias dentro de cada etapa

  Cenário: SCN-018.2 — Transição entre tarefas de raias diferentes
    Dado que uma tarefa pertence à raia Sustentação
    E que a etapa de destino contém apenas tarefas da raia Projeto
    Quando movo a tarefa para a etapa seguinte
    Então a transição é aceita
    E a tarefa mantém a raia Sustentação

  Cenário: SCN-018.3 — Agregação por raia
    Dado que o projeto tem raias definidas e histórico acumulado
    Quando consulto o tempo por etapa
    Então nenhum recorte por raia me é oferecido
