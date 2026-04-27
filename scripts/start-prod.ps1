param(
    [string]$EnvFilePath = ".env.prod"
)

if (!(Test-Path $EnvFilePath))
{
    Write-Error "Env file not found: $EnvFilePath"
    Write-Host "Copy .env.prod.example to .env.prod and set real values."
    exit 1
}

Get-Content $EnvFilePath | ForEach-Object {
    $line = $_.Trim()

    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#"))
    {
        return
    }

    $parts = $line -split "=", 2
    if ($parts.Count -ne 2)
    {
        return
    }

    $key = $parts[0].Trim()
    $value = $parts[1].Trim()
    Set-Item -Path "Env:$key" -Value $value
}

Write-Host "Starting app in prod profile..."
.\mvnw.cmd spring-boot:run
