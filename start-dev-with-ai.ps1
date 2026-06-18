# 本地开发：加载 .env 并启动后端（DeepSeek / OpenAI 兼容）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $root ".env"

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+?)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Item -Path "Env:$name" -Value $value
        }
    }
    Write-Host "Loaded environment from .env"
} else {
    Write-Host "No .env found. Copy .env.example to .env and set OPENAI_API_KEY."
}

if (-not $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD = "123456" }
if (-not $env:AI_PROVIDER) { $env:AI_PROVIDER = "mock" }

Set-Location (Join-Path $root "exam-system-backend")
mvn spring-boot:run
