# language: pt
Funcionalidade: RF-009 — Sinalizar impedimento

  Cenário: SCN-009.1 — Impedimento aberto
    Dado que uma tarefa está em curso na etapa Desenvolvimento
    Quando declaro que ela está impedida informando o motivo
    Então ela passa a constar com impedimento aberto
    E sua condição de trabalho continua sendo em curso
    E ela permanece na etapa Desenvolvimento
    E a contagem do tempo de impedimento começa
    E ela passa a se destacar para quem responde pelo desbloqueio no projeto

  Cenário: SCN-009.2 — Impedimento sem motivo
    Dado que uma tarefa está em curso
    Quando tento declará-la impedida sem informar o motivo
    Então a sinalização é recusada
    E nenhum impedimento é aberto na tarefa

  Cenário: SCN-009.3 — Sinalização sobre tarefa já impedida
    Dado que uma tarefa já tem impedimento aberto há duas horas
    Quando sinalizo um novo impedimento nela informando outro motivo
    Então nenhum segundo impedimento é criado
    E a informação é anexada ao impedimento existente
    E a contagem em curso não é reiniciada
