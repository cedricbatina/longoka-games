# Export premium pour une langue (kg ou ln). Pour kg+ln en une fois sous Linux/macOS/CI :
#   voir scripts/run-export-all-premium.sh
param(
  [string]$CycleId = (Get-Date -Format "yyyyMMdd"),
  [string]$MeaningLang = "fr",
  [string]$LanguageMode = "kg",
  [int]$CountPerPack = 24,
  [int]$WordSearchRows = 20,
  [int]$WordSearchCols = 20,
  [int]$CrosswordRows = 19,
  [int]$CrosswordCols = 19,
  [int]$ArrowwordRows = 17,
  [int]$ArrowwordCols = 17,
  [int]$GridEntries = 16,
  [int]$MorphoEntries = 12
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-PremiumGen {
  param(
    [string]$Type,
    [string]$Label,
    [string]$ExtraArgs = ""
  )

  $execArgs = "--cadence weekly --count $CountPerPack --meaningLang $MeaningLang --language $LanguageMode --type $Type --label $Label --tier premium --difficulty expert $ExtraArgs".Trim()

  Write-Host "Running premium generation: type=$Type label=$Label languages=$LanguageMode"
  & mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" "-Dexec.args=$execArgs"

  if ($LASTEXITCODE -ne 0) {
    throw "Premium generation failed for type=$Type label=$Label"
  }
}

Invoke-PremiumGen -Type "wordsearch" -Label "$CycleId-premium-wordsearch" -ExtraArgs "--rows $WordSearchRows --cols $WordSearchCols --maxEntries $GridEntries"
Invoke-PremiumGen -Type "crossword" -Label "$CycleId-premium-crossword" -ExtraArgs "--rows $CrosswordRows --cols $CrosswordCols --maxEntries $GridEntries"
# Arrowword est le plus coûteux : limiter aux profils de base sur CI.
Invoke-PremiumGen -Type "arrowword" -Label "$CycleId-premium-arrowword" -ExtraArgs "--rows $ArrowwordRows --cols $ArrowwordCols --maxEntries $GridEntries --profileSet base"
Invoke-PremiumGen -Type "domino" -Label "$CycleId-premium-domino" -ExtraArgs "--maxEntries $MorphoEntries"
Invoke-PremiumGen -Type "memory" -Label "$CycleId-premium-memory" -ExtraArgs "--maxEntries $MorphoEntries"
Invoke-PremiumGen -Type "scrabble" -Label "$CycleId-premium-scrabble" -ExtraArgs "--maxEntries $MorphoEntries"
Invoke-PremiumGen -Type "anagram" -Label "$CycleId-premium-anagram" -ExtraArgs "--maxEntries $MorphoEntries"

Write-Host "Done."
Write-Host "Output folders:"
Write-Host "  target/weekly/$CycleId-premium-wordsearch"
Write-Host "  target/weekly/$CycleId-premium-crossword"
Write-Host "  target/weekly/$CycleId-premium-arrowword"
Write-Host "  target/weekly/$CycleId-premium-domino"
Write-Host "  target/weekly/$CycleId-premium-memory"
Write-Host "  target/weekly/$CycleId-premium-scrabble"
Write-Host "  target/weekly/$CycleId-premium-anagram"
