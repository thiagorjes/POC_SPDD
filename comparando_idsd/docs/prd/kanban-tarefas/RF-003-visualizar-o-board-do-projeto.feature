# language: pt
Funcionalidade: RF-003 — Visualizar o board do projeto

  Cenário: SCN-003.1 — Board com o trabalho distribuído
    Dado que sou participante de um projeto com fluxo configurado
    E que há tarefas em etapas diferentes
    Quando abro o board do projeto
    Então vejo cada tarefa na etapa em que está
    E vejo a condição de cada uma
    E vejo quem assumiu cada tarefa que está em curso

  Cenário: SCN-003.2 — Etapa sem tarefa
    Dado que o fluxo do projeto tem uma etapa sem nenhuma tarefa
    Quando abro o board do projeto
    Então a etapa é exibida
    E é indicado que ela não tem tarefa alguma

  Cenário: SCN-003.3 — Contagens que coexistem
    Dado que uma tarefa aguarda tomada há duas horas
    E que ela tem impedimento aberto há uma hora
    Quando abro o board do projeto
    Então vejo a espera de tomada e o tempo de impedimento como grandezas distintas
    E nenhuma delas é apresentada como soma da outra
    E a condição da tarefa continua sendo aguardando tomada, com a marca de impedimento exibida à parte
