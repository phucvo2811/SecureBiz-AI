param(
    [string]$OutputDirectory = "",
    [string]$ComposeFile = ""
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($ComposeFile)) {
    $ComposeFile = Join-Path $repositoryRoot "docker-compose.yml"
}
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $repositoryRoot "backups"
}
$resolvedCompose = (Resolve-Path -LiteralPath $ComposeFile).Path
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupPath = Join-Path $resolvedOutput "securebiz-$timestamp.dump"
$metadataPath = "$backupPath.sha256"
$arguments = @("compose", "-f", $resolvedCompose, "exec", "-T", "postgres", "sh", "-c",
    'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump --format=custom --no-owner --no-privileges --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"')

$process = Start-Process -FilePath "docker" -ArgumentList $arguments -NoNewWindow -Wait -PassThru -RedirectStandardOutput $backupPath
if ($process.ExitCode -ne 0) {
    Remove-Item -LiteralPath $backupPath -ErrorAction SilentlyContinue
    throw "PostgreSQL backup failed with exit code $($process.ExitCode)."
}
if ((Get-Item -LiteralPath $backupPath).Length -eq 0) {
    Remove-Item -LiteralPath $backupPath
    throw "PostgreSQL backup was empty."
}
$hash = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath $metadataPath -Value "$hash  $([System.IO.Path]::GetFileName($backupPath))" -Encoding ascii
Write-Host "Backup created: $backupPath" -ForegroundColor Green
Write-Host "SHA-256: $hash"
