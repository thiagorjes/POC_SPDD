"""
Status checks de sincronia — IDSD.

Dois artefatos deste repositorio sao derivados e nao devem ser editados a mao.
Editar mesmo assim nao quebra nada na hora; quebra depois, quando alguem confia
no arquivo derivado e ele ja nao corresponde a fonte. Estes checks tornam a
divergencia visivel no build.

  canvas-drift  — docs/spdd/<feature>-canvas.md difere do regenerado das fontes
  skill-drift   — .claude/, .cursor/, .opencode/, .github/ diferem do .agents/

Uso:
    python check_drift.py [--root .] [--check canvas|skill|all]
"""

from __future__ import annotations

import argparse
import filecmp
import subprocess
import sys
import tempfile
from pathlib import Path

GERADOS = [".claude", ".cursor", ".opencode", ".github"]


def rodar(script: Path, *args: str) -> tuple[int, str]:
    r = subprocess.run(
        [sys.executable, str(script), *args],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    return r.returncode, (r.stdout or "") + (r.stderr or "")


def canvas_drift(raiz: Path, scripts: Path) -> list[str]:
    pasta = raiz / "docs" / "spdd"
    canvases = sorted(pasta.glob("*-canvas.md")) if pasta.is_dir() else []
    if not canvases:
        print("  canvas-drift: nenhum canvas — nada a verificar.")
        return []

    problemas = []
    for c in canvases:
        feature = c.stem.replace("-canvas", "")
        codigo, saida = rodar(scripts / "derive_canvas.py",
                              "--feature", feature, "--root", str(raiz), "--check")
        if codigo == 0:
            print(f"  canvas-drift OK: {feature}")
        else:
            problemas.append(saida.strip())
    return problemas


def diferencas(a: Path, b: Path, prefixo: str = "") -> list[str]:
    """Comparacao recursiva de duas arvores, por conteudo."""
    if not a.exists() and not b.exists():
        return []
    if not a.exists():
        return [f"{prefixo or b.name}: ausente no gerado"]
    if not b.exists():
        return [f"{prefixo or a.name}: ausente no repositorio"]

    cmp = filecmp.dircmp(str(a), str(b))
    saida = [f"{prefixo}{n}: so no repositorio" for n in cmp.left_only]
    saida += [f"{prefixo}{n}: faltando no repositorio" for n in cmp.right_only]
    iguais, diferentes, erros = filecmp.cmpfiles(
        str(a), str(b), cmp.common_files, shallow=False)
    saida += [f"{prefixo}{n}: conteudo divergente" for n in diferentes]
    saida += [f"{prefixo}{n}: nao foi possivel comparar" for n in erros]
    for sub in cmp.common_dirs:
        saida += diferencas(a / sub, b / sub, f"{prefixo}{sub}/")
    return saida


def skill_drift(raiz: Path, scripts: Path) -> list[str]:
    with tempfile.TemporaryDirectory() as tmp:
        destino = Path(tmp)
        codigo, saida = rodar(scripts / "generate_platform.py",
                              "--platform", "all", "--path", str(destino),
                              "--source", str(raiz / ".agents"))
        if codigo != 0:
            return [f"skill-drift: falha ao regenerar — {saida.strip()}"]

        problemas = []
        for nome in GERADOS:
            for d in diferencas(raiz / nome, destino / nome, f"{nome}/"):
                problemas.append(f"skill-drift: {d}")
        return problemas


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--root", default=".")
    p.add_argument("--check", default="all", choices=["canvas", "skill", "all"])
    args = p.parse_args()

    raiz = Path(args.root).resolve()
    scripts = Path(__file__).resolve().parent

    problemas: list[str] = []
    if args.check in ("canvas", "all"):
        print("canvas-drift")
        problemas += canvas_drift(raiz, scripts)
    if args.check in ("skill", "all"):
        print("skill-drift")
        problemas += skill_drift(raiz, scripts)
        if not problemas:
            print("  skill-drift OK: plataformas em sincronia com .agents/")

    if problemas:
        print()
        for p_ in problemas:
            print(f"ERRO: {p_}", file=sys.stderr)
        print(
            "\nArquivo derivado editado a mao. Corrija a fonte e regenere "
            "(derive_canvas.py / init.py); nao edite o derivado.",
            file=sys.stderr,
        )
        return 1

    print("\nOK - sem drift.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
