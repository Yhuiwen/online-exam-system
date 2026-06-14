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

const sourceCategoryMap = {
  REAL_EXAM: '真题',
  MOCK_EXAM: '模拟题',
  SELF_AUTHORED: '自命题',
  PRACTICE: '练习题'
}

const examScopeMap = {
  NATIONAL: '国考',
  PROVINCIAL: '省考'
}

const sourceCategoryOptions = [
  ['PRACTICE', '练习题'],
  ['REAL_EXAM', '真题'],
  ['MOCK_EXAM', '模拟题'],
  ['SELF_AUTHORED', '自命题']
]

const examScopeOptions = [
  ['NATIONAL', '国考'],
  ['PROVINCIAL', '省考']
]

const provinceOptions = [
  '全国', '北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江',
  '上海', '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南', '湖北',
  '湖南', '广东', '广西', '海南', '重庆', '四川', '贵州', '云南', '西藏',
  '陕西', '甘肃', '青海', '宁夏', '新疆'
]

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
export const formatSourceCategory = value => format(sourceCategoryMap, value || 'PRACTICE')
export const formatExamScope = value => format(examScopeMap, value)
export { sourceCategoryOptions, examScopeOptions, provinceOptions }

export function formatSourceSummary(row) {
  if (!row) return '-'
  const category = row.sourceCategory || 'PRACTICE'
  if (category === 'PRACTICE') return '-'
  if (category === 'REAL_EXAM') {
    const parts = []
    if (row.examYear) parts.push(`${row.examYear}年`)
    if (row.examScope) parts.push(formatExamScope(row.examScope))
    if (row.province) parts.push(row.province)
    if (row.paperType) parts.push(row.paperType)
    if (row.sourceRef) parts.push(row.sourceRef)
    return parts.join(' · ') || '-'
  }
  return row.sourceRef || '-'
}
