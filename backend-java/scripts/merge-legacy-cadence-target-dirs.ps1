# Fusionne les anciens dossiers target/weekly et target/biweekly vers target/packs/ (un seul arbre local).
# Usage : depuis backend-java :  pwsh -File scripts/merge-legacy-cadence-target-dirs.ps1
# Ne supprime pas les sources ; à faire à la main après vérification.

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$backendRoot = Split-Path -Parent $PSScriptRoot
$targetRoot = Join-Path $backendRoot "target"
$dest = Join-Path $targetRoot "packs"

if (-not (Test-Path $targetRoot)) {
  Write-Host "Rien à faire : $targetRoot absent."
  exit 0
}

New-Item -ItemType Directory -Force -Path $dest | Out-Null

foreach ($name in @("weekly", "biweekly")) {
  $src = Join-Path $targetRoot $name
  if (-not (Test-Path $src)) { continue }
  Write-Host ">>> Fusion $src -> $dest"
  & robocopy $src $dest /E /NFL /NDL /NJH /NJS /NP | Out-Null
  $code = $LASTEXITCODE
  if ($code -ge 8) {
    throw "robocopy a échoué (code $code) pour $src"
  }
}

Write-Host "Terminé. Vérifiez $dest puis supprimez manuellement $targetRoot\weekly et $targetRoot\biweekly si tout est bon."
