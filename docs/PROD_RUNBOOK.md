# Production Runbook

## 1) One-time setup

1. Copy `.env.prod.example` to `.env.prod`.
2. Set real values in `.env.prod`:
   - `SPRING_DATASOURCE_PASSWORD`
   - `APP_CRYPTO_KEY` (long random string, keep it safe)
3. Ensure PostgreSQL database `taskdb` exists.

## 2) First production boot

On first boot only, allow table creation:

```powershell
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"
.\scripts\start-prod.ps1
```

After first successful boot, stop app and remove override:

```powershell
Remove-Item Env:SPRING_JPA_HIBERNATE_DDL_AUTO
```

Then run again normally:

```powershell
.\scripts\start-prod.ps1
```

## 3) Normal startup

```powershell
.\scripts\start-prod.ps1
```

## 4) Backup

Set password for PostgreSQL tools:

```powershell
$env:PGPASSWORD="your-postgres-password"
```

Run backup:

```powershell
.\scripts\backup-postgres.ps1
```

Backup files are generated in `backups/`.

## 5) Restore (when needed)

```powershell
$env:PGPASSWORD="your-postgres-password"
.\scripts\restore-postgres.ps1 -BackupFile ".\backups\taskdb-YYYYMMDD-HHMMSS.dump"
```

To drop existing objects before restore:

```powershell
.\scripts\restore-postgres.ps1 -BackupFile ".\backups\file.dump" -Clean
```

## 6) Recommended operations

- Keep `APP_CRYPTO_KEY` fixed once production data exists.
- Run backup daily.
- Test restore at least once in a separate database.
