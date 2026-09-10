# language: pt
Funcionalidade: RF-005 — Avançar a tarefa para a etapa seguinte

  Cenário: SCN-005.1 — Avanço para a etapa seguinte
    Dado que uma tarefa está em curso comigo na etapa Desenvolvimento
    E que a etapa seguinte do fluxo é Review
    Quando indico que ela avança para Review
    Então ela passa a estar na etapa Review
    E sua condição passa a ser aguardando tomada
    E ela deixa de ter responsável individual
    E a contagem de espera de tomada em Review começa

  Cenário: SCN-005.2 — Etapa de destino não alcançável
    Dado que uma tarefa está na etapa Desenvolvimento
    E que Homologação não é adjacente a Desenvolvimento nem é a primeira etapa do fluxo
    Quando tento mover a tarefa para Homologação
    Então a transição é recusada
    E a tarefa permanece em Desenvolvimento
    E a razão da recusa me é informada

  Cenário: SCN-005.3 — Origem declarada já não é a atual
    Dado que li a tarefa quando ela estava na etapa Desenvolvimento
    E que outra pessoa já a moveu para Review
    Quando peço para avançá-la a partir de Desenvolvimento
    Então a transição é recusada
    E me é informado que ela está agora em Review
    E nenhuma contagem é alterada
