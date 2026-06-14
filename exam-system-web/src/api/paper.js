import request from '../utils/request'

export const getManualQuestions = (examId, params) =>
  request.get(`/exam/${examId}/manual/questions`, { params })

export const saveManualPaper = (examId, questions) =>
  request.post(`/exam/${examId}/manual/save`, { questions })

export const getPaperPreview = examId =>
  request.get(`/exam/${examId}/preview`)

export const deletePaperQuestion = (examId, questionId) =>
  request.delete(`/exam/${examId}/manual/question/${questionId}`)

export const updatePaperQuestion = (examId, questionId, data) =>
  request.put(`/exam/${examId}/manual/question/${questionId}`, data)
