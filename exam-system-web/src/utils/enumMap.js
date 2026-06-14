const questionTypeMap = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
}

const difficultyMap = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

const examStatusMap = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  CLOSED: '已关闭'
}

const studentExamStatusMap = {
  IN_PROGRESS: '进行中',
  SUBMITTED: '已提交',
  PENDING_REVIEW: '待批改'
}

const reviewStatusMap = {
  AUTO_GRADED: '自动判分',
  PENDING_REVIEW: '待批改',
  REVIEWED: '已批改'
}

const riskLevelMap = {
  NORMAL: '正常',
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险'
}

const userRoleMap = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const userStatusMap = {
  1: '启用',
  0: '禁用'
}

function format(map, value) {
  return map[value] ?? value
}

export const formatQuestionType = value => format(questionTypeMap, value)
export const formatDifficulty = value => format(difficultyMap, value)
export const formatExamStatus = value => format(examStatusMap, value)
export const formatStudentExamStatus = value => format(studentExamStatusMap, value)
export const formatReviewStatus = value => format(reviewStatusMap, value)
export const formatRiskLevel = value => format(riskLevelMap, value)
export const formatUserRole = value => format(userRoleMap, value)
export const formatUserStatus = value => format(userStatusMap, value)
