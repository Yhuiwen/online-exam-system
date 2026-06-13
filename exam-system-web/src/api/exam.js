import request from '../utils/request'
export const getExams = () => request.get('/exams')
export const getExam = id => request.get(`/exams/${id}`)
export const createExam = data => request.post('/exams', data)
export const updateExamStatus = (id, status) => request.put(`/exams/${id}/status`, null, { params: { status } })
export const getExamQuestions = id => request.get(`/exams/${id}/questions`)
export const autoPaper = (id, data) => request.post(`/exams/${id}/auto-paper`, data)
