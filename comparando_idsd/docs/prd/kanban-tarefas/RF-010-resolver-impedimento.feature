# language: pt
Funcionalidade: RF-010 — Resolver impedimento

  Cenário: SCN-010.1 — Impedimento resolvido
    Dado que uma tarefa em curso tem impedimento aberto há três horas
    Quando declaro o impedimento resolvido registrando o desfecho
    Então a contagem de impedimento é encerrada com três horas registradas
    E a tarefa deixa de constar com impedimento aberto
    E sua condição de trabalho continua sendo em curso
    E permanece na etapa em que estava

  Cenário: SCN-010.2 — Resolução por quem sinalizou
    Dado que eu mesmo sinalizei o impedimento de uma tarefa
    E que não tenho o papel de desbloqueio no projeto
    Quando declaro o impedimento resolvido registrando o desfecho
    Então a resolução é aceita

  Cenário: SCN-010.3 — Resolução repetida
    Dado que um impedimento já foi resolvido
    Quando peço novamente sua resolução
    Então nada é alterado
    E o tempo de impedimento registrado permanece o mesmo
