# Duas saidas a partir da mesma base: `desenvolvimento`, que o compose usa, e
# `runtime`, que e a imagem enxuta sem codigo-fonte nem toolchain de build.
FROM node:24-alpine@sha256:50c8e8ca1d27439048670df5883f32d57cf81cff6233222c893fd0d9884cbd81 AS dependencias

WORKDIR /app
# Manifesto antes do codigo: mudanca de codigo nao reinstala dependencia.
COPY package.json package-lock.json ./
RUN npm ci

FROM dependencias AS desenvolvimento
COPY . .
USER node
EXPOSE 3000
CMD ["npm", "run", "dev"]

FROM dependencias AS construcao
COPY . .
RUN npm run build

FROM node:24-alpine@sha256:50c8e8ca1d27439048670df5883f32d57cf81cff6233222c893fd0d9884cbd81 AS runtime
WORKDIR /app
ENV NODE_ENV=production
# COPY de artefato especifico, nunca do contexto inteiro: a imagem final nao
# carrega codigo-fonte nem dependencia de build.
COPY --from=construcao /app/.next/standalone ./
COPY --from=construcao /app/.next/static ./.next/static
COPY --from=construcao /app/public ./public
USER node
EXPOSE 3000
CMD ["node", "server.js"]
