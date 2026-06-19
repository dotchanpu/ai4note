# AI4Note

面向 AI 助手的大学课程知识库整理与智能导出平台。

本项目用于整理大学课程资料，包括课件 PDF、实验报告、往年真题、复习笔记等，并支持解析资料文本、检索课程内容、导出 Agent 可读取的课程知识包。

## 当前已实现

- 用户注册与登录，密码使用 BCrypt 保存
- 用户课程列表、课程创建与信息修改
- 课程详情展示
- 课程章节列表、章节创建与信息修改
- 课程资料上传、文件落盘、分类展示、详情与信息修改
- PDF 按页提取与段落/句子感知分块、解析结果覆盖更新与文本预览
- 前后端接口错误提示与参数校验
- Vue 前端基础工作台

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Axios、Pinia、Vue Router
- 后端：Java 8、Spring Boot 2.7、MySQL Connector、Apache PDFBox
- 数据库：本地 MySQL 8
- 环境编排：Docker Compose，可选

## 项目结构

```text
.
├─ frontend/                 # Vue 3 前端项目
├─ backend/                  # Spring Boot 后端项目
├─ database/                 # 数据库建表、约束、触发器、存储过程和迁移脚本
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
- MySQL 8

检查命令：

```bash
java -version
mvn -version
node -v
npm -v
mysql --version
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
用户名：root
密码：123456
端口：3306
```

如果你使用已有 MySQL 账号，请修改 `.env` 和 `backend/src/main/resources/application.yml` 对应配置，或在启动后端前设置环境变量：

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ai4note
DB_USERNAME=root
DB_PASSWORD=123456
```

### 2. 初始化本地 MySQL 数据库

确认本地 MySQL 服务已启动：

```bash
mysqladmin ping -h localhost -P 3306 -u root -p
```

使用本地默认账号 `root/123456` 执行初始化脚本：

```powershell
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\schema.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\constraints.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\triggers.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\procedures.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\init-data.sql"
```

如果后续希望切换为独立开发账号 `ai4note/ai4note123`，可以额外执行：

```sql
CREATE USER IF NOT EXISTS 'ai4note'@'localhost' IDENTIFIED BY 'ai4note123';
CREATE USER IF NOT EXISTS 'ai4note'@'%' IDENTIFIED BY 'ai4note123';
GRANT ALL PRIVILEGES ON ai4note.* TO 'ai4note'@'localhost';
GRANT ALL PRIVILEGES ON ai4note.* TO 'ai4note'@'%';
FLUSH PRIVILEGES;
```

也可以直接执行项目提供的本地账号初始化脚本：

```bash
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\local-user.sql"
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

基础接口：

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/courses?userId={userId}
GET  /api/courses/{courseId}?userId={userId}
POST /api/courses
GET  /api/courses/{courseId}/chapters?userId={userId}
POST /api/courses/{courseId}/chapters?userId={userId}
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

初始化本地数据库：

```powershell
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\schema.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\constraints.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\triggers.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\procedures.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\init-data.sql"
```

创建默认开发账号：

```bash
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\local-user.sql"
```

可选：使用 Docker 启动 MySQL：

```bash
docker compose up -d mysql
```

停止 Docker MySQL：

```bash
docker compose down
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

本地 MySQL 和 Docker MySQL 都使用同一组脚本：

```text
database/schema.sql       # 建库建表
database/constraints.sql  # 检查约束
database/triggers.sql     # 触发器
database/procedures.sql   # 存储过程
database/init-data.sql    # 初始化数据
database/local-user.sql   # 本地开发账号，可选
database/migrations/      # 已有数据库升级脚本
```

已有数据库升级时，按编号顺序执行 `database/migrations/` 下的脚本：

```powershell
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\migrations\001_course_relation_ai_generation.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\migrations\002_ai_generation_constraints.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\migrations\003_teacher_profile_ai_evidence.sql"
cmd /c "mysql -h localhost -P 3306 -u root -p123456 < database\migrations\004_exam_mapping_and_gap_detection.sql"
```

如果使用 Docker MySQL，首次启动会自动执行 `database/` 下挂载的初始化脚本。如果修改了数据库脚本，并希望 Docker 重新执行初始化，需要删除 MySQL 数据卷：

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
