param(
  [string]$CycleId = (Get-Date -Format "yyyyMMdd"),
  [string]$MeaningLang = "fr",
  [int]$Count = 24,
  [int]$Rows = 12,
  [int]$Cols = 12,
  [int]$MaxEntries = 10,
  [switch]$IncludeMorphoPacks
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-WeeklyGen {
  param(
    [string]$Type,
    [string]$Label
  )

  $execArgs = "--cadence weekly --count $Count --rows $Rows --cols $Cols --maxEntries $MaxEntries --meaningLang $MeaningLang --type $Type --label $Label"
  Write-Host "Running: cadence=weekly type=$Type label=$Label count=$Count"

  & mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" "-Dexec.args=$execArgs"
  if ($LASTEXITCODE -ne 0) {
    throw "Generation failed for type=$Type label=$Label"
  }
}

$weekLabel = "$CycleId-semaine-1"

# Hebdo: un seul dossier/label
Invoke-WeeklyGen -Type "wordsearch" -Label $weekLabel
Invoke-WeeklyGen -Type "crossword" -Label $weekLabel

if ($IncludeMorphoPacks) {
  Invoke-WeeklyGen -Type "anagram" -Label "$CycleId-morpho-anagram"
}

Write-Host "Done."
Write-Host "Week folder: target/weekly/$weekLabel"
if ($IncludeMorphoPacks) {
  Write-Host "Morpho anagram folder: target/weekly/$CycleId-morpho-anagram"
}

