USE exam_system;

CREATE TABLE IF NOT EXISTS civil_practice_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  module_code VARCHAR(50),
  module_name VARCHAR(50),
  question_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  accuracy DECIMAL(6,2) NOT NULL DEFAULT 0,
  duration_seconds INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_civil_session_student(student_id, create_time)
);

CREATE TABLE IF NOT EXISTS civil_practice_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  module_code VARCHAR(50),
  user_answer TEXT,
  correct_answer TEXT,
  is_correct TINYINT NOT NULL DEFAULT 0,
  duration_seconds INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_civil_answer_student(student_id, create_time),
  INDEX idx_civil_answer_module(student_id, module_code)
);

CREATE TABLE IF NOT EXISTS civil_wrong_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  module_code VARCHAR(50),
  user_answer TEXT,
  correct_answer TEXT,
  wrong_count INT NOT NULL DEFAULT 1,
  mastered TINYINT NOT NULL DEFAULT 0,
  last_wrong_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_civil_wrong_student_question(student_id, question_id),
  INDEX idx_civil_wrong_student(student_id, mastered, last_wrong_time),
  INDEX idx_civil_wrong_module(student_id, module_code)
);

INSERT IGNORE INTO course(id, course_name, description, teacher_id) VALUES
(100, '公务员考试', '行测模块化刷题与错题分析专用题库', 2);

INSERT IGNORE INTO question(id,course_id,question_type,content,options_json,answer,analysis,difficulty,score,knowledge_tag,create_user_id) VALUES
(1001,100,'SINGLE_CHOICE','请选择与“釜底抽薪”含义最接近的一项。','["从根本上解决问题","临时补救问题","扩大问题影响","回避关键矛盾"]','A','“釜底抽薪”强调从根本上解决问题。','EASY',1,'言语理解',2),
(1002,100,'SINGLE_CHOICE','“发展数字经济，既要鼓励创新，也要守住安全底线。”这句话主要强调的是？','["创新与安全并重","只重视发展速度","限制数字技术发展","安全可以完全忽略"]','A','句子通过“既要……也要……”强调两个方面都重要。','EASY',1,'言语理解',2),
(1003,100,'SINGLE_CHOICE','填入句子最恰当的一项是：基层治理不能只靠“运动式”整治，更要形成____机制。','["长效","偶然","短暂","封闭"]','A','与“不能只靠运动式整治”相对，最合适的是“长效机制”。','EASY',1,'言语理解',2),
(1004,100,'SINGLE_CHOICE','对“未雨绸缪”理解正确的是？','["事先做好准备","雨后修补漏洞","等待问题出现","拒绝采取行动"]','A','“未雨绸缪”比喻事先做好准备。','EASY',1,'言语理解',2),
(1005,100,'SINGLE_CHOICE','文段强调：公共政策制定既要考虑效率，也要关注公平，不能让少数群体在改革中被忽视。该文段主旨是？','["政策制定应兼顾效率与公平","改革只需要关注效率","公平会阻碍公共治理","少数群体不应参与政策"]','A','文段核心是效率与公平的平衡。','MEDIUM',1,'言语理解',2),

(1006,100,'SINGLE_CHOICE','某商品原价100元，先涨价20%，再降价20%，现价为多少？','["96元","100元","104元","120元"]','A','100×1.2×0.8=96。','EASY',1,'数量关系',2),
(1007,100,'SINGLE_CHOICE','甲乙合做一项工程需6天，甲单独做需10天，则乙单独做需要多少天？','["15天","16天","18天","20天"]','A','乙效率为1/6-1/10=1/15，所以乙单独15天完成。','MEDIUM',1,'数量关系',2),
(1008,100,'SINGLE_CHOICE','数列1，3，7，15，31，下一项是？','["63","62","64","65"]','A','规律为前一项×2+1，31×2+1=63。','EASY',1,'数量关系',2),
(1009,100,'SINGLE_CHOICE','60人中有30人会A技能，40人会B技能，则至少有多少人同时会A和B？','["10人","20人","30人","40人"]','A','根据容斥下限，30+40-60=10。','MEDIUM',1,'数量关系',2),
(1010,100,'SINGLE_CHOICE','一辆车每小时行驶60千米，1.5小时可行驶多少千米？','["90千米","75千米","80千米","100千米"]','A','路程=速度×时间=60×1.5=90。','EASY',1,'数量关系',2),

(1011,100,'SINGLE_CHOICE','所有公务员都应遵守纪律，张某是公务员。由此可以推出？','["张某应遵守纪律","张某不需要遵守纪律","只有张某遵守纪律","无法推出任何结论"]','A','由全称命题和个体归属可以推出张某应遵守纪律。','EASY',1,'判断推理',2),
(1012,100,'SINGLE_CHOICE','医生：医院，与之关系最相似的是？','["教师：学校","司机：方向盘","作家：文字","农民：粮食"]','A','医生主要工作场所是医院，教师主要工作场所是学校。','EASY',1,'判断推理',2),
(1013,100,'SINGLE_CHOICE','如果A发生，则B发生。现在B没有发生，可以推出？','["A没有发生","A已经发生","B一定发生","无法判断A是否发生"]','A','充分条件命题否后必否前。','MEDIUM',1,'判断推理',2),
(1014,100,'SINGLE_CHOICE','公共服务是政府或公共组织为满足社会公共需要而提供的服务。下列属于公共服务的是？','["社区卫生服务","私人定制旅游","个人收藏交易","企业内部聚餐"]','A','社区卫生服务面向公共需要，符合定义。','EASY',1,'判断推理',2),
(1015,100,'SINGLE_CHOICE','某单位评优要求：只有完成年度考核，才能获得推荐资格。小李没有完成年度考核，因此？','["小李不能获得推荐资格","小李一定获得推荐资格","小李可能直接晋升","无法判断是否完成考核"]','A','“只有完成考核才有资格”等价于没有完成考核则没有资格。','MEDIUM',1,'判断推理',2),

(1016,100,'SINGLE_CHOICE','某市2024年地区生产总值为5000亿元，同比增长5%，按简单计算增长量约为？','["250亿元","200亿元","500亿元","100亿元"]','A','5000×5%=250。','EASY',1,'资料分析',2),
(1017,100,'SINGLE_CHOICE','某商店上半年销售额120万元，下半年180万元，全年平均每月销售额为？','["25万元","20万元","30万元","35万元"]','A','全年销售额300万元，平均每月300÷12=25万元。','EASY',1,'资料分析',2),
(1018,100,'SINGLE_CHOICE','某类产品A销量40件，B销量60件，总销量100件，A占总销量的比例为？','["40%","60%","20%","50%"]','A','40÷100=40%。','EASY',1,'资料分析',2),
(1019,100,'SINGLE_CHOICE','某指标2023年为200，2024年为240，则同比增长率为？','["20%","40%","16.7%","25%"]','A','增长率=(240-200)÷200=20%。','MEDIUM',1,'资料分析',2),
(1020,100,'SINGLE_CHOICE','某部门总人数80人，其中本科及以上48人，本科及以上人员占比为？','["60%","48%","40%","80%"]','A','48÷80=60%。','EASY',1,'资料分析',2),

(1021,100,'SINGLE_CHOICE','我国宪法规定，中华人民共和国的根本制度是？','["社会主义制度","资本主义制度","联邦制度","贵族制度"]','A','社会主义制度是中华人民共和国的根本制度。','EASY',1,'常识判断',2),
(1022,100,'SINGLE_CHOICE','行政机关依法行使职权应主要遵循的原则是？','["依法行政","随意行政","个人决定","秘密行政"]','A','依法行政是行政机关行使职权的基本要求。','EASY',1,'常识判断',2),
(1023,100,'SINGLE_CHOICE','“双碳”目标通常指的是？','["碳达峰和碳中和","碳排放和碳交易","低碳和无碳","煤炭和焦炭"]','A','“双碳”指碳达峰、碳中和。','EASY',1,'常识判断',2),
(1024,100,'SINGLE_CHOICE','公文中的“请示”主要适用于？','["向上级机关请求指示、批准","向下级布置工作","公开发布法规","记录会议情况"]','A','请示用于向上级机关请求指示、批准。','MEDIUM',1,'常识判断',2),
(1025,100,'SINGLE_CHOICE','计算机CPU主要由哪两部分组成？','["运算器和控制器","显示器和键盘","内存和硬盘","主板和电源"]','A','CPU主要包括运算器和控制器。','EASY',1,'常识判断',2);
