# 数据库设计说明：在线考试与智能题库管理系统

> 本文档用于项目展示、简历答辩和面试复盘。内容依据 `exam-system-backend/src/main/resources/schema.sql` 以及知识库、公考练习等迁移脚本整理，重点说明表结构如何支撑在线考试业务闭环。

## 1. 设计目标

本项目的数据库围绕“题库维护 → 组卷 → 发布考试 → 学生答题 → 自动判分 / 人工批改 → 成绩统计 → 错题沉淀 → 异常监控”的流程设计。核心目标包括：

- 支撑管理员、教师、学生三类用户的账号、角色和状态管理；
- 支撑课程、题库、考试、试卷题目和学生答卷之间的关系；
- 支撑客观题自动判分、主观题待批改、错题记录和成绩统计；
- 支撑考试异常行为记录，便于教师查看监控风险；
- 支撑 AI 出题和 RAG 知识库等扩展能力，但不影响主考试流程；
- 保持课程设计项目可读、可运行、可讲解，避免过度复杂化。

## 2. 核心表总览

| 表名 | 作用 | 关键字段 | 面试讲解重点 |
| --- | --- | --- | --- |
| `sys_user` | 用户表 | `username`、`password`、`role`、`status` | 三角色权限、BCrypt 密码、禁用用户控制 |
| `operation_log` | 操作日志表 | `user_id`、`module`、`action`、`path`、`success` | 记录关键操作，便于排查和审计 |
| `course` | 课程表 | `course_name`、`teacher_id` | 课程归属教师，是题库和考试的业务分组 |
| `question` | 题库表 | `course_id`、`question_type`、`difficulty`、`score`、`knowledge_tag`、`create_user_id` | 题型、难度、知识点、创建者归属 |
| `exam` | 考试表 | `course_id`、`teacher_id`、`start_time`、`end_time`、`duration_minutes`、`status` | 考试生命周期和时间窗口 |
| `exam_proctor` | 监考教师表 | `exam_id`、`teacher_id` | 支持一个考试关联多个监考教师 |
| `exam_question` | 试卷题目关联表 | `exam_id`、`question_id`、`sort_no`、`score` | 考试与题目多对多，保存题目顺序和卷面分 |
| `student_exam` | 学生考试记录表 | `student_id`、`exam_id`、`start_time`、`submit_time`、`total_score`、`status` | 每个学生每场考试唯一一次考试会话 |
| `student_answer` | 学生答案表 | `student_exam_id`、`question_id`、`answer`、`score`、`review_status` | 每题作答、得分、主观题批改状态 |
| `wrong_question` | 错题记录表 | `student_id`、`question_id`、`exam_id`、`student_answer`、`correct_answer` | 错题沉淀与复习 |
| `exam_violation` | 考试异常记录表 | `student_exam_id`、`student_id`、`exam_id`、`violation_type` | 切屏、失焦、复制粘贴等异常行为记录 |
| `course_knowledge_document` | 知识库文档表 | `course_id`、`title`、`file_type`、`chunk_count` | RAG 文档元数据 |
| `course_knowledge_chunk` | 知识库片段表 | `document_id`、`course_id`、`chunk_index`、`content`、`embedding_json` | 文档分片、关键词检索和 embedding 扩展 |
| `civil_practice_session` | 公考练习会话表 | `student_id`、`module_code`、`question_count`、`accuracy` | 公考模块化练习统计 |
| `civil_practice_answer` | 公考练习答案表 | `session_id`、`question_id`、`is_correct` | 记录每次练习的作答结果 |
| `civil_wrong_question` | 公考错题表 | `student_id`、`question_id`、`wrong_count`、`mastered` | 模块化错题复习和掌握状态 |

## 3. 核心关系设计

### 3.1 用户、课程与题库

```text
sys_user(teacher) 1 -- N course
course 1 -- N question
sys_user(teacher/admin) 1 -- N question(create_user_id)
```

- `course.teacher_id` 关联授课教师，用来表达课程归属。
- `question.course_id` 表示题目属于哪门课程，组卷时按课程筛选题目。
- `question.create_user_id` 表示题目创建者，便于教师只能维护自己创建的题目。
- 面试时可以强调：**课程归属和题目创建者归属是两层概念**，教师可以按课程组卷，但编辑、删除题目时要做创建者校验。

### 3.2 考试与试卷题目

```text
course 1 -- N exam
sys_user(teacher) 1 -- N exam
exam N -- N question，通过 exam_question 关联
```

- `exam` 保存考试元数据，例如课程、教师、考试时间、时长、总分、状态。
- `exam_question` 是考试与题目的中间表，同时保存 `sort_no` 和 `score`。
- 这样设计后，同一道题可以被多个考试引用，而不同考试中同一道题也可以设置不同分值。
- `uk_exam_question(exam_id, question_id)` 防止同一张试卷重复加入同一道题。

### 3.3 学生答题与批改

```text
sys_user(student) 1 -- N student_exam
exam 1 -- N student_exam
student_exam 1 -- N student_answer
question 1 -- N student_answer
```

- `student_exam` 表示某学生参加某场考试的会话，使用 `uk_student_exam(student_id, exam_id)` 限制同一学生同一考试只有一条记录。
- `student_answer` 表示每道题的作答情况，使用 `uk_student_answer(student_exam_id, question_id)` 防止同一题重复写入答案。
- 客观题提交后可直接计算 `score` 和 `is_correct`；主观题进入待批改状态，由教师更新 `review_status`、`review_comment`、`reviewer_id` 和 `review_time`。
- `wrong_question` 独立保存错题，便于后续复习，不需要反复扫描历史答卷。

### 3.4 异常行为监控

```text
student_exam 1 -- N exam_violation
exam 1 -- N exam_violation
student 1 -- N exam_violation
```

- `exam_violation` 记录一次异常行为，例如页面隐藏、窗口失焦、退出全屏、复制、粘贴、右键等。
- 建立 `idx_violation_exam` 和 `idx_violation_student_exam`，方便教师按考试和学生答卷查看异常记录。
- `idx_violation_deduplicate(student_exam_id, violation_type, create_time)` 用于支撑同类事件去重或查询。
- 设计原则是“记录与预警”，不建议单次异常就自动判作弊。

### 3.5 RAG 知识库扩展

```text
course 1 -- N course_knowledge_document
course_knowledge_document 1 -- N course_knowledge_chunk
course 1 -- N course_knowledge_chunk
```

- `course_knowledge_document` 保存上传文档的元信息，例如标题、原文件名、文件类型、文件大小和分片数量。
- `course_knowledge_chunk` 保存文档分片内容，预留 `embedding_json` 字段支持向量相似度计算。
- 该设计没有引入 Milvus、Elasticsearch 或 pgvector，适合课程设计规模；后续数据量变大时可以替换检索层。

## 4. 状态设计

## 4.1 用户状态

| 字段 | 常见值 | 含义 |
| --- | --- | --- |
| `sys_user.role` | `ADMIN`、`TEACHER`、`STUDENT` | 控制菜单、接口权限和业务身份 |
| `sys_user.status` | `1` / `0` | 表示账号启用或禁用 |

面试讲法：JWT 中可以携带用户 ID，但真实角色和状态最好以后端数据库为准，避免用户被禁用或角色变更后旧 token 仍继续生效。

## 4.2 考试状态

| 状态 | 含义 | 可执行操作 |
| --- | --- | --- |
| `DRAFT` | 草稿考试 | 教师可编辑、组卷、预览 |
| `PUBLISHED` | 已发布考试 | 学生可在时间范围内参加，试卷应锁定 |
| `CLOSED` / 结束态 | 考试结束 | 主要用于统计、批改和归档 |

面试讲法：发布后锁定试卷是为了避免学生已经答题后题目和分值发生变化，导致成绩不可追溯。

## 4.3 学生考试与答案状态

| 表 | 字段 | 含义 |
| --- | --- | --- |
| `student_exam` | `status` | 表示学生考试会话状态，如进行中、已提交、待批改等 |
| `student_answer` | `review_status` | 表示单题是否无需批改、待批改、已批改等 |

面试讲法：把“考试会话状态”和“单题批改状态”分开，可以同时支持客观题自动判分和主观题人工批改。

## 5. 关键约束与索引

| 约束 / 索引 | 作用 |
| --- | --- |
| `sys_user.username UNIQUE` | 用户名唯一，避免重复注册 |
| `question.idx_question_filter(course_id, question_type, difficulty)` | 支撑题库筛选和自动组卷 |
| `exam_question.uk_exam_question(exam_id, question_id)` | 防止同一试卷重复加入同一题 |
| `student_exam.uk_student_exam(student_id, exam_id)` | 防止同一学生重复开启同一考试记录 |
| `student_answer.uk_student_answer(student_exam_id, question_id)` | 防止同一学生同一题重复答题记录 |
| `exam_violation.idx_violation_exam` | 按考试查询异常记录 |
| `course_knowledge_chunk.idx_course_chunk(course_id, document_id, chunk_index)` | 按课程和文档组织知识库片段 |
| `civil_wrong_question.uk_civil_wrong_student_question(student_id, question_id)` | 公考错题按学生和题目唯一沉淀 |

## 6. 典型业务如何落到表设计

### 6.1 教师组卷

1. 教师创建 `exam` 草稿。
2. 按 `course_id`、`question_type`、`difficulty` 查询 `question`。
3. 手动或自动选择题目后写入 `exam_question`。
4. 更新 `exam.total_score`。
5. 发布考试后修改 `exam.status`，前端不再允许随意调整试卷。

### 6.2 学生答题

1. 学生进入考试时写入或读取 `student_exam`。
2. 加载 `exam_question` 和 `question`，但不返回答案和解析。
3. 学生提交后写入 `student_answer`。
4. 客观题立即判分，主观题写入待批改状态。
5. 汇总 `student_exam.total_score`，必要时进入待批改状态。

### 6.3 主观题批改

1. 教师查询 `student_answer.review_status = PENDING_REVIEW` 的记录。
2. 批改后写入 `score`、`review_comment`、`reviewer_id`、`review_time`。
3. 更新学生考试总分和状态。
4. 如果回答错误，写入或更新 `wrong_question`。

### 6.4 异常行为上报

1. 前端监听切屏、失焦、复制、粘贴等事件。
2. 后端写入 `exam_violation`。
3. 教师端按 `exam_id` 或 `student_exam_id` 查询异常汇总。
4. 业务上只作为风险提示，不直接替代人工判断。

## 7. 为什么这样设计

- **考试和题目分离**：题库可以复用，考试只保存题目引用、顺序和卷面分。
- **答卷和答案分离**：`student_exam` 管整体，`student_answer` 管单题，方便统计和批改。
- **客观题和主观题兼容**：统一记录答案，但用 `review_status` 区分自动判分和人工批改。
- **错题单独沉淀**：避免每次复习都扫描历史考试数据。
- **异常监控独立成表**：不污染考试主流程，便于后续统计风险分。
- **知识库独立扩展**：RAG 能力和考试核心表低耦合，便于启用或关闭。

## 8. 面试可讲的改进方向

- `question` 可增加更细的标签表，支持一题多知识点。
- `wrong_question` 可增加掌握状态、复习次数和最近复习时间。
- 大规模考试可为 `student_answer`、`exam_violation` 按 `exam_id` 或时间分区。
- RAG 部分可接入 pgvector、Milvus 或 Elasticsearch，提高大规模检索能力。
- 可增加更完整的审计日志，记录教师修改试卷、批改分数和管理员操作。
- 可增加乐观锁或版本号，进一步处理高并发提交和批改冲突。
