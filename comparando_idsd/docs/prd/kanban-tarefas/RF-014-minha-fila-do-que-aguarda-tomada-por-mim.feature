# language: pt
Funcionalidade: RF-014 — Minha fila do que aguarda tomada por mim

  Cenário: SCN-014.1 — Fila atravessando projetos
    Dado que participo de dois projetos
    E que há uma tarefa aguardando tomada há cinco horas em um deles
    E que há outra aguardando tomada há uma hora no outro
    Quando abro minha fila
    Então vejo as duas tarefas
    E a que aguarda há cinco horas aparece antes da que aguarda há uma hora

  Cenário: SCN-014.2 — Inverter a ordenação da fila
    Dado que minha fila está ordenada pela maior espera primeiro
    Quando inverto a ordenação
    Então a tarefa que aguarda há menos tempo passa a aparecer primeiro

  Cenário: SCN-014.3 — Fila vazia
    Dado que nenhuma tarefa aguarda tomada nos projetos em que participo
    Quando abro minha fila
    Então vejo que nada aguarda tomada por mim
    E nenhum tempo de espera me é apresentado
