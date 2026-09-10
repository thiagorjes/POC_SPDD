# language: pt
Funcionalidade: RF-006 — Retroceder a tarefa e devolvê-la ao início do fluxo

  Cenário: SCN-006.1 — Retrocesso de uma etapa
    Dado que uma tarefa está em curso na etapa Review
    E que a etapa anterior do fluxo é Desenvolvimento
    Quando indico que ela retorna para Desenvolvimento
    Então ela passa a estar em Desenvolvimento
    E sua condição passa a ser aguardando tomada

  Cenário: SCN-006.2 — Retorno ao início do fluxo
    Dado que uma tarefa está na etapa Homologação
    E que a primeira etapa do fluxo é Backlog
    Quando indico que ela retorna para Backlog
    Então ela passa a estar em Backlog
    E a transição é aceita ainda que Backlog não seja adjacente a Homologação

  Cenário: SCN-006.3 — Tarefa com impedimento aberto que retrocede
    Dado que uma tarefa está na etapa Review com impedimento aberto há três horas
    Quando ela é movida para Desenvolvimento
    Então ela passa a estar em Desenvolvimento
    E sua condição de trabalho passa a ser aguardando tomada
    E o impedimento continua aberto
    E o tempo de impedimento continua contando desde o início original
