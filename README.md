# 在线考试与智能题库管理系统

一个基于 Spring Boot 与 Vue 3 构建的前后端分离在线考试平台，面向管理员、教师和学生三类用户，覆盖题库维护、智能组卷、在线答题、自动判分、人工批改、成绩分析和考试防作弊监控等完整业务流程。

本项目适合作为 Java Web 课程设计、毕业设计基础项目及 Java 后端开发简历项目。

## 技术栈

### 后端

| 技术 | 用途 |
| --- | --- |
| Spring Boot | Web 应用与业务服务基础框架 |
| Spring Security | 身份认证与角色权限控制 |
| JWT | 无状态登录认证 |
| MyBatis-Plus | 数据访问与分页查询 |
| MySQL | 业务数据存储 |
| Apache POI | Excel 题库导入、导出与模板生成 |
| Maven | 依赖管理与项目构建 |

### 前端

| 技术 | 用途 |
| --- | --- |
| Vue 3 | 前端应用框架 |
| Vite | 开发服务器与生产构建 |
| Element Plus | 页面组件与交互反馈 |
| Vue Router | 页面路由与访问控制 |
| Pinia | 登录状态与用户信息管理 |
| Axios | HTTP 请求封装 |
| ECharts | 成绩统计与数据可视化 |

## 功能模块

### 用户与权限

- 管理员、教师、学生三角色权限体系
- 用户注册、登录、JWT 鉴权
- 当前用户信息查询
- 用户启用与禁用
- 前端菜单与路由权限控制

### 题库管理

- 题目新增、编辑、删除与分页查询
- 按课程、题型、难度、知识点和关键词筛选
- 支持单选题、多选题、判断题、填空题和简答题
- Excel 导入模板下载
- Excel 批量导入与筛选导出
- 导入数据逐行校验与错误行提示
- 数据库重复题目及 Excel 文件内重复题目检测

### 试卷与考试

- 教师创建、发布和关闭考试
- 按题型、难度比例自动随机组卷
- 从课程题库手动选择题目组卷
- 调整试题顺序与试卷分值
- 完整试卷预览，展示题目、选项、答案和解析
- 发布后锁定试卷内容

### 学生答题与判分

- 学生查看可参加考试并开始答题
- 在线考试倒计时
- 单选、多选、判断和填空题自动判分
- 简答题进入教师人工批改流程
- 防止重复提交
- 成绩与答题详情查询
- 错题自动记录与解析查看

### 公考刷题与错题分析 Skill

- 按言语理解、数量关系、判断推理、资料分析和常识判断模块专项刷题
- 自动记录公考错题、错误次数与最近错误时间，并支持查看完整题目解析
- 统计总体正确率及各模块正确率，帮助定位薄弱模块
- 根据练习结果、模块表现和未掌握错题生成针对性复习建议

### 批改与统计

- 教师查询待批改答卷
- 简答题评分与评语填写
- 批改完成后自动更新最终成绩
- 教师端考试平均分、最高分、最低分和参与人数统计
- 题目正确率与成绩分布分析
- 学生端考试次数、平均分、错题数量和成绩趋势统计
- ECharts 柱状图与折线图可视化

### 考试防作弊

- 记录切换页面、窗口失焦、退出全屏、复制、粘贴和右键等异常行为
- 前后端五秒重复上报限制
- 教师查看考试异常汇总与学生异常明细
- 根据异常次数计算正常、低风险、中风险和高风险等级
- 异常监控不会自动交卷，不影响正常答题流程

### 工程管理

- 前后端分离目录结构
- 统一接口返回格式与全局异常处理
- GitHub 版本管理
- Maven 与 Vite 独立构建

## 系统角色

| 角色 | 主要权限 |
| --- | --- |
| 管理员 | 用户管理、课程管理、考试管理、试卷预览、考试监控和统计分析 |
| 教师 | 课程管理、题库管理、自动组卷、手动组卷、考试管理、人工批改、考试监控和统计分析 |
| 学生 | 参加考试、在线答题、成绩查询、错题查看和个人统计 |

## 项目结构

```text
Online Exam and Intelligent Question Bank System
├── exam-system-backend
│   ├── src/main/java/com/exam/system
│   │   ├── common          # 统一响应对象
│   │   ├── config          # Security、跨域等配置
│   │   ├── controller      # REST API
│   │   ├── dto             # 请求数据对象
│   │   ├── entity          # 数据库实体
│   │   ├── exception       # 业务异常与全局异常处理
│   │   ├── mapper          # MyBatis-Plus Mapper
│   │   ├── security        # JWT 与登录认证
│   │   ├── service         # 业务接口
│   │   ├── service/impl    # 业务实现
│   │   └── vo              # 响应视图对象
│   ├── src/main/resources
│   │   ├── application.yml
│   │   ├── schema.sql
│   │   ├── data.sql
│   │   └── *-migration.sql
│   └── pom.xml
├── exam-system-web
│   ├── src
│   │   ├── api             # 前端接口封装
│   │   ├── layout          # 主页面布局
│   │   ├── router          # 路由与权限守卫
│   │   ├── store           # Pinia 状态管理
│   │   ├── utils           # Axios、枚举格式化等工具
│   │   └── views           # 业务页面
│   ├── package.json
│   └── vite.config.js
├── docs/images             # 项目截图
└── README.md
```

## 环境要求

- Java 17
- Maven 3.8+
- Node.js 18+
- MySQL 8
- npm 9+

## 数据库初始化

1. 确保 MySQL 8 已启动。
2. 进入 SQL 文件目录。
3. 登录 MySQL，并依次执行建表脚本和测试数据脚本。

```bash
cd exam-system-backend/src/main/resources
mysql -uroot -p --default-character-set=utf8mb4
```

进入 MySQL 命令行后执行：

```sql
SOURCE schema.sql;
SOURCE data.sql;
SOURCE civil-service-skill-migration.sql;
```

`schema.sql` 会创建 `exam_system` 数据库及项目所需数据表，`data.sql` 会写入演示账号、课程、题目和考试数据。

`civil-service-skill-migration.sql` 会动态查找或创建“公务员考试”课程，初始化公考练习、答题和错题分析表，并写入 25 道示例题。脚本按课程、题型和题干检测已有题目，可重复执行而不会重复插入示例数据。

已有数据库需要保留数据时，请根据实际功能增量执行迁移脚本：

```sql
SOURCE review-migration.sql;
SOURCE violation-migration.sql;
SOURCE civil-service-skill-migration.sql;
```

> 请使用本机 MySQL 密码登录。README 和代码仓库中不应保存生产数据库密码。

## 后端启动

进入后端目录：

```bash
cd exam-system-backend
```

通过环境变量设置当前终端使用的 MySQL 密码。

PowerShell：

```powershell
$env:MYSQL_PASSWORD="你的本地MySQL密码"
mvn spring-boot:run
```

Windows CMD：

```bat
set MYSQL_PASSWORD=你的本地MySQL密码
mvn spring-boot:run
```

Linux 或 macOS：

```bash
export MYSQL_PASSWORD="你的本地MySQL密码"
mvn spring-boot:run
```

也可以先完成打包：

```bash
mvn clean package -DskipTests
java -jar target/exam-system-backend-1.0.0.jar
```

后端默认地址：`http://localhost:8080`

数据库密码由 `MYSQL_PASSWORD` 环境变量提供，具体配置可在 `application.yml` 中按本地环境调整。

## 前端启动

进入前端目录并安装依赖：

```bash
cd exam-system-web
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

Vite 开发服务器会将 `/api` 请求代理到 `http://localhost:8080`。

生产构建：

```bash
npm run build
```

## 默认测试账号

测试账号的登录密码均为 `123456`。

| 角色 | 用户名 | 主要用途 |
| --- | --- | --- |
| 管理员 | `admin` | 用户管理与系统级功能验收 |
| 教师 | `teacher` | 题库、组卷、考试、批改与监控 |
| 学生 | `student` | 在线考试、成绩与错题查询 |

> 测试账号仅用于本地开发和课程演示，部署前请修改默认密码。

## 核心业务流程

### 教师出题与组卷

```text
维护课程
  → 新增题目或通过 Excel 批量导入
  → 筛选课程题库
  → 自动组卷或手动选题组卷
  → 调整题目顺序与分值
  → 预览完整试卷
  → 发布考试
```

### 学生考试与成绩

```text
查看可参加考试
  → 开始考试
  → 在线答题与倒计时
  → 提交试卷
  → 客观题自动判分
  → 简答题等待教师批改
  → 查看最终成绩、答题详情与错题
```

### 教师批改与分析

```text
查看待批改答卷
  → 为简答题评分并填写评语
  → 系统重新计算总分
  → 查看成绩分布与题目正确率
  → 查看考试异常行为与风险等级
```

## 项目截图

> 将实际截图放入 `docs/images` 目录后，以下图片会自动显示。

### 登录与工作台

![登录页](docs/images/login.png)

![系统工作台](docs/images/dashboard.png)

### 题库与组卷

![题库管理](docs/images/question-manage.png)

![手动组卷](docs/images/manual-paper.png)

![试卷预览](docs/images/paper-preview.png)

### 考试与批改

![在线考试](docs/images/online-exam.png)

![主观题批改](docs/images/teacher-review.png)

### 统计与监控

![统计分析](docs/images/statistics.png)

![考试监控](docs/images/exam-monitor.png)

### 公考刷题与错题分析 Skill

按行测模块选择题目并进行专项练习。

![公考模块刷题](docs/images/civil-practice.png)

提交练习后查看正确率、答题结果与题目解析。

![公考练习结果](docs/images/civil-practice-result.png)

在公考错题本中查看个人答案、正确答案和详细解析。

![公考错题解析](docs/images/civil-wrong-detail.png)

通过模块正确率和最近练习趋势分析薄弱环节。

![公考分析建议](docs/images/civil-analysis.png)

## 项目亮点

- 完整覆盖“题库、组卷、发布、答题、判分、批改、统计”的考试业务闭环
- Spring Security 与 JWT 实现前后端分离的角色权限控制
- Excel 导入采用逐行校验，支持部分成功、错误定位和重复题目检测
- 自动组卷支持题型数量及难度比例配置，并提供题库不足的明确提示
- 手动组卷支持课程隔离、题目排序、自定义分值和发布后锁定
- 客观题自动判分与主观题人工批改协同，确保成绩状态准确
- 前后端双重异常上报限制与考试风险等级统计
- ECharts 展示成绩分布、题目正确率和学生成绩趋势
- 公考专项模块覆盖模块化刷题、错题解析、错误频次追踪和掌握状态管理
- 公考分析按模块统计正确率并识别薄弱项，结合练习数据自动生成复习建议
- 统一响应、业务异常处理和前端枚举格式化，降低前后端耦合

## 后续优化方向

- 引入 Redis 缓存、登录状态控制和热点数据加速
- 使用 WebSocket 实现考试倒计时同步与实时监考
- 增加考试通知、消息中心和教师批量发布能力
- 增加题目图片、公式和富文本编辑支持
- 增加题库标签体系、收藏、审核与版本记录
- 优化自动组卷策略，引入知识点覆盖率和试卷难度评分
- 增加 Docker Compose 一键部署和 CI/CD 流程
- 完善单元测试、接口测试和端到端自动化测试
- 增加操作日志、审计记录和敏感配置管理

## 版本管理

项目使用 Git 和 GitHub 进行版本管理。建议按功能创建分支，通过清晰的提交信息记录题库、考试、批改、统计等模块的开发过程。

## License

本项目主要用于课程学习、技术交流和个人项目展示。

## AI 出题功能

教师端题库管理页新增“AI 出题”入口。教师选择课程、题型、难度、知识点、生成数量、每题分值和额外要求后，可以先生成题目预览；预览中的题干、选项、答案、解析、难度、分值和知识点均可编辑。AI 生成内容不会直接入库，必须由教师确认后点击“保存入题库”才会批量写入现有 `question` 表。

### 接口说明

- `POST /api/ai/questions/generate`：生成题目预览，不入库。
- `POST /api/ai/questions/save`：保存教师确认后的 AI 题目到题库。

两个接口均要求登录，并且只允许 `ADMIN`、`TEACHER` 访问，`STUDENT` 访问会返回 403。

### Mock 模式

默认配置为 mock 模式，无需大模型 API Key 即可演示完整前后端流程：

```yaml
ai:
  provider: mock
```

也可以通过环境变量指定：

```powershell
$env:AI_PROVIDER="mock"
```

### 配置真实大模型

后端支持 OpenAI 兼容接口，API Key 不写入代码，优先从环境变量读取：

```powershell
$env:AI_PROVIDER="openai"
$env:OPENAI_API_KEY="你的 API Key"
$env:OPENAI_MODEL="gpt-4o-mini"
$env:OPENAI_BASE_URL="https://api.openai.com/v1"
```

`application.yml` 中的默认配置如下，没有 Key 或 provider 不是 `openai` 时会自动使用 Mock：

```yaml
ai:
  provider: ${AI_PROVIDER:mock}
  openai:
    api-key: ${OPENAI_API_KEY:}
    base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
    model: ${OPENAI_MODEL:gpt-4o-mini}
```

### 教师端使用流程

1. 使用教师或管理员账号登录。
2. 进入“题库管理”，点击“AI 出题”。
3. 填写课程、题型、难度、知识点、数量、分值和额外要求。
4. 点击“生成题目”，检查并编辑预览列表。
5. 删除不需要的题目，确认无误后点击“保存入题库”。
