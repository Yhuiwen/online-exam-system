-- 用 UTF-8 明文重写演示种子数据（修复后执行一次即可）
USE exam_system;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE sys_user SET real_name = '系统管理员' WHERE id = 1;
UPDATE sys_user SET real_name = '教师用户' WHERE id = 2;
UPDATE sys_user SET real_name = '学生用户' WHERE id = 3;

UPDATE course SET
  course_name = 'Java Web 开发',
  description = 'Spring Boot、MyBatis-Plus 与前后端分离实践'
WHERE id = 1;

UPDATE course SET
  course_name = '数据库原理',
  description = 'MySQL 数据建模与 SQL 基础'
WHERE id = 2;

UPDATE course SET
  course_name = '公务员考试',
  description = '行测模块化刷题与错题分析专用题库'
WHERE id = 3;

UPDATE exam SET exam_name = 'Java Web 基础测试' WHERE id = 1;
