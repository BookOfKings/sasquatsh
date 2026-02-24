docker run --rm mysql:8 mysqldump -h 35.233.243.215 -u j1lv2coreuser "-p{02F3B125-E499-48FA-8AEE-AACB17E5D2A9}" gamedaydb 2>$null | Out-File -FilePath "C:\code\gamenightapp\gamenight_backup.sql" -Encoding utf8
Write-Host "Export complete. File size:" (Get-Item "C:\code\gamenightapp\gamenight_backup.sql").Length "bytes"
