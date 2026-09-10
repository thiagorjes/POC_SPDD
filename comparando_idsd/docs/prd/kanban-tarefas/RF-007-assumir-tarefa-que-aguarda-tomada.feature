# language: pt
Funcionalidade: RF-007 — Assumir tarefa que aguarda tomada

  Cenário: SCN-007.1 — Tomada da tarefa
    Dado que uma tarefa aguarda tomada na etapa Review há duas horas
    E que participo do projeto com permissão de escrita
    Quando assumo a tarefa
    Então passo a constar como responsável por ela
    E sua condição passa a ser em curso
    E a espera de tomada é encerrada com duas horas registradas

  Cenário: SCN-007.2 — Tomada repetida por quem já assumiu
    Dado que já assumi uma tarefa
    Quando peço para assumi-la novamente
    Então continuo constando como responsável
    E o momento em que assumi não é alterado

  Cenário: SCN-007.3 — Duas pessoas assumem ao mesmo tempo
    Dado que uma tarefa aguarda tomada na etapa Review
    E que outra pessoa a assumiu um instante antes de mim
    Quando tento assumi-la
    Então minha ação é recusada
    E me é informado quem a assumiu
    E o responsável registrado não é alterado

  Cenário: SCN-007.4 — Tomada de tarefa com impedimento aberto
    Dado que uma tarefa aguarda tomada na etapa Review há duas horas
    E que ela tem impedimento aberto há uma hora
    Quando assumo a tarefa
    Então passo a constar como responsável por ela
    E sua condição de trabalho passa a ser em curso
    E a espera de tomada é encerrada com duas horas registradas
    E o impedimento continua aberto, contando desde o início original
