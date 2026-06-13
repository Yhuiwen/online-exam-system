import request from '../utils/request'
export const getQuestions = params => request.get('/questions', { params })
export const createQuestion = data => request.post('/questions', data)
export const updateQuestion = (id, data) => request.put(`/questions/${id}`, data)
export const deleteQuestion = id => request.delete(`/questions/${id}`)
