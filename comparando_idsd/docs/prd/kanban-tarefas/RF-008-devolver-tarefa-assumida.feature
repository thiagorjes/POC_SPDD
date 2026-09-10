# language: pt
Funcionalidade: RF-008 — Devolver tarefa assumida

  Cenário: SCN-008.1 — Devolução ao pool da etapa
    Dado que assumi uma tarefa na etapa Review
    Quando a devolvo
    Então ela volta a aguardar tomada na etapa Review
    E deixa de ter responsável individual
    E uma nova contagem de espera de tomada começa

  Cenário: SCN-008.2 — Devolução por quem não assumiu
    Dado que uma tarefa foi assumida por outra pessoa
    Quando tento devolvê-la
    Então a devolução é recusada
    E o responsável registrado não é alterado

  Cenário: SCN-008.3 — Devolução com impedimento aberto
    Dado que assumi uma tarefa e sinalizei um impedimento nela
    Quando a devolvo
    Então ela volta a aguardar tomada na mesma etapa
    E o impedimento continua aberto com sua contagem em curso
    E ela continua disponível para ser assumida por qualquer participante
