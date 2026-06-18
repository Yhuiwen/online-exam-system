# 命名隧道（使用自有域名，适合长期部署）
# 前置：完成 config.yml 配置，且 MySQL、后端、前端均已启动
# 用法：powershell -ExecutionPolicy Bypass -File cloudflare/start-named-tunnel.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$config = Join-Path $PSScriptRoot "config.yml"

if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Error "未找到 cloudflared，请先安装并登录: cloudflared tunnel login"
}

if (-not (Test-Path $config)) {
    Write-Error "未找到 cloudflare/config.yml，请复制 config.example.yml 并填写 tunnel ID 与域名"
}

Write-Host "正在启动命名隧道，配置文件: $config"
Set-Location $root
cloudflared tunnel --config $config run
