param(
  [string]$CycleId = (Get-Date -Format "yyyyMMdd"),
  [string]$MeaningLang = "fr",
  [int]$CountPerWeek = 24,
  [int]$Rows = 12,
  [int]$Cols = 12,
  [int]$MaxEntries = 10,
  # Genere aussi les packs morpho (anagrammes, etc.) — utile pour alimenter InDesign sur le meme rythme.
  [switch]$IncludeMorphoPacks
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Planification: ce script n'est pas un scheduler Windows en soi. Pour un intervalle regulier,
# planifiez-le avec le Planificateur de taches (ou CI) en pointant sur ce repo + Maven.
# Pour la gamme "premium" complete (mots fleches, dominos, memory, scrabble, anagrammes...),
# utilisez run-premium-cycle.ps1 ou run-premium-cycle-kg.ps1 / run-premium-cycle-ln.ps1.

function Invoke-BiweeklyGen {
  param(
    [string]$Type,
    [string]$Label
  )

  $execArgs = "--count $CountPerWeek --rows $Rows --cols $Cols --maxEntries $MaxEntries --meaningLang $MeaningLang --type $Type --label $Label"

  Write-Host "Running: type=$Type label=$Label count=$CountPerWeek"
  & mvn -q -DskipTests exec:java "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" "-Dexec.args=$execArgs"

  if ($LASTEXITCODE -ne 0) {
    throw "Generation failed for type=$Type label=$Label"
  }
}

$week1Label = "$CycleId-semaine-1"
$week2Label = "$CycleId-semaine-2"

Invoke-BiweeklyGen -Type "wordsearch" -Label $week1Label
Invoke-BiweeklyGen -Type "crossword" -Label $week2Label

if ($IncludeMorphoPacks) {
  Invoke-BiweeklyGen -Type "anagram" -Label "$CycleId-morpho-anagram"
}

Write-Host "Done."
Write-Host "Week1 folder: target/biweekly/$week1Label"
Write-Host "Week2 folder: target/biweekly/$week2Label"
if ($IncludeMorphoPacks) {
  Write-Host "Morpho anagram folder: target/biweekly/$CycleId-morpho-anagram"
}
