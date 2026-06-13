import request from '../utils/request'

export const getPendingReviews = examId => request.get(`/review/exam/${examId}/pending`)
export const getReviewDetail = studentExamId => request.get(`/review/student-exam/${studentExamId}`)
export const reviewAnswer = (answerId, data) => request.post(`/review/answer/${answerId}`, data)
