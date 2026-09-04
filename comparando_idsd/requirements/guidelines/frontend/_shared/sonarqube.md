# Qualidade de Código — SonarQube (Frontend)

> Regras extraídas do antigo `GUIDELINE_SONAR.md` (seções Frontend e CSS). Perfil `Banestes_way`.
> Transversal a qualquer stack web (`frontend/nextjs` elaborada; `frontend/react` = stub).
> Método de design agnóstico de framework: [`design-principles.md`](design-principles.md).

## JavaScript / TypeScript / Web

- **Tabnapping:** link externo com `target="_blank"` deve ter `rel="noopener noreferrer"`. (S5148)
- **URIs em HTML:** evitar URI absoluta em tag HTML da aplicação; preferir caminho relativo ou
  injetado por configuração. (S1829)
- **Tamanho de arquivo:** arquivo web (HTML/JS) não deve exceder 1000 linhas.
- **Comparações estritas:** sempre `===` e `!==`. (S888)
- **Parâmetros:** função com no máximo 7 parâmetros. (S107)

## CSS

- Evitar seletores excessivamente genéricos ou inválidos.
- Não duplicar propriedades dentro do mesmo bloco.
- `!important` proibido, exceto override justificado de biblioteca externa. (S4653)

## Regras de ouro para a IA

- Nunca suprimir alerta com comentário/anotação — corrigir estruturalmente.
- Assumir comparadores estritos e funções pequenas com poucos parâmetros.
