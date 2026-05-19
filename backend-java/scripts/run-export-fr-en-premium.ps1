# Export FR puis EN — dossiers séparés sous target/packs/ (pas d'écrasement FR/EN).
param(
  [string]$CycleId = (Get-Date -Format "yyyyMMdd"),
  [string]$LanguageMode = "kg",
  [switch]$SyncToLongoka,
  [string]$LongokaRoot = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$env:CYCLE_ID = $CycleId

foreach ($lang in @("fr", "en")) {
  Remove-Item Env:PREMIUM_LABEL -ErrorAction SilentlyContinue
  Write-Host ""
  Write-Host "=== Export meaningLang=$lang ===" -ForegroundColor Cyan
  & (Join-Path $PSScriptRoot "run-premium-cycle.ps1") -CycleId $CycleId -MeaningLang $lang -LanguageMode $LanguageMode
}

$packsRoot = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")) "target\packs"
Write-Host ""
Write-Host "Terminé. Dossiers sous: $packsRoot" -ForegroundColor Green
Get-ChildItem -LiteralPath $packsRoot -Directory | Sort-Object Name | Select-Object -Last 6 | ForEach-Object { Write-Host "  - $($_.Name)" }

if ($SyncToLongoka) {
  if (-not $LongokaRoot) {
    $LongokaRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\longoka") -ErrorAction Stop
  }
  $env:GAMES_PACKS_SOURCE = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")) "target"
  Push-Location $LongokaRoot
  try {
    pnpm games:sync
  }
  finally {
    Pop-Location
  }
}
