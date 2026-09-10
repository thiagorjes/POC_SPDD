# language: pt
Funcionalidade: RF-016 — Consultar o tempo por etapa

  Cenário: SCN-016.1 — Séries de tempo separadas
    Dado que um projeto acumulou histórico em três etapas
    Quando consulto o tempo por etapa
    Então vejo, para cada etapa, o tempo de permanência agregado
    E vejo a espera de tomada como série distinta
    E vejo o tempo de impedimento como série distinta
    E nenhuma das três é apresentada como soma das outras

  Cenário: SCN-016.2 — Tentativa de agregar por pessoa
    Dado que sou o Product Owner do projeto
    Quando consulto o tempo por etapa
    Então nenhum recorte, filtro, ordenação ou exportação por pessoa me é oferecido
    E qualquer agregação por pessoa solicitada por outro caminho é recusada

  Cenário: SCN-016.3 — Projeto sem histórico
    Dado que um projeto foi criado e nenhuma tarefa concluiu etapa alguma
    Quando consulto o tempo por etapa
    Então sou informado de que ainda não há medição
    E nenhum valor zero me é apresentado como tempo
