import request from '../utils/request'
export const getCourses = () => request.get('/courses')
export const createCourse = data => request.post('/courses', data)
export const updateCourse = (id, data) => request.put(`/courses/${id}`, data)
export const deleteCourse = id => request.delete(`/courses/${id}`)
