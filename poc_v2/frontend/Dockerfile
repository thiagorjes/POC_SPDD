# syntax=docker/dockerfile:1
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
# Sem build args: as URLs de API e Keycloak sao lidas em runtime pelo layout raiz (ADR-008).
RUN npm run build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
RUN addgroup --system --gid 10002 kanban \
    && adduser --system --uid 10002 --ingroup kanban kanban
COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
USER kanban
EXPOSE 3000
CMD ["node", "server.js"]
