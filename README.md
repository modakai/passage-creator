# Sakura AI Passage Creator

Sakura AI Passage Creator 是一个前后端分离的 AI 图文创作平台，核心目标是把选题、标题、大纲、正文、配图、小红书笔记等内容生产流程串成可观测、可干预、可运营的完整系统。

项目当前包含两个主工程：

- `passage-creator`：前端应用，基于 Vue 3、TypeScript、Vite、shadcn-vue 和 Tailwind CSS。
- `passage-creator-api`：后端 API，基于 Spring Boot 3、Spring AI Alibaba、MyBatis-Flex、MySQL 和 Redis。

## 核心能力

- AI 文章创作：支持标题生成、大纲确认、正文生成、配图分析和图片生成的多阶段工作流。
- 小红书创作：支持面向小红书场景的选题搜索、内容生成、封面图提示词和正文配图提示词生成。
- 人机协作：标题、大纲等关键节点支持用户确认或修改后继续执行。
- 配图能力：支持 Pexels、Mermaid、Iconify、SVG 图示、OpenAI 图片生成等策略，并通过后端统一编排。
- 会员与额度：支持点数、模型计价、AI 用量记录和人工扫码充值流程。
- 后台管理：包含用户、文章、提示词模板、通知、字典、协议、审计日志、在线用户、系统配置和可观测性面板。
- 可观测性：后端提供接口监控、系统状态、登录安全事件、审计日志和 Prometheus 指标入口。

## 技术栈

### 前端

- Vue 3 + TypeScript + Vite
- shadcn-vue + Reka UI + Tailwind CSS
- Pinia + pinia-plugin-persistedstate
- vue-router + vite-plugin-vue-layouts
- TanStack Vue Query + TanStack Vue Table
- ofetch
- Vitest + ESLint
- pnpm

### 后端

- Java 17
- Spring Boot 3.5.4
- Spring AI Alibaba
- Spring Modulith
- MyBatis-Flex
- MySQL 8.x
- Redis
- Lombok + MapStruct Plus
- Hutool
- Apache Fesod
- 阿里云 OSS SDK
- wx-java
- Maven

## 目录结构

```text
.
├── passage-creator/       # Vue 前端应用
├── passage-creator-api/   # Spring Boot 后端 API
├── docs/                  # 项目设计、功能说明和过程文档
├── openspec/              # OpenSpec 变更与规格文档
├── AGENTS.md              # 仓库级协作约束
└── README.md              # 项目说明
```

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 22.15+
- pnpm 10+
- MySQL 8.x
- Redis 6+

## 本地启动

### 1. 初始化 MySQL

后端开发环境配置文件当前默认连接本机 MySQL：

```text
数据库：sakura_passage_creator
用户名：root
密码：root
```

初始化脚本位于：

```text
passage-creator-api/sql/mysql/create_table.sql
passage-creator-api/sql/mysql/init_data.sql
```

注意：当前 MySQL 初始化脚本内部创建并切换的数据库名是 `sakura_boot_init`，而 `application-dev.yml` 默认连接的是 `sakura_passage_creator`。启动前必须统一这两个值，否则后端会连到没有初始化表的库。

更稳妥的做法是不要直接改公共配置，而是在 `passage-creator-api/.env.local` 中覆盖开发数据库连接：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sakura_boot_init
spring.datasource.username=root
spring.datasource.password=root
```

然后执行初始化脚本：

```bash
mysql -uroot -proot < passage-creator-api/sql/mysql/create_table.sql
mysql -uroot -proot sakura_boot_init < passage-creator-api/sql/mysql/init_data.sql
```

如果需要 PostgreSQL 结构参考，可查看：

```text
passage-creator-api/sql/postgresql/
```

注意：当前后端默认依赖 MySQL 驱动和 MySQL 方言，PostgreSQL 脚本主要用于后续适配参考。

### 2. 启动 Redis

开发环境默认 Redis 配置：

```text
Host：localhost
Port：6379
Database：1
Password：空
```

本机没有 Redis 时，可临时使用 Docker 启动：

```bash
docker run --name sakura-passage-redis -p 6379:6379 -d redis:7
```

### 3. 配置后端密钥

后端会读取 `passage-creator-api` 目录下的环境文件：

```text
.env
.env.${spring.profiles.active}
.env.local
```

开发环境可在 `passage-creator-api/.env.local` 或 `passage-creator-api/.env.dev` 中配置敏感信息：

```properties
DASH_SCOPE_KEY=
OPENAI_API_KEY=
OPENAI_BASE_URL=
PEXELS_API_KEY=
TAVILY_API_KEY=
OSS_ACCESS_KEY=
OSS_SECRET_KEY=
```

缺少部分第三方密钥时，相关能力会不可用或进入降级逻辑；但数据库、Redis、登录和后台基础功能仍应优先保证可运行。

### 4. 启动后端

```bash
cd passage-creator-api
mvn spring-boot:run
```

默认地址：

```text
服务根地址：http://localhost:8101
接口上下文：/api
完整接口前缀：http://localhost:8101/api
```

### 5. 启动前端

```bash
cd passage-creator
pnpm install
pnpm dev
```

前端开发环境默认读取 `passage-creator/.env.development`：

```text
VITE_SERVER_API_URL=http://localhost:8101
VITE_SERVER_API_PREFIX=/api
```

因此前端调用 `/user/login` 时，真实请求地址是：

```text
http://localhost:8101/api/user/login
```

## 默认账号

默认账号由初始化数据脚本提供，当前 MySQL 脚本内置账号如下：

```text
账号：sakura
密码：sakura123
```

该账号是本地初始化用的超级管理员，生产环境必须立即修改密码或删除后重新创建管理员。

如果初始化后没有可用管理员账号，可以先注册普通用户，再在数据库中把目标用户设置为管理员：

```sql
update user set user_role = 'admin' where user_account = '你的账号';
```

## 常用命令

### 前端

```bash
cd passage-creator

pnpm dev        # 启动开发服务
pnpm build      # 类型检查并构建生产包
pnpm preview    # 预览生产构建
pnpm lint       # ESLint 检查
pnpm lint:fix   # 自动修复 lint 问题
pnpm test       # 运行 Vitest
```

### 后端

```bash
cd passage-creator-api

mvn spring-boot:run        # 启动开发服务
mvn test                   # 运行测试
mvn clean package          # 打包
mvn clean package -DskipTests
```

打包后运行：

```bash
java -jar target/passage-creator-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 环境变量说明

### 前端环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `VITE_SERVER_API_URL` | `http://localhost:8101` | 后端服务根地址，不要以 `/` 结尾 |
| `VITE_SERVER_API_PREFIX` | `/api` | 后端接口统一前缀，可为空 |
| `VITE_SERVER_API_TIMEOUT` | `5000` | 接口超时时间，单位毫秒 |
| `VITE_AUTH_TOKEN_HEADER_NAME` | `Authorization` | 主 Token 请求头名称 |
| `VITE_AUTH_TOKEN_HEADER_PREFIX` | `Bearer ` | 主 Token 请求头前缀 |
| `VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_ENABLED` | `true` | 是否同时发送兼容旧接口的 Token 请求头 |
| `VITE_AUTH_COMPATIBILITY_TOKEN_HEADER_NAME` | `token` | 兼容旧 Token 请求头名称 |
| `VITE_APP_TITLE` | `Sakura Admin` | 前端应用标题 |

### 后端关键环境变量

| 变量 | 说明 |
| --- | --- |
| `DASH_SCOPE_KEY` | DashScope / Spring AI Alibaba 模型调用密钥 |
| `OPENAI_API_KEY` | OpenAI 图片生成 API Key |
| `OPENAI_BASE_URL` | OpenAI 兼容接口地址，默认 `https://api.openai.com/v1` |
| `OPENAI_IMAGE_OUTPUT_FORMAT` | 图片输出格式，默认 `png` |
| `OPENAI_IMAGE_MAX_SECTION_IMAGES` | 单篇文章章节图片数量上限 |
| `PEXELS_API_KEY` | Pexels 图库检索 API Key |
| `TAVILY_API_KEY` | 小红书搜索 Agent 使用的 Tavily API Key |
| `OSS_ACCESS_KEY` | 阿里云 OSS AccessKey |
| `OSS_SECRET_KEY` | 阿里云 OSS SecretKey |
| `WX_MP_TOKEN` | 微信公众平台 Token |
| `WX_MP_AES_KEY` | 微信公众平台 AES Key |
| `WX_MP_APP_ID` | 微信公众平台 AppId |
| `WX_MP_SECRET` | 微信公众平台 Secret |
| `WX_OPEN_APP_ID` | 微信开放平台 AppId |
| `WX_OPEN_APP_SECRET` | 微信开放平台 AppSecret |

数据库、Redis、Token、OSS、可观测性等完整默认值在以下文件中维护：

```text
passage-creator-api/src/main/resources/application.yml
passage-creator-api/src/main/resources/application-dev.yml
passage-creator-api/src/main/resources/application-prod.yml
passage-creator-api/src/main/resources/application-test.yml
```

## Docker

当前仓库提供了前端和后端各自的 Dockerfile：

```text
passage-creator/Dockerfile
passage-creator-api/Dockerfile
```

后端镜像构建示例：

```bash
cd passage-creator-api
docker build -t sakura-passage-api:latest .
docker run --name sakura-passage-api -p 8101:8101 --env SPRING_PROFILES_ACTIVE=prod -d sakura-passage-api:latest
```

前端镜像构建示例：

```bash
cd passage-creator
docker build -t sakura-passage-web:latest .
docker run --name sakura-passage-web -p 80:80 -d sakura-passage-web:latest
```

根目录当前没有 `docker-compose.yml`。如果要一键编排 MySQL、Redis、后端和前端，需要补充 Compose 文件后再按统一部署链路启动。

## 常见问题

### 前端请求 404

检查 `VITE_SERVER_API_URL`、`VITE_SERVER_API_PREFIX` 是否与后端 `server.port`、`server.servlet.context-path` 一致。默认完整接口前缀应为 `http://localhost:8101/api`。

### 前端跨域失败

确认后端已启动，且前端请求地址指向真实后端地址。生产部署时建议通过 Nginx 把 `/api` 反向代理到后端，减少跨域配置复杂度。

### 登录失败或登录后立刻失效

优先检查 Redis 是否启动、连接配置是否正确，并确认前后端 Token 请求头一致。默认主请求头为 `Authorization: Bearer <token>`，同时兼容旧请求头 `token`。

### 数据库连接失败

确认 MySQL 已启动，并检查 `spring.datasource.url`、`spring.datasource.username`、`spring.datasource.password`。尤其要确认后端连接的数据库名与 SQL 初始化脚本创建的数据库名一致。

### AI 生成功能不可用

检查对应模型或工具密钥是否配置。例如文章和小红书 Agent 依赖 `DASH_SCOPE_KEY`，OpenAI 图片生成依赖 `OPENAI_API_KEY`，Pexels 图库检索依赖 `PEXELS_API_KEY`，Tavily 搜索依赖 `TAVILY_API_KEY`。

### pnpm install 后 Git hooks 安装失败

前端 `postinstall` 会执行 `simple-git-hooks`。如果当前环境不是标准 Git 工作区或没有写 hook 权限，可以确认 Git 仓库状态后重新执行 `pnpm install`。

## 相关文档

- `docs/project.md`：项目介绍和功能规划。
- `docs/article/`：文章创作主流程设计。
- `docs/feature-*.md`：功能模块设计说明。
- `openspec/specs/`：已归档或当前维护的规格说明。
