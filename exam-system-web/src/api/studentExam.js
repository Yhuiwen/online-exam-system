import request from '../utils/request'
export const startExam = id => request.post(`/student-exams/${id}/start`)
export const submitExam = data => request.post('/student-exams/submit', data)
export const getStudentExams = () => request.get('/student-exams')
export const getAnswers = id => request.get(`/student-exams/${id}/answers`)
