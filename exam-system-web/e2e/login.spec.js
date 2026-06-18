import { test, expect } from '@playwright/test'

test.describe('登录页', () => {
  test('展示登录表单', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: '在线考试与智能题库管理系统' })).toBeVisible()
    await expect(page.getByText('账号登录')).toBeVisible()
    await expect(page.getByLabel('用户名')).toBeVisible()
    await expect(page.getByLabel('密码')).toBeVisible()
    await expect(page.getByRole('button', { name: '登录' })).toBeVisible()
  })

  test('空表单可点击登录按钮', async ({ page }) => {
    await page.goto('/login')
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).toHaveURL(/login/)
  })
})
