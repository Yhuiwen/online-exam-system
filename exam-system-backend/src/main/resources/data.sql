USE exam_system;
-- 三个账号密码均为 123456（BCrypt）
INSERT INTO sys_user
(id, username, password, real_name, role, email, phone, status, create_time, update_time)
VALUES
(1, 'admin', '$2a$10$diJw72QhBlxt0JkLmxp/Ee1O3/VlWCDTt1GAShm3cFR.7679hxarS', '系统管理员', 'ADMIN', 'admin@example.com', '13800000001', 1, NOW(), NOW()),
(2, 'teacher', '$2a$10$diJw72QhBlxt0JkLmxp/Ee1O3/VlWCDTt1GAShm3cFR.7679hxarS', '教师用户', 'TEACHER', 'teacher@example.com', '13800000002', 1, NOW(), NOW()),
(3, 'student', '$2a$10$diJw72QhBlxt0JkLmxp/Ee1O3/VlWCDTt1GAShm3cFR.7679hxarS', '学生用户', 'STUDENT', 'student@example.com', '13800000003', 1, NOW(), NOW());

INSERT INTO course(id, course_name, description, teacher_id) VALUES
(1, 'Java Web 开发', 'Spring Boot、MyBatis-Plus 与前后端分离实践', 2),
(2, '数据库原理', 'MySQL 数据建模与 SQL 基础', 2);

INSERT INTO question(id,course_id,question_type,content,options_json,answer,analysis,difficulty,score,knowledge_tag,create_user_id) VALUES
(1,1,'SINGLE_CHOICE','Spring Boot 默认内嵌的 Web 容器是？','["Tomcat","Nginx","Apache HTTP Server","IIS"]','A','默认 starter-web 使用 Tomcat。','EASY',5,'Spring Boot',2),
(2,1,'SINGLE_CHOICE','用于依赖注入的常用注解是？','["@Autowired","@TableName","@Select","@BeanName"]','A','@Autowired 可按类型注入 Bean。','EASY',5,'Spring IoC',2),
(3,1,'SINGLE_CHOICE','JWT 通常由几部分组成？','["1","2","3","4"]','C','Header、Payload、Signature 三部分。','MEDIUM',5,'JWT',2),
(4,1,'SINGLE_CHOICE','HTTP 401 表示？','["资源不存在","未认证","服务器错误","请求成功"]','B','401 Unauthorized 表示未认证。','MEDIUM',5,'HTTP',2),
(5,1,'MULTIPLE_CHOICE','Spring Bean 常见作用域包括？','["singleton","prototype","request","compile"]','A,B,C','compile 不是 Bean 作用域。','EASY',5,'Spring IoC',2),
(6,1,'MULTIPLE_CHOICE','属于 HTTP 方法的是？','["GET","POST","PUT","SELECT"]','A,B,C','SELECT 是 SQL 关键字。','MEDIUM',5,'HTTP',2),
(7,1,'TRUE_FALSE','BCrypt 每次加密同一密码得到的字符串可能不同。',NULL,'TRUE','BCrypt 使用随机盐。','EASY',5,'Security',2),
(8,1,'TRUE_FALSE','JWT 必须保存在服务端 Session 中。',NULL,'FALSE','JWT 可用于无状态认证。','MEDIUM',5,'JWT',2),
(9,1,'FILL_BLANK','Java 17 中用于声明不可变数据载体的关键字是 ____。',NULL,'record','record 可简化 DTO。','EASY',5,'Java',2),
(10,1,'FILL_BLANK','MyBatis-Plus 通用 Mapper 接口名为 ____。',NULL,'BaseMapper','BaseMapper 提供基础 CRUD。','MEDIUM',5,'MyBatis-Plus',2),
(11,1,'SHORT_ANSWER','简述 Spring IoC 的含义。',NULL,'','由容器负责对象创建和依赖管理。','HARD',10,'Spring IoC',2),
(12,1,'SHORT_ANSWER','说明前后端分离项目中 JWT 的认证流程。',NULL,'','登录签发 token，请求携带 token，服务端校验并建立认证上下文。','HARD',10,'JWT',2),
(13,2,'SINGLE_CHOICE','用于查询数据的 SQL 关键字是？','["SELECT","UPDATE","DELETE","DROP"]','A','SELECT 用于查询。','EASY',5,'SQL',2),
(14,2,'SINGLE_CHOICE','MySQL 8 默认推荐字符集是？','["ascii","latin1","utf8mb4","binary"]','C','utf8mb4 支持完整 Unicode。','EASY',5,'MySQL',2),
(15,2,'MULTIPLE_CHOICE','属于聚合函数的是？','["COUNT","SUM","AVG","WHERE"]','A,B,C','WHERE 是过滤子句。','MEDIUM',5,'SQL',2),
(16,2,'TRUE_FALSE','主键可以重复。',NULL,'FALSE','主键必须唯一。','EASY',5,'数据库设计',2),
(17,2,'TRUE_FALSE','索引越多，写入性能一定越高。',NULL,'FALSE','索引会增加写维护成本。','MEDIUM',5,'索引',2),
(18,2,'FILL_BLANK','事务原子性的英文单词是 ____。',NULL,'Atomicity','ACID 中的 A。','MEDIUM',5,'事务',2),
(19,2,'SHORT_ANSWER','简述数据库事务 ACID。',NULL,'','原子性、一致性、隔离性、持久性。','HARD',10,'事务',2),
(20,2,'SHORT_ANSWER','说明建立数据库索引的优缺点。',NULL,'','提升查询效率，但占空间并增加写入维护成本。','HARD',10,'索引',2);

INSERT INTO exam(id,exam_name,course_id,teacher_id,start_time,end_time,duration_minutes,total_score,status) VALUES
(1,'Java Web 基础测试',1,2,'2025-01-01 00:00:00','2030-12-31 23:59:59',60,50,'PUBLISHED');
INSERT INTO exam_question(exam_id,question_id,sort_no,score) VALUES
(1,1,1,5),(1,2,2,5),(1,3,3,5),(1,5,4,5),(1,6,5,5),(1,7,6,5),(1,8,7,5),(1,9,8,5),(1,10,9,5),(1,11,10,5);
