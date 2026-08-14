# syntax=docker/dockerfile:1

FROM node:24-alpine AS build
WORKDIR /workspace

COPY package.json ./
RUN npm install --no-audit --no-fund

COPY tsconfig*.json vite.config.ts index.html ./
COPY src ./src
RUN npm run build

FROM nginx:1.29-alpine AS runtime

RUN rm /etc/nginx/conf.d/default.conf \
    && mkdir -p /usr/share/nginx/html /etc/nginx/conf.d /run \
    && chown -R nginx:nginx \
        /usr/share/nginx/html \
        /var/cache/nginx \
        /var/run \
        /run \
        /etc/nginx/conf.d

COPY --from=build --chown=nginx:nginx /workspace/dist/ /usr/share/nginx/html/
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

ENV BACKEND_URL=http://backend:8080 \
    NGINX_ENVSUBST_OUTPUT_DIR=/etc/nginx/conf.d

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -q -O - http://127.0.0.1:8080/healthz >/dev/null || exit 1

USER nginx

CMD ["nginx", "-g", "daemon off;"]
