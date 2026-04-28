# Audite la structure backend-java/target pour distinguer l'arbre canonique target/packs/
# des reliquats legacy weekly/biweekly et des JSON a plat.
#
# Usage:
#   pwsh -File scripts/audit-target-layout.ps1
#   pwsh -File scripts/audit-target-layout.ps1 -ApplyFlatMoves
#
# - Par defaut: genere un rapport JSON et n'efface rien.
# - Avec -ApplyFlatMoves: deplace les JSON a plat publiables vers target/packs/_flat-legacy/
#   sans supprimer weekly/biweekly.

param(
  [switch]$ApplyFlatMoves
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path -Parent $PSScriptRoot
$targetRoot = Join-Path $backendRoot 'target'
$packsRoot = Join-Path $targetRoot 'packs'
$weeklyRoot = Join-Path $targetRoot 'weekly'
$biweeklyRoot = Join-Path $targetRoot 'biweekly'
$flatLegacyRoot = Join-Path $packsRoot '_flat-legacy'
$reportPath = Join-Path $targetRoot '_target-layout-audit.json'

function Test-JsonDisposable([string]$Name) {
  return $Name -match '(?i)(demo|démo|test|sample|preview)' -or $Name -match '(?i)(report|manifest)\.json$'
}

function Get-JsonFiles([string]$Path) {
  if (-not (Test-Path $Path)) { return @() }
  return Get-ChildItem -Path $Path -File -Filter *.json | Sort-Object Name
}

function Get-JsonFilesRecursive([string]$Path) {
  if (-not (Test-Path $Path)) { return @() }
  return Get-ChildItem -Path $Path -File -Recurse -Filter *.json | Sort-Object FullName
}

function Classify-FlatJson([System.IO.FileInfo]$File) {
  $disposable = Test-JsonDisposable $File.Name
  $isLikelyPack = -not $disposable
  [pscustomobject]@{
    name = $File.Name
    path = $File.FullName
    size = $File.Length
    disposable = $disposable
    recommendedAction = if ($disposable) { 'delete-or-ignore' } else { 'move-to-target/packs/_flat-legacy' }
    notes = if ($disposable) { 'Nom compatible avec un fichier de demo/test/report, ignore par le sync Longoka.' } else { 'JSON a plat non canonique; le sync privilegie target/packs puis weekly/biweekly.' }
    isLikelyPack = $isLikelyPack
  }
}

if (-not (Test-Path $targetRoot)) {
  Write-Host "Rien a auditer: $targetRoot absent."
  exit 0
}

$flatJson = Get-JsonFiles $targetRoot | ForEach-Object { Classify-FlatJson $_ }
$packsJson = Get-JsonFilesRecursive $packsRoot
$weeklyJson = Get-JsonFilesRecursive $weeklyRoot
$biweeklyJson = Get-JsonFilesRecursive $biweeklyRoot

if ($ApplyFlatMoves) {
  $movable = $flatJson | Where-Object { $_.recommendedAction -eq 'move-to-target/packs/_flat-legacy' }
  if ($movable.Count -gt 0) {
    New-Item -ItemType Directory -Force -Path $flatLegacyRoot | Out-Null
    foreach ($item in $movable) {
      $destination = Join-Path $flatLegacyRoot $item.name
      Move-Item -Path $item.path -Destination $destination -Force
      $item.recommendedAction = 'moved-to-target/packs/_flat-legacy'
      $item.path = $destination
      $item.notes = 'Deplace automatiquement vers l''arbre canonique des packs legacy a plat.'
    }
  }
}

$report = [pscustomobject]@{
  generatedAt = (Get-Date).ToString('s')
  targetRoot = $targetRoot
  canonicalRoot = $packsRoot
  legacyRoots = @($weeklyRoot, $biweeklyRoot)
  summary = [pscustomobject]@{
    packsJsonCount = @($packsJson).Count
    weeklyJsonCount = @($weeklyJson).Count
    biweeklyJsonCount = @($biweeklyJson).Count
    flatJsonCount = @($flatJson).Count
    flatLikelyPackCount = @($flatJson | Where-Object { $_.isLikelyPack }).Count
    flatDisposableCount = @($flatJson | Where-Object { $_.disposable }).Count
  }
  flatJson = $flatJson
  recommendations = @(
    'Target canonique: target/packs/.',
    'Fusionner target/weekly et target/biweekly vers target/packs/ avec merge-legacy-cadence-target-dirs.ps1 avant suppression manuelle.',
    'Laisser les fichiers demo/test/report hors publication ou les supprimer apres verification.',
    'Ne pas produire de nouveaux packs a la racine de target.'
  )
}

$report | ConvertTo-Json -Depth 6 | Set-Content -Path $reportPath -Encoding UTF8

Write-Host "Rapport ecrit: $reportPath"
Write-Host "packs=$(@($packsJson).Count) weekly=$(@($weeklyJson).Count) biweekly=$(@($biweeklyJson).Count) flat=$(@($flatJson).Count)"
