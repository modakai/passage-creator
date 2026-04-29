# 前端生产镜像：先构建静态资源，再交给 Nginx 托管。
FROM node:22.15-alpine AS builder

WORKDIR /app

# 先复制依赖声明，提升 Docker 构建缓存命中率。
COPY package.json pnpm-lock.yaml ./

# 使用 Corepack 固定 pnpm 版本，和 packageManager 保持一致。
RUN corepack enable && corepack prepare pnpm@10.33.0 --activate

# Docker 构建阶段不需要安装 Git hooks，避免容器内没有标准 Git 环境导致 postinstall 失败。
RUN pnpm install --frozen-lockfile --ignore-scripts

# 复制源码并执行生产构建。
COPY . .
ARG VITE_SERVER_API_URL=http://localhost
ARG VITE_SERVER_API_PREFIX=/api
ARG VITE_SERVER_API_TIMEOUT=8000
ARG VITE_AUTH_TOKEN_HEADER_NAME=Authorization
ARG VITE_AUTH_TOKEN_HEADER_PREFIX="Bearer "
ARG VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_ENABLED=true
ARG VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_NAME=token
ARG VITE_APP_TITLE="Sakura Admin"
ENV VITE_SERVER_API_URL=${VITE_SERVER_API_URL}
ENV VITE_SERVER_API_PREFIX=${VITE_SERVER_API_PREFIX}
ENV VITE_SERVER_API_TIMEOUT=${VITE_SERVER_API_TIMEOUT}
ENV VITE_AUTH_TOKEN_HEADER_NAME=${VITE_AUTH_TOKEN_HEADER_NAME}
ENV VITE_AUTH_TOKEN_HEADER_PREFIX=${VITE_AUTH_TOKEN_HEADER_PREFIX}
ENV VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_ENABLED=${VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_ENABLED}
ENV VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_NAME=${VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_NAME}
ENV VITE_APP_TITLE=${VITE_APP_TITLE}
RUN pnpm build

# Nginx 只负责托管静态资源和代理 /api。
FROM nginx:1.27-alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
