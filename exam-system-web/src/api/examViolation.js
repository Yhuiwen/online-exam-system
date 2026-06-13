import axios from 'axios'

const violationRequest = axios.create({
  baseURL: '/api',
  timeout: 5000
})

violationRequest.interceptors.request.use(config => {
  const token = localStorage.getItem('exam-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function unwrap(response) {
  if (response.data?.code !== 200) {
    return Promise.reject(new Error(response.data?.message || '异常监控请求失败'))
  }
  return response.data.data
}

export const reportViolation = data =>
  violationRequest.post('/exam-violation/report', data).then(unwrap)

export const getMyViolationSummary = studentExamId =>
  violationRequest.get(`/exam-violation/my/${studentExamId}`).then(unwrap)

export const getExamViolationSummary = examId =>
  violationRequest.get(`/exam-violation/exam/${examId}/summary`).then(unwrap)

export const getStudentExamViolations = studentExamId =>
  violationRequest.get(`/exam-violation/student-exam/${studentExamId}`).then(unwrap)
