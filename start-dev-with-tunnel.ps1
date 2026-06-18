# 一键启动：MySQL 检测 + 后端 + 前端 + Cloudflare 快速隧道
# 用法：powershell -ExecutionPolicy Bypass -File start-dev-with-tunnel.ps1

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$backend = Join-Path $root "exam-system-backend"
$frontend = Join-Path $root "exam-system-web"

function Test-Port([int]$Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)
}

if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Error "未找到 cloudflared，请先安装: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/"
}

$mysql = Get-Service MySQL80 -ErrorAction SilentlyContinue
if ($mysql -and $mysql.Status -ne "Running") {
    Write-Warning "MySQL80 未运行，尝试启动..."
    try { Start-Service MySQL80 } catch { Write-Warning "无法自动启动 MySQL，请手动启动后重试" }
}

if (-not (Test-Port 8080)) {
    Write-Host "启动后端..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backend'; `$env:MYSQL_PASSWORD='123456'; mvn spring-boot:run"
    for ($i = 0; $i -lt 60; $i++) {
        if (Test-Port 8080) { break }
        Start-Sleep -Seconds 2
    }
}

if (-not (Test-Port 5173)) {
    Write-Host "启动前端..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontend'; npm run dev"
    for ($i = 0; $i -lt 30; $i++) {
        if (Test-Port 5173) { break }
        Start-Sleep -Seconds 2
    }
}

Write-Host ""
Write-Host "本地访问: http://localhost:5173"
Write-Host "正在启动 Cloudflare 快速隧道..."
Write-Host ""

Set-Location $root
cloudflared tunnel --url http://127.0.0.1:5173
