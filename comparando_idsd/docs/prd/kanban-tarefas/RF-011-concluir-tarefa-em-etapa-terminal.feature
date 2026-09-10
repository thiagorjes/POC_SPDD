# language: pt
Funcionalidade: RF-011 — Concluir tarefa em etapa terminal

  Cenário: SCN-011.1 — Conclusão em etapa terminal
    Dado que uma tarefa está em curso na última etapa antes da terminal
    E que a etapa Concluído é terminal no fluxo do projeto
    Quando a movo para Concluído
    Então sua condição passa a ser concluída
    E todas as contagens de tempo cessam
    E ela deixa de figurar como trabalho em curso

  Cenário: SCN-011.2 — Conclusão sem passar por etapa terminal
    Dado que uma tarefa está na etapa Review, que não é terminal
    Quando tento marcá-la como concluída
    Então a operação é recusada
    E sou informado de que a conclusão se dá ao alcançar uma etapa terminal

  Cenário: SCN-011.3 — Conclusão com impedimento aberto
    Dado que uma tarefa tem impedimento aberto
    Quando tento movê-la para a etapa terminal Concluído
    Então a operação é recusada
    E sou informado de que o impedimento precisa ter desfecho registrado antes
    E o impedimento permanece aberto
