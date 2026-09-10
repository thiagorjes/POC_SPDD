# language: pt
Funcionalidade: RF-012 — Encerrar tarefa sem conclusão

  Cenário: SCN-012.1 — Encerramento sem conclusão
    Dado que tenho permissão de configuração no projeto
    E que uma tarefa está em curso e não será mais realizada
    E que ela não tem impedimento aberto
    Quando a encerro sem conclusão registrando o motivo
    Então sua condição passa a ser encerrada sem conclusão
    E as contagens de permanência na etapa e de espera de tomada cessam
    E o histórico dela é preservado

  Cenário: SCN-012.2 — Encerramento por participante comum
    Dado que participo do projeto sem permissão de configuração
    Quando tento encerrar uma tarefa sem conclusão
    Então a operação é recusada
    E a condição da tarefa não é alterada

  Cenário: SCN-012.3 — Retorno de tarefa encerrada sem conclusão
    Dado que uma tarefa está encerrada sem conclusão
    Quando tento movê-la para qualquer etapa
    Então a operação é recusada
    E sou informado de que o caminho é abrir outra tarefa

  Cenário: SCN-012.4 — Encerramento de tarefa com impedimento aberto
    Dado que tenho permissão de configuração no projeto
    E que uma tarefa tem impedimento aberto sem desfecho registrado
    Quando tento encerrá-la sem conclusão
    Então a operação é recusada
    E sou informado de que o desfecho do impedimento precisa ser registrado antes
    E a condição da tarefa não é alterada
    E o impedimento permanece aberto
