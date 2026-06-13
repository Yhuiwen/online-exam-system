import request from '../utils/request'
export const getQuestions = params => request.get('/questions', { params })
export const createQuestion = data => request.post('/questions', data)
export const updateQuestion = (id, data) => request.put(`/questions/${id}`, data)
export const deleteQuestion = id => request.delete(`/questions/${id}`)
export const downloadQuestionTemplate = () => request.get('/question/excel/template', { responseType: 'blob' })
export const importQuestions = file => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/question/excel/import', formData)
}
export const exportQuestions = params => request.get('/question/excel/export', {
  params,
  responseType: 'blob'
})
