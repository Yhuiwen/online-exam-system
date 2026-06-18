import request from '../utils/request'

export const generateAiQuestions = data => request.post('/ai/questions/generate', data)
export const saveAiQuestions = data => request.post('/ai/questions/save', data)
