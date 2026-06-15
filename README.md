# AI4Note

面向 AI 助手的大学课程知识库整理与智能导出平台。

本项目用于整理大学课程资料，包括课件 PDF、实验报告、往年真题、复习笔记等，并支持解析资料文本、检索课程内容、导出 Agent 可读取的课程知识包。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Axios、Pinia、Vue Router
- 后端：Java 8、Spring Boot 2.7、MySQL Connector、Apache PDFBox
- 数据库：MySQL 8
- 环境编排：Docker Compose

## 项目结构

```text
.
├─ frontend/                 # Vue 3 前端项目
├─ backend/                  # Spring Boot 后端项目
├─ database/                 # 数据库建表、约束、触发器、存储过程脚本
├─ storage/                  # 上传文件、解析文本、导出知识包目录
├─ docs/                     # 课程设计文档
├─ course-agent-kb-template/ # Agent 知识库导出模板
├─ docker-compose.yml        # MySQL 本地开发环境
└─ .env.example              # 环境变量示例
```

## 环境要求

本地开发建议安装：

- JDK 8+
- Maven 3.9+
- Node.js 20+
- npm 10+
- Docker Desktop

检查命令：

```bash
java -version
mvn -version
node -v
npm -v
docker -v
docker compose version
```

## 快速启动

### 1. 准备环境变量

复制环境变量样例：

```bash
copy .env.example .env
```

PowerShell 也可以使用：

```powershell
Copy-Item .env.example .env
```

默认配置会创建 MySQL 数据库：

```text
数据库名：ai4note
用户名：ai4note
密码：ai4note123
端口：3306
```

### 2. 启动数据库

在项目根目录执行：

```bash
docker compose up -d mysql
```

首次启动会自动执行 `database/` 下的建表、约束、触发器、存储过程和初始化数据脚本。

查看数据库状态：

```bash
docker compose ps
```

查看数据库日志：

```bash
docker compose logs -f mysql
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

项目已在 `backend/.mvn/maven.config` 中配置 Maven 本地依赖缓存目录：

```text
../.m2/repository
```

这样不会依赖或污染本机全局 Maven 仓库。

后端默认地址：

```text
http://localhost:8080
```

健康检查：

```text
http://localhost:8080/api/health
```

### 4. 启动前端

另开一个终端：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

前端开发服务器会把 `/api` 请求代理到 `http://localhost:8080`。

## 常用命令

启动数据库：

```bash
docker compose up -d mysql
```

停止数据库：

```bash
docker compose down
```

停止数据库并删除数据卷：

```bash
docker compose down -v
```

重新初始化数据库：

```bash
docker compose down -v
docker compose up -d mysql
```

运行后端：

```bash
cd backend
mvn spring-boot:run
```

运行前端：

```bash
cd frontend
npm run dev
```

构建前端：

```bash
cd frontend
npm run build
```

## 数据库脚本说明

Docker Compose 会按以下顺序初始化数据库：

```text
database/schema.sql       # 建库建表
database/constraints.sql  # 检查约束
database/triggers.sql     # 触发器
database/procedures.sql   # 存储过程
database/init-data.sql    # 初始化数据
```

如果修改了数据库脚本，并希望重新执行初始化，需要删除 MySQL 数据卷：

```bash
docker compose down -v
docker compose up -d mysql
```

## 本地配置说明

后端配置文件位于：

```text
backend/src/main/resources/application.yml
```

其中数据库连接、用户名、密码、文件存储路径都支持环境变量覆盖：

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
AI4NOTE_STORAGE_ROOT
```

如果不设置环境变量，会使用 `.env.example` 中对应的默认值。

## Git 说明

当前项目远程仓库：

```text
git@github.com:dotchanpu/ai4note.git
```

主分支：

```text
main
```
