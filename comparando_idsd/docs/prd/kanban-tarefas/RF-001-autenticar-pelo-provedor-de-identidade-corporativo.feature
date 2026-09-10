# language: pt
Funcionalidade: RF-001 — Autenticar pelo provedor de identidade corporativo

  Cenário: SCN-001.1 — Entrada bem-sucedida
    Dado que sou uma pessoa cadastrada no provedor de identidade corporativo
    E que participo de ao menos um projeto
    Quando concluo a autenticação
    Então sou levado à lista dos projetos em que participo

  Cenário: SCN-001.2 — Autenticado sem participação
    Dado que sou uma pessoa cadastrada no provedor de identidade corporativo
    E que não participo de nenhum projeto
    Quando concluo a autenticação
    Então entro no sistema
    E vejo que ainda não participo de nenhum projeto

  Cenário: SCN-001.3 — Provedor de identidade indisponível
    Dado que o provedor de identidade corporativo está indisponível
    Quando tento entrar no sistema
    Então a entrada é recusada com a indisponibilidade informada
    E nenhuma forma alternativa de autenticação me é oferecida
