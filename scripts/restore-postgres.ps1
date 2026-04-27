param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$Host = "localhost",
    [int]$Port = 5432,
    [string]$Database = "taskdb",
    [string]$Username = "postgres",
    [switch]$Clean
)

if (!(Test-Path $BackupFile))
{
    Write-Error "Backup file not found: $BackupFile"
    exit 1
}

if ([string]::IsNullOrWhiteSpace($env:PGPASSWORD))
{
    Write-Error "PGPASSWORD is not set. Set it before running this script."
    exit 1
}

$args = @(
    "-h", $Host,
    "-p", $Port,
    "-U", $Username,
    "-d", $Database,
    "--no-owner",
    "--no-privileges"
)

if ($Clean)
{
    $args += "--clean"
}

$args += $BackupFile

pg_restore @args

if ($LASTEXITCODE -ne 0)
{
    Write-Error "Restore failed."
    exit $LASTEXITCODE
}

Write-Host "Restore completed from: $BackupFile"
