import request from '../utils/request'

export const getCivilModules = () => request.get('/civil-service/modules')
export const getCivilPracticeQuestions = params => request.get('/civil-service/practice/questions', { params })
export const submitCivilPractice = data => request.post('/civil-service/practice/submit', data)
export const getCivilWrongQuestions = params => request.get('/civil-service/wrong-questions', { params })
export const markCivilWrongMastered = id => request.put(`/civil-service/wrong-questions/${id}/mastered`)
export const deleteCivilWrongQuestion = id => request.delete(`/civil-service/wrong-questions/${id}`)
export const getCivilOverview = () => request.get('/civil-service/analysis/overview')
export const getCivilModuleAnalysis = () => request.get('/civil-service/analysis/modules')
export const getCivilRecommendations = () => request.get('/civil-service/recommendations')
