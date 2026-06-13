import request from '../utils/request'
export const getWrongQuestions = () => request.get('/wrong-questions')
export const getWrongDetail = id => request.get(`/wrong-questions/${id}/detail`)
export const deleteWrongQuestion = id => request.delete(`/wrong-questions/${id}`)
