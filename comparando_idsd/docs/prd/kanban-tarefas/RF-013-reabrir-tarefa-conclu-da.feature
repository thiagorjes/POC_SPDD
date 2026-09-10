# language: pt
Funcionalidade: RF-013 — Reabrir tarefa concluída

  Cenário: SCN-013.1 — Reabertura pelo Product Owner
    Dado que sou o Product Owner do projeto
    E que uma tarefa está concluída
    Quando a reabro informando o motivo
    Então ela volta a aguardar tomada na primeira etapa do fluxo
    E fica sem responsável individual
    E o retorno fica registrado no histórico dela como novo episódio

  Cenário: SCN-013.2 — Reabertura por outro papel
    Dado que participo do projeto e não sou o Product Owner
    E que uma tarefa está concluída
    Quando tento reabri-la
    Então a operação é recusada
    E a tarefa permanece concluída

  Cenário: SCN-013.3 — Tempo por etapa de tarefa reaberta
    Dado que uma tarefa permaneceu quatro horas em Review antes de ser concluída
    E que ela foi reaberta e passou mais duas horas em Review
    Quando consulto o tempo por etapa dessa tarefa
    Então vejo seis horas em Review
    E vejo que esse total corresponde a dois episódios distintos
