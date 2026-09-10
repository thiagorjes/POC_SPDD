# language: pt
Funcionalidade: RF-021 — Administrar o sistema como administrador global

  Cenário: SCN-021.1 — Promoção única e auditada
    Dado que ainda não existe administrador global no sistema
    E que há uma conta corporativa com identificador verificado designada para isso
    Quando essa pessoa é promovida a administradora global
    Então a promoção fica registrada com quem foi promovido e quando
    E uma segunda promoção pelo mesmo caminho é recusada

  Cenário: SCN-021.2 — Acesso a projeto sem participação
    Dado que sou administrador global
    E que existe um projeto do qual não participo
    Quando abro esse projeto
    Então vejo o board e posso agir sobre as tarefas
    E me é indicado que estou agindo pelo alcance de administração global

  Cenário: SCN-021.3 — Limites que o alcance global não ultrapassa
    Dado que sou administrador global
    Quando consulto o tempo por etapa de qualquer projeto e tento alterar tempo já contado
    Então nenhuma visão me oferece tempo agregado por pessoa
    E a alteração de tempo já contado é recusada como para qualquer outro perfil
