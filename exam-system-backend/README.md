# Exam System Backend

1. 使用 `mysql -uroot -p --default-character-set=utf8mb4` 登录 MySQL 8。
2. 使用 MySQL 的 `SOURCE` 命令依次执行 `schema.sql` 和 `data.sql`，避免 PowerShell 管道破坏中文编码。
3. 通过 `MYSQL_PASSWORD` 环境变量设置自己的 MySQL 密码。未设置时使用本地开发默认值。
4. 运行 `mvn spring-boot:run`。

```sql
SOURCE E:/JAVAWeb/Online Exam and Intelligent Question Bank System/exam-system-backend/src/main/resources/schema.sql;
SOURCE E:/JAVAWeb/Online Exam and Intelligent Question Bank System/exam-system-backend/src/main/resources/data.sql;
```

PowerShell：

```powershell
$env:MYSQL_PASSWORD="你的MySQL密码"
mvn spring-boot:run
```

CMD：

```bat
set MYSQL_PASSWORD=你的MySQL密码
mvn spring-boot:run
```

接口统一前缀为 `/api`，登录后在请求头携带 `Authorization: Bearer <token>`。
