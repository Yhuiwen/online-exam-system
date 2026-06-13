import request from '../utils/request'
export const getUsers = params => request.get('/users', { params })
export const updateUserStatus = (id, status) => request.put(`/users/${id}/status`, null, { params: { status } })
