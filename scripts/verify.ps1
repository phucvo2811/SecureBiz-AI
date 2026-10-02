param([string]$PythonExecutable = "python", [switch]$SkipAi)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot

Push-Location (Join-Path $repositoryRoot "backend")
try {
    & .\mvnw.cmd -q test
    if ($LASTEXITCODE -ne 0) { throw "Backend verification failed" }
} finally { Pop-Location }

Push-Location (Join-Path $repositoryRoot "frontend")
try {
    & npm test
    if ($LASTEXITCODE -ne 0) { throw "Frontend tests failed" }
    & npm run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend verification failed" }
} finally { Pop-Location }

if (-not $SkipAi) {
    Push-Location (Join-Path $repositoryRoot "ai-service")
    try {
        & $PythonExecutable -m pytest -q
        if ($LASTEXITCODE -ne 0) { throw "AI verification failed" }
    } finally { Pop-Location }
}

Write-Host "SecureBiz AI verification passed." -ForegroundColor Green
