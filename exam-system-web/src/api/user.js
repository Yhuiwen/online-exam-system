import request from '../utils/request'

export const getUsers = params => request.get('/users', { params })
export const createUser = data => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const resetUserPassword = (id, data) => request.put(`/users/${id}/password`, data)
export const updateUserStatus = (id, status) => request.put(`/users/${id}/status`, { status })
