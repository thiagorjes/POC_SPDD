"""
Validacao estrutural das skills — IDSD.

Contrato da stack nova:
  - frontmatter com name, description, camada, output-artifacts;
  - `satisfaz-gates` obrigatorio (lista, possivelmente vazia) e todo gate
    citado precisa existir em governance/gates.yaml;
  - o par (skill, gate) precisa bater com o que algum flow declara — skill que
    afirma satisfazer um gate que nenhum flow lhe atribui e inconsistencia;
  - toda skill precisa aparecer em ao menos um flow registrado;
  - secoes minimas: Objetivo, Workflow, Handoff;
  - se existe `template:`, o arquivo precisa existir;
  - se existe validate-rules.json, o campo `skill` precisa bater com o nome.

`canvas-dimensions` e a secao `## Canvas` sao opcionais: o canvas passa a ser
derivado por script, nao mantido a mao por cada skill.

Uso: python validate_skills.py [caminho/para/.agents/skills]
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import yaml

CAMPOS_OBRIGATORIOS = ["name", "description", "camada", "satisfaz-gates"]
SECOES_OBRIGATORIAS = ["## Objetivo", "## Workflow", "## Handoff"]
CAMADAS = {"control", "execution", "governance"}

# Skills herdadas da stack anterior que ainda nao cumprem o contrato novo. Sao
# validadas apenas pelas secoes minimas ate serem migradas.
#
# Vazio de proposito: manter uma isencao permanente e como manter um teste
# ignorado — ela deixa de ser divida visivel e vira regra tacita. Reabrir exige
# nomear a skill aqui e dizer por que.
LEGADO: set[str] = set()


def frontmatter(texto: str) -> dict:
    m = re.search(r"^---\n(.*?)\n---", texto, re.DOTALL)
    if not m:
        return {}
    try:
        return yaml.safe_load(m.group(1)) or {}
    except yaml.YAMLError as exc:
        return {"__erro__": str(exc)}


def carregar_governanca(raiz: Path) -> tuple[set[str], dict[str, set[str]], set[str]]:
    """Retorna (gates conhecidos, gates por skill segundo os flows, skills em flows)."""
    gates_file = raiz / "governance" / "gates.yaml"
    flows_dir = raiz / "governance" / "flows"
    gates: set[str] = set()
    por_skill: dict[str, set[str]] = {}
    em_flow: set[str] = set()

    if gates_file.exists():
        cat = yaml.safe_load(gates_file.read_text(encoding="utf-8")) or {}
        gates = {g["id"] for g in cat.get("gates", [])}

    if flows_dir.exists():
        for arq in flows_dir.glob("*.yaml"):
            flow = yaml.safe_load(arq.read_text(encoding="utf-8")) or {}
            for etapa in flow.get("etapas", []):
                nome = str(etapa.get("skill", "")).lstrip("/")
                if not nome:
                    continue
                em_flow.add(nome)
                por_skill.setdefault(nome, set()).update(etapa.get("satisfaz") or [])
    return gates, por_skill, em_flow


def validar(skill_dir: Path, raiz: Path, gov) -> tuple[list[str], list[str]]:
    gates_conhecidos, gates_por_skill, skills_em_flow = gov
    erros: list[str] = []
    avisos: list[str] = []
    nome_dir = skill_dir.name

    skill_md = skill_dir / "SKILL.md"
    if not skill_md.exists():
        return [f"[{nome_dir}] SKILL.md ausente"], []

    texto = skill_md.read_text(encoding="utf-8")
    fm = frontmatter(texto)

    if "__erro__" in fm:
        return [f"[{nome_dir}] frontmatter YAML invalido: {fm['__erro__']}"], []

    for secao in SECOES_OBRIGATORIAS:
        if not re.search(rf"^{re.escape(secao)}\s*$", texto, re.MULTILINE):
            erros.append(f'[{nome_dir}] secao obrigatoria ausente: "{secao}"')

    nome = fm.get("name", nome_dir)
    if nome != nome_dir:
        erros.append(f"[{nome_dir}] frontmatter name='{nome}' difere do diretorio")

    if nome_dir in LEGADO:
        avisos.append(f"[{nome_dir}] formato legado — migrar para o contrato novo")
        return erros, avisos

    for campo in CAMPOS_OBRIGATORIOS:
        if campo not in fm:
            erros.append(f"[{nome_dir}] frontmatter sem campo '{campo}'")

    camada = fm.get("camada")
    if camada and camada not in CAMADAS:
        erros.append(
            f"[{nome_dir}] camada invalida: '{camada}' "
            f"(esperado: {', '.join(sorted(CAMADAS))})"
        )

    declarados = set(fm.get("satisfaz-gates") or [])
    for gid in sorted(declarados - gates_conhecidos):
        erros.append(f"[{nome_dir}] gate declarado nao existe no catalogo: {gid}")

    if nome_dir not in skills_em_flow:
        erros.append(f"[{nome_dir}] nao aparece em nenhum flow registrado")
    else:
        atribuidos = gates_por_skill.get(nome_dir, set())
        for gid in sorted(declarados - atribuidos):
            erros.append(
                f"[{nome_dir}] declara satisfazer {gid}, mas nenhum flow lhe "
                f"atribui esse gate"
            )
        for gid in sorted(atribuidos - declarados):
            erros.append(
                f"[{nome_dir}] flow atribui {gid} a esta skill, mas o "
                f"frontmatter nao o declara"
            )

    template = fm.get("template")
    if template and not (raiz / template).exists():
        erros.append(f"[{nome_dir}] template declarado nao existe: {template}")

    rules = skill_dir / "validate-rules.json"
    if not rules.exists():
        avisos.append(f"[{nome_dir}] sem validate-rules.json")
    else:
        try:
            dados = json.loads(rules.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            erros.append(f"[{nome_dir}] validate-rules.json invalido: {exc}")
        else:
            if dados.get("skill") != nome_dir:
                erros.append(
                    f"[{nome_dir}] validate-rules.json declara skill="
                    f"'{dados.get('skill')}'"
                )
            saida = dados.get("modes", {}).get("output", {})
            for passo in saida.get("custom_steps", []):
                script = passo.get("script", "")
                if script and not (raiz / script).exists():
                    erros.append(
                        f"[{nome_dir}] custom_step aponta para script inexistente: "
                        f"{script}"
                    )

    return erros, avisos


def main() -> int:
    if len(sys.argv) > 1:
        skills_dir = Path(sys.argv[1]).resolve()
    else:
        skills_dir = Path(__file__).resolve().parents[1] / "skills"

    if not skills_dir.exists():
        print(f"ERRO: diretorio de skills nao encontrado: {skills_dir}", file=sys.stderr)
        return 2

    raiz = skills_dir.parents[1]
    gov = carregar_governanca(raiz)

    total_erros, total_avisos, n = 0, 0, 0
    for d in sorted(skills_dir.iterdir()):
        if not d.is_dir():
            continue
        n += 1
        erros, avisos = validar(d, raiz, gov)
        for e in erros:
            print(f"ERRO: {e}", file=sys.stderr)
        for a in avisos:
            print(f"AVISO: {a}")
        total_erros += len(erros)
        total_avisos += len(avisos)

    print(f"\n{n} skills verificadas - {total_erros} erro(s), {total_avisos} aviso(s).")
    return 1 if total_erros else 0


if __name__ == "__main__":
    sys.exit(main())
