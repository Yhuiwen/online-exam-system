# 快速隧道（无需自有域名，适合临时演示）
# 前置：先启动 MySQL、后端(8080)、前端(5173)
# 用法：powershell -ExecutionPolicy Bypass -File cloudflare/start-quick-tunnel.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Test-Port([int]$Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)
}

if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Error "未找到 cloudflared，请先安装: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/"
}

if (-not (Test-Port 5173)) {
    Write-Error "前端未启动，请先在 exam-system-web 目录运行 npm run dev"
}

if (-not (Test-Port 8080)) {
    Write-Warning "后端 8080 未监听，登录/API 可能不可用"
}

Write-Host "正在创建 Cloudflare 快速隧道 -> http://127.0.0.1:5173"
Write-Host "启动后会显示 *.trycloudflare.com 公网地址，用该地址访问即可"
Write-Host ""

Set-Location $root
cloudflared tunnel --url http://127.0.0.1:5173
