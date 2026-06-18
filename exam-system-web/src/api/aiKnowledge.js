import request from '../utils/request'

export const uploadKnowledgeDocument = formData => request.post('/ai/knowledge/documents', formData)
export const listKnowledgeDocuments = courseId => request.get('/ai/knowledge/documents', { params: { courseId } })
export const deleteKnowledgeDocument = id => request.delete(`/ai/knowledge/documents/${id}`)
export const askKnowledgeQuestion = data => request.post('/ai/knowledge/ask', data)
