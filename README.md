# 在线考试与智能题库管理系统

## 项目结构

- `exam-system-backend`: Spring Boot 3、Java 17、MyBatis-Plus、Security、JWT
- `exam-system-web`: Vue 3、Vite、Element Plus、Pinia、Axios、ECharts

## 数据库初始化

1. 使用 MySQL 8 登录。
2. 执行 `exam-system-backend/src/main/resources/schema.sql`。
3. 执行 `exam-system-backend/src/main/resources/data.sql`。
4. 启动前通过 `MYSQL_PASSWORD` 环境变量设置自己的 MySQL 密码。未设置时使用本地开发默认值。

```sql
mysql -uroot -p --default-character-set=utf8mb4
SOURCE E:/JAVAWeb/Online Exam and Intelligent Question Bank System/exam-system-backend/src/main/resources/schema.sql;
SOURCE E:/JAVAWeb/Online Exam and Intelligent Question Bank System/exam-system-backend/src/main/resources/data.sql;
```

已有旧数据库且需要保留数据时，只执行主观题批改字段迁移：

```sql
SOURCE E:/JAVAWeb/Online Exam and Intelligent Question Bank System/exam-system-backend/src/main/resources/review-migration.sql;
```

## 启动

PowerShell 设置当前终端使用的 MySQL 密码：

```powershell
$env:MYSQL_PASSWORD="你的MySQL密码"
```

CMD 设置当前终端使用的 MySQL 密码：

```bat
set MYSQL_PASSWORD=你的MySQL密码
```

```bash
cd exam-system-backend
mvn spring-boot:run
```

```bash
cd exam-system-web
npm install
npm run dev
```

前端地址：`http://localhost:5173`，后端地址：`http://localhost:8080`。

## 默认账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | admin | 123456 |
| 教师 | teacher | 123456 |
| 学生 | student | 123456 |

## 接口测试

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"teacher\",\"password\":\"123456\"}"
```

取返回的 `token` 后访问受保护接口：

```bash
curl http://localhost:8080/api/courses \
  -H "Authorization: Bearer YOUR_TOKEN"
```
