# 公考刷题与错题分析 Skill

本模块基于现有在线考试系统增量实现，复用 `question` 题库表，并通过 `knowledge_tag` 区分行测模块。

## 功能范围

- 学生端公考模块刷题
- 按模块、难度和题量加载题目
- 提交练习后自动判分
- 自动记录未掌握错题
- 错题标记为已掌握或删除
- 统计练习次数、累计答题、总体正确率和未掌握错题数
- 按言语理解、数量关系、判断推理、资料分析、常识判断生成模块正确率
- 根据薄弱模块和错题数量生成复习建议

## 数据库初始化

首次使用前，在 MySQL 中执行：

```sql
SOURCE exam-system-backend/src/main/resources/civil-service-skill-migration.sql;
```

该脚本会新增三张业务表：

- `civil_practice_session`
- `civil_practice_answer`
- `civil_wrong_question`

同时会新增 `公务员考试` 课程和 25 道行测示例题。

## 后端接口

接口前缀：`/api/civil-service`

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/modules` | 查询行测模块 |
| GET | `/practice/questions` | 获取练习题 |
| POST | `/practice/submit` | 提交练习并判分 |
| GET | `/wrong-questions` | 查询公考错题本 |
| PUT | `/wrong-questions/{id}/mastered` | 标记错题已掌握 |
| DELETE | `/wrong-questions/{id}` | 删除错题 |
| GET | `/analysis/overview` | 查询练习概览 |
| GET | `/analysis/modules` | 查询模块分析 |
| GET | `/recommendations` | 查询复习建议 |

## 前端入口

学生登录后，左侧菜单会出现：

```text
公考刷题
```

页面路径：

```text
/civil-service-skill
```

## 简历写法

可写为：

> 在在线考试系统中增量实现公务员考试刷题与错题分析 Skill，复用通用题库模型，新增练习会话、答题记录和公考错题表，支持按行测模块刷题、自动判分、错题沉淀、模块正确率统计和个性化复习建议生成，提升系统从普通考试平台到个性化学习平台的扩展能力。
