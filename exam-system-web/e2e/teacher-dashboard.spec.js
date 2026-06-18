import { test, expect } from '@playwright/test'

test.describe('教师工作台', () => {
  test.skip(!process.env.E2E_WITH_BACKEND, '需要后端与数据库，设置 E2E_WITH_BACKEND=1 后运行')

  test('教师登录后进入首页', async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('用户名').fill('teacher')
    await page.getByLabel('密码').fill('123456')
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page).not.toHaveURL(/login/)
    await expect(page.locator('.page-title, h1').first()).toBeVisible()
  })
})
