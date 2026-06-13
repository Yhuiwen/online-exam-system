import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getMe } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('exam-token') || '')
  const user = ref(JSON.parse(localStorage.getItem('exam-user') || 'null'))

  async function signIn(form) {
    const data = await login(form)
    token.value = data.token
    user.value = data.user
    localStorage.setItem('exam-token', data.token)
    localStorage.setItem('exam-user', JSON.stringify(data.user))
  }
  async function refreshUser() {
    user.value = await getMe()
    localStorage.setItem('exam-user', JSON.stringify(user.value))
  }
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('exam-token')
    localStorage.removeItem('exam-user')
  }
  return { token, user, signIn, refreshUser, logout }
})
