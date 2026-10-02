param(
    [Parameter(Mandatory = $true)][string]$BackupPath,
    [string]$ComposeFile = ""
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($ComposeFile)) {
    $ComposeFile = Join-Path $repositoryRoot "docker-compose.yml"
}
$resolvedCompose = (Resolve-Path -LiteralPath $ComposeFile).Path
$resolvedBackup = (Resolve-Path -LiteralPath $BackupPath).Path
$databaseName = "securebiz_restore_" + ([Guid]::NewGuid().ToString("N").Substring(0, 12))
$containerPath = "/tmp/$databaseName.dump"

function Invoke-Compose([string[]]$Command) {
    & docker compose -f $resolvedCompose @Command
    if ($LASTEXITCODE -ne 0) { throw "Docker Compose command failed: $($Command -join ' ')" }
}

try {
    Invoke-Compose @("cp", $resolvedBackup, "postgres:$containerPath")
    Invoke-Compose @("exec", "-T", "postgres", "sh", "-c",
        "PGPASSWORD=`"`$POSTGRES_PASSWORD`" createdb --username=`"`$POSTGRES_USER`" $databaseName")
    Invoke-Compose @("exec", "-T", "postgres", "sh", "-c",
        "PGPASSWORD=`"`$POSTGRES_PASSWORD`" pg_restore --exit-on-error --no-owner --no-privileges --username=`"`$POSTGRES_USER`" --dbname=$databaseName $containerPath")
    Invoke-Compose @("exec", "-T", "postgres", "sh", "-c",
        "PGPASSWORD=`"`$POSTGRES_PASSWORD`" psql --username=`"`$POSTGRES_USER`" --dbname=$databaseName --tuples-only --command=`"SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1`"")
    Write-Host "Restore rehearsal passed in isolated database $databaseName." -ForegroundColor Green
}
finally {
    & docker compose -f $resolvedCompose exec -T postgres sh -c "PGPASSWORD=`"`$POSTGRES_PASSWORD`" dropdb --if-exists --force --username=`"`$POSTGRES_USER`" $databaseName"
    & docker compose -f $resolvedCompose exec -T postgres rm -f $containerPath
}
