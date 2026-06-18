import request from '../utils/request'

export const generateAiQuestions = data => request.post('/ai/questions/generate', data)
export const saveAiQuestions = data => request.post('/ai/questions/save', data)
export const generateAiPaper = data => request.post('/ai/questions/generate-paper', data)

export const parseAiDocument = (courseId, file, knowledgePoint) => {
  const formData = new FormData()
  formData.append('courseId', courseId)
  formData.append('file', file)
  if (knowledgePoint) formData.append('knowledgePoint', knowledgePoint)
  return request.post('/ai/questions/parse-document', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
