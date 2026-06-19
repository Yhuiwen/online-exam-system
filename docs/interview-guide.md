# 在线考试系统 — 面试讲解指南

> 本文档用于简历项目答辩、Java 后端 / 全栈岗位面试时的结构化讲解，内容基于仓库当前真实实现，不夸大未落地能力。

---

## 1. 项目一句话介绍

这是一个 **前后端分离的在线考试与智能题库管理系统**，支持管理员、教师、学生三角色，覆盖题库维护、手动/自动组卷、在线答题、客观题自动判分、主观题人工批改、成绩统计与考试防作弊监控；并集成 **AI 出题** 与 **轻量级 RAG 课程知识库答疑**。

---

## 2. 技术栈介绍

| 层次 | 技术 | 说明 |
| --- | --- | --- |
| 前端 | Vue 3、Vite、Element Plus、Pinia、Axios、ECharts | SPA，路由权限，API 代理 |
| 后端 | Spring Boot 3.3、Spring Security、JWT、MyBatis-Plus | REST + WebSocket（STOMP） |
| 数据 | MySQL 8、Redis 7 | 业务持久化；Redis 可选（限流等） |
| 文档/工具 | Knife4j、Apache POI、Playwright | 接口文档、Excel 导入导出、Smoke E2E |
| AI | OpenAI 兼容 API / Mock | 出题、组卷、RAG embedding、答疑 |
| 部署 | Docker Compose、Nginx | 开发 compose 暴露多端口；生产 compose 仅暴露前端 |

---

## 3. 核心业务流程

1. **教师** 创建草稿考试，绑定课程与时间窗口。
2. **组卷**：从同课程题库手动选题，或按题型/难度比例自动随机组卷；支持 AI 一键组卷（生成题目后写入试卷）。
3. **发布** 后试卷锁定，学生仅可见 `PUBLISHED` 考试。
4. **学生** 开始考试 → 倒计时答题 → 提交答卷。
5. **客观题** 自动判分；**简答题** 进入 `PENDING_REVIEW` 待批改队列。
6. **教师** 批改简答题后更新总分与答卷状态。
7. **统计**：考试维度平均分/分布/题目正确率；学生维度成绩趋势与错题本。
8. **监考**：前端上报异常行为，后端累计风险分，WebSocket 推送给监控页。

---

## 4. 我负责 / 实现的核心模块

可按个人实际参与情况选用表述：

- **用户与鉴权**：JWT 登录、角色权限、登录限流、禁用用户拦截。
- **题库与组卷**：题目 CRUD、Excel 导入导出、手动/自动组卷、试卷预览。
- **考试流程**：考试生命周期（草稿/发布/关闭）、学生答题会话、自动判分与提交幂等。
- **批改与统计**：简答题人工批改、成绩汇总、ECharts 可视化。
- **防作弊**：异常行为上报、加权风险评分、监控列表与 WebSocket 推送。
- **AI 能力**：AI 出题预览入库、文档解析、RAG 资料上传与混合检索答疑。
- **工程化**：ExamAccessGuard / QuestionAccessGuard、单元测试、Docker、CI。

---

## 5. 项目难点与解决方案

### 5.1 权限边界复杂

**难点**：教师只能管理自己创建的考试和题目，但组卷需要看到同课程他人题目；监考教师只能看监控不能改卷。

**方案**：抽取 `ExamAccessGuard`、`QuestionAccessGuard`，在 Service 层统一校验；Controller 只做角色粗筛。批改/统计接口同样走 `requireManageableExam`。

### 5.2 考试并发与重复提交

**难点**：学生重复点击提交、倒计时边界。

**方案**：`student_exam` 唯一约束 + Service 层状态机（`IN_PROGRESS` → `SUBMITTED` / `PENDING_REVIEW`）；提交接口幂等校验。

### 5.3 防作弊误报与性能

**难点**：前端频繁上报、同类事件刷屏。

**方案**：前后端 5 秒去重；加权累计风险分而非单次即判定；监控不自动交卷，避免误伤。

### 5.4 RAG 轻量落地

**难点**：不引入 Milvus 等重型组件，又要可演示语义检索。

**方案**：MySQL 存 chunk 与 `embedding_json`；关键词 + 向量余弦混合排序；Mock embedding 保证无 Key 可演示。

### 5.5 字符集与 Windows 环境

**难点**：Docker + PowerShell 导入 SQL 中文乱码。

**方案**：`utf8mb4` 全链路配置；Windows 用 `docker cp` + `source` 而非管道导入。

---

## 6. 权限安全如何设计

```
请求 → JwtAuthenticationFilter（Bearer Token，DB 刷新角色）
     → SecurityConfig（路径白名单 / authenticated）
     → @PreAuthorize（Controller 角色）
     → ExamAccessGuard / QuestionAccessGuard（资源级 owner 校验）
     → BusinessException(403) / AccessDeniedException
     → GlobalExceptionHandler（HTTP 状态码与统一 JSON）
```

要点：

- JWT 仅作身份载体，**角色以数据库为准**，防篡改 claim。
- 考试：`teacherId` 所有者；题目：`createUserId` 所有者；管理员 bypass。
- 学生只能访问已发布考试，题目接口不返回答案/解析。
- CORS 白名单（`CORS_ALLOWED_ORIGINS`）；生产 `prod` profile 强制强 `JWT_SECRET`。
- 删除已被 `exam_question` 引用的题目会拒绝，防止试卷缺题。

---

## 7. 防作弊如何实现

| 环节 | 实现 |
| --- | --- |
| 前端采集 | 切页、失焦、退出全屏、复制/粘贴/右键等事件 |
| 上报 | REST API，5 秒内同类型去重 |
| 存储 | `exam_violation` 表，关联 `student_exam` / `exam` |
| 评分 | `ViolationRiskUtils` 按类型加权累计 0+ 分，映射低/中/高风险 |
| 展示 | 教师监控页筛选 + 30s 轮询兜底 |
| 实时 | STOMP `/topic/exam/{examId}/monitor`，CONNECT 带 JWT |

设计原则：**只记录与预警，不自动交卷**，减少对正常答题的干扰。

---

## 8. AI / RAG 如何实现

### AI 出题

1. 教师配置课程、题型、难度、知识点等。
2. 后端调用 OpenAI 兼容 API（或 Mock）生成 JSON 结构题目。
3. 前端预览、可编辑，**确认后才** `save` 写入 `question` 表。

### RAG 知识库

1. 上传 PDF/DOCX/TXT/MD → 解析文本 → 分片写入 `course_knowledge_chunk`。
2. 分片时可选生成 embedding 存入 `embedding_json`。
3. 提问时：关键词打分 + 向量余弦相似度 → 加权合并 top-K。
4. 将「问题 + 引用片段」拼 prompt 调 LLM；依据不足时直接返回提示，不编造。
5. **未使用** Milvus / Elasticsearch / pgvector，适合课程设计规模。

---

## 9. 数据库设计说明

核心表职责：

| 表 | 职责 |
| --- | --- |
| `sys_user` | 用户账号、角色、状态 |
| `course` | 课程，关联授课教师 |
| `question` | 题库，归属课程与创建者 |
| `exam` | 考试元数据与状态机 |
| `exam_question` | 考试与题目多对多，含顺序与分值 |
| `student_exam` | 学生某次考试会话与总分 |
| `student_answer` | 每题作答、得分、批改状态 |
| `wrong_question` | 错题本 |
| `exam_violation` | 防作弊异常记录 |

设计要点：

- 考试与题目通过 `exam_question` 解耦，同一题目可被多卷引用（删除时校验引用）。
- `student_exam` 与 `student_answer` 支撑判分与批改状态流转。
- 主要查询字段有组合索引（如 `course_id + question_type + difficulty`）。

---

## 10. 面试官可能追问及回答

### Q1：为什么用 JWT 还要查数据库？

**A**：JWT 负责无状态认证，但用户在服务端可能被禁用或角色变更。过滤器里用 token 中的 userId 查库校验 `status` 和最新 `role`，避免仅信任客户端 claim。

### Q2：教师怎么能组卷用别人的题，却不能改别人的题？

**A**：组卷是**只读选用**同课程 `question`；编辑/删除走 `QuestionAccessGuard` 校验 `create_user_id`。列表查询不按创建者过滤，变更操作才校验所有权。

### Q3：如何防止学生看到答案？

**A**：`ExamServiceImpl.questions` 对学生调用 `requireViewableExam` 且 `includeAnswers=false`，清空 `answer` 和 `analysis` 字段后再返回。

### Q4：自动组卷如何保证公平随机？

**A**：按题型和难度比例从题库 pool 随机 shuffle 后 subList；数量不足抛业务异常，避免生成不完整试卷。

### Q5：RAG 不用向量数据库性能怎么办？

**A**：当前面向单课程小规模资料，chunk 数量有限，内存算余弦可接受。资料量上千再考虑 pgvector/Milvus，接口层已预留 embedding 存储。

### Q6：生产环境如何部署？

**A**：使用 `docker-compose.prod.yml`：MySQL/Redis/后端不暴露端口，仅 Nginx 80 对外；必须配置 `MYSQL_PASSWORD`、`JWT_SECRET`、`CORS_ALLOWED_ORIGINS`；后端启用 `prod` profile 校验 JWT 强度。

### Q7：测试怎么做？

**A**：后端 57 个 JUnit + Mockito 单测（权限、组卷、登录状态码等）；CI 跑 `mvn test` + `npm run build` + Playwright **smoke**（仅登录页，不启后端）。完整联调 E2E 需本地手动启全套服务并设 `E2E_WITH_BACKEND=1`。

### Q8：如果让你继续优化？

**A**：前端路由级权限隐藏、QuestionExcel 导入同样走 Guard、集成测试覆盖 Review API、RAG 接入专用向量库、Playwright 扩展关键业务路径（在 smoke 之外按需联调）。

---

## 附录：快速演示路径（5 分钟）

1. 登录 `teacher / 123456` → 题库管理 → 查看题目。
2. 考试管理 → 创建考试 → 手动/自动组卷 → 发布。
3. 登录 `student / 123456` → 参加考试 → 提交。
4. 教师 → 批改（如有简答）→ 统计分析。
5. （可选）考试监控、AI 出题、知识库答疑。

接口文档：`http://localhost:8080/doc.html`
