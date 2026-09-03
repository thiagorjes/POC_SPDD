// Gera src/styles/tokens.css a partir de design-tokens.json (DDR-001).
// O CSS e derivado, nunca editado a mao: divergencia entre token e estilo vira bug de design.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const tokens = JSON.parse(readFileSync(resolve(raiz, 'design-tokens.json'), 'utf8'));

const linhas = [];
const emitir = (prefixo, objeto) => {
  for (const [chave, valor] of Object.entries(objeto)) {
    const nome = `${prefixo}-${chave.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)}`;
    if (valor !== null && typeof valor === 'object') {
      emitir(nome, valor);
    } else {
      linhas.push(`  --${nome}: ${valor};`);
    }
  }
};

emitir('cor', tokens.colors);
emitir('fonte', tokens.typography);
emitir('espaco', tokens.spacing);
emitir('raio', tokens.radius);
emitir('bp', tokens.breakpoints);
emitir('elevacao', tokens.elevation);
emitir('a11y', tokens.accessibility);

const css = `/* GERADO por scripts/gen-tokens.mjs. Nao edite manualmente. */\n:root {\n${linhas.join('\n')}\n}\n`;

mkdirSync(resolve(raiz, 'src/styles'), { recursive: true });
writeFileSync(resolve(raiz, 'src/styles/tokens.css'), css, 'utf8');
console.log(`tokens.css gerado com ${linhas.length} variaveis.`);
