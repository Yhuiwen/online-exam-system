# 在线考试与智能题库管理系统

![Java 17](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Vue 3](https://img.shields.io/badge/Vue-3-42b883?style=flat-square&logo=vuedotjs&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?style=flat-square&logo=mysql&logoColor=white)
![CI](https://github.com/Yhuiwen/online-exam-system/actions/workflows/ci.yml/badge.svg)

一个基于 Spring Boot 与 Vue 3 构建的前后端分离在线考试平台，面向管理员、教师和学生三类用户，覆盖题库维护、智能组卷、在线答题、自动判分、人工批改、成绩分析和考试防作弊监控等完整业务流程。

本项目适合作为 Java Web 课程设计、毕业设计基础项目及 Java 后端开发简历项目。面试答辩可参考 [docs/interview-guide.md](docs/interview-guide.md)。

## 项目预览

| 登录页 | 教师工作台 |
| --- | --- |
| ![登录页](docs/screenshots/01-login.png) | ![教师工作台](docs/screenshots/02-dashboard.png) |

| 题库管理 | 试卷预览 |
| --- | --- |
| ![题库管理](docs/screenshots/03-question-bank.png) | ![试卷预览](docs/screenshots/04-exam-preview.png) |

| 学生答题 | 成绩统计 |
| --- | --- |
| ![学生答题](docs/screenshots/05-student-exam.png) | ![成绩统计](docs/screenshots/06-statistics.png) |

## 快速导航

- [本地运行指南](docs/run-guide.md)
- [演示指南与面试讲解](docs/demo-guide.md)
- [数据库设计说明](docs/database-design.md)
- [面试答辩指南](docs/interview-guide.md)
- [项目截图目录](docs/screenshots)
- [数据库建表脚本](exam-system-backend/src/main/resources/schema.sql)
- [演示数据脚本](exam-system-backend/src/main/resources/data.sql)

## 演示账号

演示账号来自 `exam-system-backend/src/main/resources/data.sql`，默认密码均为 `123456`。

| 角色 | 用户名 | 密码 | 可演示功能 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `123456` | 用户管理、课程管理、考试管理、数据查看 |
| 教师 | `teacher` | `123456` | 题库维护、组卷、主观题批改、成绩统计 |
| 学生 | `student` | `123456` | 在线答题、查看成绩、错题记录 |

## 一分钟运行流程

本仓库已提供 `docker-compose.yml` 和 `.env.example`，可优先使用 Docker Compose 运行；也可按 [本地运行指南](docs/run-guide.md) 使用本地 MySQL + Maven + Vite 启动。

```powershell
copy .env.example .env
docker compose up -d --build
```

访问地址：`http://localhost:5173`。更完整的启动、排查和演示步骤见 [本地运行指南](docs/run-guide.md) 与 [演示指南](docs/demo-guide.md)。

## 项目亮点总览

- **三角色权限体系**：管理员 / 教师 / 学生，JWT + Spring Security，考试与题目按创建者隔离
- **在线考试闭环**：创建考试 → 组卷 → 发布 → 答题 → 判分 → 批改 → 统计
- **题库导入导出**：Excel 模板、逐行校验、重复检测，支持多题型与课程维度筛选
- **主观题批改**：简答题人工评分，批改完成后自动汇总最终成绩
- **防作弊监控**：前端行为上报 + 加权风险评分 + WebSocket 实时推送
- **AI 出题 / RAG 知识库**：AI 生成题目审核入库；MySQL 片段 + embedding 混合检索答疑
- **数据库设计可讲解**：用户、课程、题库、试卷、答卷、批改、错题和异常监控分表设计，详见 [数据库设计说明](docs/database-design.md)
- **Docker 部署与测试**：开发/生产 Compose 分离，57+ 单元测试 + CI + Playwright smoke E2E

## 系统架构说明

```mermaid
flowchart TB
    subgraph Client["用户浏览器"]
        Vue["Vue 3 + Vite + Element Plus"]
    end

    subgraph Gateway["接入层"]
        Nginx["Nginx / Vite Dev Proxy"]
    end

    subgraph Backend["Spring Boot 后端"]
        API["REST API + WebSocket STOMP"]
        Security["Spring Security + JWT"]
        Service["业务 Service 层"]
        MP["MyBatis-Plus"]
    end

    subgraph Data["数据与缓存"]
        MySQL[("MySQL 8")]
        Redis[("Redis 7")]
    end

    subgraph AI["AI 能力（可选）"]
        Provider["OpenAI 兼容 API / Mock"]
        RAG["RAG 检索 + 答疑"]
    end

    subgraph Deploy["部署"]
        Docker["Docker Compose"]
    end

    Vue --> Nginx
    Nginx --> API
    API --> Security --> Service --> MP
    MP --> MySQL
    Service --> Redis
    Service --> Provider
    Service --> RAG
    Docker --- Nginx
    Docker --- Backend
    Docker --- MySQL
    Docker --- Redis
```

## 核心业务流程

```mermaid
flowchart LR
    A[教师创建考试] --> B[题库选题 / 自动组卷]
    B --> C[预览并调整试卷]
    C --> D[发布考试]
    D --> E[学生登录并答题]
    E --> F{题型?}
    F -->|客观题| G[自动判分]
    F -->|简答题| H[进入待批改]
    G --> I[提交答卷]
    H --> J[教师人工批改]
    J --> I
    I --> K[成绩与错题记录]
    K --> L[统计分析 / 监控查看]
```

## 数据库核心表关系

```mermaid
erDiagram
    sys_user ||--o{ course : "teaches"
    sys_user ||--o{ question : "creates"
    sys_user ||--o{ exam : "creates"
    sys_user ||--o{ student_exam : "takes"
    course ||--o{ question : "contains"
    course ||--o{ exam : "belongs_to"
    exam ||--o{ exam_question : "has"
    question ||--o{ exam_question : "referenced_by"
    exam ||--o{ student_exam : "assigned_to"
    student_exam ||--o{ student_answer : "contains"
    question ||--o{ student_answer : "answered_as"
    sys_user ||--o{ student_answer : "reviews"
    student_exam ||--o{ exam_violation : "reports"
    exam ||--o{ exam_violation : "monitored_in"
    sys_user ||--o{ wrong_question : "records"
    question ||--o{ wrong_question : "wrong_on"
    exam ||--o{ wrong_question : "from_exam"
```

> 更详细的字段、约束、索引和状态设计见 [docs/database-design.md](docs/database-design.md)。
