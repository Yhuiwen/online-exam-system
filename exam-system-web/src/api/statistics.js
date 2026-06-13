import request from '../utils/request'
export const getExamStatistics = id => request.get(`/statistics/exam/${id}`)
export const getStudentStatistics = () => request.get('/statistics/student')
