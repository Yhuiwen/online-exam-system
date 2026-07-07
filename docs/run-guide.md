# 本地运行指南

## 1. 环境要求

- JDK 17 或以上
- Maven 3.8+
- Node.js 18 或以上
- npm 9 或以上
- MySQL 8
- Docker Desktop 可选，仓库已提供 `docker-compose.yml`
- Redis 7 可选；Docker Compose 环境会启动 Redis，后端可按配置启用缓存和实时能力

## 2. Docker Compose 启动

当前仓库已提供 `docker-compose.yml` 和 `.env.example`。如果本机已安装 Docker Desktop，可优先使用 Docker Compose 启动 MySQL、Redis、后端和前端：

```powershell
copy .env.example .env
docker compose up -d --build
```

启动后建议检查：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`
- Knife4j：`http://localhost:8080/doc.html`

## 3. 数据库初始化

确保 MySQL 8 已启动，并使用本机 root 账号登录：

```powershell
cd exam-system-backend\src\main\resources
mysql -uroot -p --default-character-set=utf8mb4
```

进入 MySQL 后依次执行：

```sql
SOURCE schema.sql;
SOURCE data.sql;
SOURCE review-migration.sql;
SOURCE violation-migration.sql;
SOURCE civil-service-skill-migration.sql;
```

说明：

- `schema.sql` 会创建 `exam_system` 数据库和核心业务表。
- `data.sql` 会写入管理员、教师、学生演示账号，以及课程、题目和考试数据。
- `review-migration.sql`、`violation-migration.sql`、`civil-service-skill-migration.sql` 用于补充批改、考试异常监控和公考专项练习相关数据表。

## 4. 后端启动

进入后端目录：

```powershell
cd exam-system-backend
```

设置本机 MySQL 密码并启动：

```powershell
$env:MYSQL_PASSWORD="你的本地MySQL密码"
mvn spring-boot:run
```

也可以先打包再运行：

```powershell
mvn clean package -DskipTests
java -jar target\exam-system-backend-1.0.0.jar
```

后端默认地址：

```text
http://localhost:8080
```

后端配置文件位于：

```text
exam-system-backend/src/main/resources/application.yml
```

默认数据库连接为：

```text
jdbc:mysql://localhost:3306/exam_system
```

## 5. 前端启动

进入前端目录：

```powershell
cd exam-system-web
```

安装依赖并启动开发服务器：

```powershell
npm.cmd install
npm.cmd run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 会把 `/api` 请求代理到后端：

```text
http://localhost:8080
```

生产构建命令：

```powershell
npm.cmd run build
```

## 6. 演示账号

演示账号来自 `data.sql`，默认密码均为 `123456`。

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `123456` | 用户管理、课程管理、系统级功能 |
| 教师 | `teacher` | `123456` | 题库、组卷、考试、批改、监控、统计 |
| 学生 | `student` | `123456` | 在线考试、成绩查询、错题记录、公考专项练习 |

## 7. 推荐验证流程

1. 使用教师账号登录，进入题库管理页面，确认题目列表能正常加载。
2. 进入考试管理页面，查看或创建考试。
3. 进入手动组卷或自动组卷页面，确认题目可加入试卷。
4. 进入试卷预览页面，确认题目、答案、解析和分值显示正确。
5. 切换学生账号，进入可参加考试列表并开始答题。
6. 提交后切回教师账号，查看批改、统计和考试监控页面。

## 8. 常见启动问题

### MySQL 无法连接

检查 MySQL 服务是否启动、3306 端口是否监听、root 密码是否正确，以及是否已经执行数据库初始化脚本。后端默认读取 `MYSQL_PASSWORD` 环境变量，如果没有设置则使用 `123456`。

### 数据库表不存在

通常是只启动了后端但没有执行 SQL 脚本。请先执行 `schema.sql`，再执行 `data.sql` 和迁移脚本。

### 登录失败

确认数据库中已经写入 `admin`、`teacher`、`student` 三个账号。默认密码是 `123456`，数据库中保存的是 BCrypt 密文。

### 前端接口 404 或网络错误

确认后端是否运行在 `8080` 端口，前端是否运行在 `5173` 端口，并检查 `exam-system-web/vite.config.js` 中 `/api` 代理目标是否正确。

### PowerShell 无法运行 npm

如果 PowerShell 拦截 `npm.ps1`，请使用 Windows 命令版本：

```powershell
npm.cmd install
npm.cmd run dev
```

### Swagger 或 Knife4j 打不开

当前仓库已集成 Swagger / Knife4j。若 `/swagger-ui.html` 或 `/doc.html` 打不开，请先确认后端是否启动成功、端口是否为 `8080`，以及是否被登录鉴权或浏览器缓存影响。

## 9. 本次环境验证记录

本次落地以 GitHub 远端最新仓库为准，仓库已包含 `docker-compose.yml`、`.env.example`、`docker-compose.prod.yml`、Swagger/Knife4j、Redis、CI 和 AI/RAG 相关说明。此前在旧本地副本中执行过后端 `mvn -DskipTests compile` 编译验证；本次主要完成 README 展示、演示指南、运行指南和截图目录增强。
