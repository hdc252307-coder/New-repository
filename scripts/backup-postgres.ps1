param(
    [string]$Host = "localhost",
    [int]$Port = 5432,
    [string]$Database = "taskdb",
    [string]$Username = "postgres",
    [string]$OutputDir = ".\backups"
)

if ([string]::IsNullOrWhiteSpace($env:PGPASSWORD))
{
    Write-Error "PGPASSWORD is not set. Set it before running this script."
    exit 1
}

if (!(Test-Path $OutputDir))
{
    New-Item -Path $OutputDir -ItemType Directory | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputFile = Join-Path $OutputDir "$Database-$timestamp.dump"

pg_dump `
  -h $Host `
  -p $Port `
  -U $Username `
  -d $Database `
  -F c `
  -f $outputFile

if ($LASTEXITCODE -ne 0)
{
    Write-Error "Backup failed."
    exit $LASTEXITCODE
}

Write-Host "Backup completed: $outputFile"
