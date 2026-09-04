# Segurança na Exposição de APIs (transversal)

> Autenticação, autorização e endurecimento de APIs expostas. Agnóstico de stack;
> implementação concreta (filtros, configuração do resource server) em
> `backend/<stack>/spring-boot.md` ou equivalente.

## 1. Modelo de identidade — RH-SSO (Keycloak/RHSSO)

- O **RH-SSO** é o Identity Provider corporativo (OpenID Connect / OAuth2).
- Toda API exposta atua como **OAuth2 Resource Server**: só aceita requests com
  `Authorization: Bearer <access_token>` emitido por um realm confiável do RH-SSO.
- O token é um **JWT** validado localmente (assinatura via JWKS do realm), sem round-trip por request.

### Validação obrigatória do token (a cada request)

- Assinatura contra o JWKS do issuer (cache do JWKS com rotação de chave).
- `iss` = issuer esperado do realm; `aud`/`azp` = client id desta API.
- `exp` / `nbf` / `iat` dentro da janela; _clock skew_ tolerado ≤ 60s.
- `scope` e/ou roles necessárias presentes (ver autorização).
- Rejeitar token opaco, `alg: none`, ou algoritmo fora da allowlist (`RS256`/`ES256`).

## 2. Transcode de token (token exchange RH-SSO)

Quando a API precisa chamar **outra API downstream** em nome do usuário, ela **não repassa
o token recebido**. Faz o *transcode* (OAuth2 Token Exchange, RFC 8693) no RH-SSO:

```
token do usuário (audience = API-A)
        │  POST /realms/<realm>/protocol/openid-connect/token
        │  grant_type=urn:ietf:params:oauth:grant-type:token-exchange
        │  subject_token=<token do usuário>  audience=API-B
        ▼
token de acesso novo (audience = API-B, escopo reduzido ao necessário)
```

Regras:

- Cada salto tem seu próprio token, com `audience` do destino e **escopo mínimo**.
- A troca é feita por um client confidencial dedicado (credenciais em cofre/segredo externo,
  nunca no código nem no repositório).
- Tokens transcodificados são cacheados por audience+subject até pouco antes do `exp`
  (margem ≥ 30s); nunca logados (ver [`logging-and-levels.md`](logging-and-levels.md)).
- Falha no transcode ⇒ `502`/`503` para o cliente (problema de infraestrutura de auth),
  não `401` (o usuário está autenticado).
- Chamadas *service-to-service* sem usuário no contexto usam `client_credentials`, também
  com audience e escopo específicos — nunca um "superclient".

## 3. Autorização

- **Autenticação ≠ autorização.** Todo endpoint declara explicitamente a permissão exigida;
  o default de qualquer rota não mapeada é **negar**.
- Autorização baseada em `scope` (o que o client pode) + roles/claims (o que o usuário pode).
- Checagem grosseira (tem acesso ao endpoint) na borda; checagem fina (é dono do recurso /
  pertence à agência X) no serviço, sobre dados reais — nunca confiando em id vindo do cliente.
- `403` quando autenticado sem permissão; `404` quando não se deve revelar a existência do recurso.
- Sem autorização baseada só em algo controlável pelo cliente (header custom, campo no body).

## 4. Endurecimento da exposição

- **HTTPS/TLS obrigatório** em toda borda; HSTS habilitado. Sem downgrade para HTTP.
- CORS restritivo: allowlist explícita de origens, métodos e headers; sem `*` com credenciais.
- Rate limiting / throttling por client e por IP na borda (gateway) — resposta `429` com `Retry-After`.
- Tamanho máximo de payload e de upload configurados; timeouts de leitura/escrita.
- Headers de resposta: `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` em respostas
  com dado sensível, sem header que exponha versão de framework/servidor.
- Bean Validation / validação de schema em **todo** input; rejeitar campo desconhecido quando o
  contrato for estrito.
- Documentação (Swagger UI) protegida ou desabilitada em produção (ver `openapi-swagger.md`).
- Mensagem de erro sem stack trace, SQL, host interno ou nome de classe (RFC 7807, ver
  [`api-standards.md`](api-standards.md)).
- Segredos (client secret do transcode, chaves, credenciais de banco) só via configuração
  externa / cofre; proibido em código, `application.yml` versionado ou variável de imagem.

## 5. Auditoria

- Eventos de segurança relevantes (login falho repetido, `403`, uso de escopo administrativo,
  transcode para audience sensível) registrados em `INFO`/`WARN` com `correlationId`, sujeito e
  ação — sem o token em si.

---

## Checklist

- [ ] API configurada como OAuth2 Resource Server; JWT validado localmente (assinatura, `iss`, `aud`, `exp`, `alg` allowlist)
- [ ] Downstream chamado com token **transcodificado** (audience + escopo mínimo), nunca o token repassado
- [ ] Client secret do transcode em cofre/segredo externo; token transcodificado cacheado e nunca logado
- [ ] Todo endpoint com permissão explícita; default = negar; checagem fina de posse no serviço
- [ ] TLS obrigatório, CORS allowlist, rate limit `429`, limite de payload, headers de segurança
- [ ] Erros sem vazamento; Swagger protegido/desligado em produção
- [ ] Eventos de segurança auditados com `correlationId`, sem token
