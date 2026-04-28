# Export premium pour une langue (kg ou ln). Pour kg+ln en une fois sous Linux/macOS/CI :
#   voir scripts/run-export-all-premium.sh
#
# Convention dossiers (alignée sur run-export-weekly-standard.sh / bihebdo : YYYYMMdd-semaine-*) :
#   tout est écrit sous target/packs/<CycleId-semaine-premium>/ (un seul répertoire par cycle).
#
# PowerShell (Windows) :
#   - Cadence : uniquement weekly (LONGOKA_CADENCE=biweekly est normalisé côté Java vers weekly).
#   - Arrowword : profils complets par défaut (robuste). Pour CI rapide : -ArrowwordProfileSet base
#   - Les scripts .sh nécessitent Git Bash ou WSL ; sans WSL, utiliser ce .ps1 + mvn.
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
  [int]$MorphoEntries = 12,
  [ValidateSet("full", "base")]
  [string]$ArrowwordProfileSet = "full"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$cadence = "weekly"
if ($env:LONGOKA_CADENCE) {
  $c = $env:LONGOKA_CADENCE.Trim().ToLowerInvariant()
  if ($c -eq "biweekly") {
    Write-Host "LONGOKA_CADENCE=biweekly -> normalisé vers weekly (meta.exportCadence)."
  }
}

# Même logique que le standard hebdo : un libellé de semaine unique pour tout le lot.
$PremiumLabel = "$CycleId-semaine-premium"

$arrowwordExtra = "--rows $ArrowwordRows --cols $ArrowwordCols --maxEntries $GridEntries"
if ($ArrowwordProfileSet -eq "base") {
  $arrowwordExtra = "$arrowwordExtra --profileSet base"
}

function Invoke-PremiumGen {
  param(
    [string]$Type,
    [string]$Label,
    [string]$ExtraArgs = ""
  )

  $execArgs = "--cadence $cadence --count $CountPerPack --meaningLang $MeaningLang --language $LanguageMode --type $Type --label $Label --tier premium --difficulty expert $ExtraArgs".Trim()

  Write-Host "Running premium generation: cadence=$cadence type=$Type label=$Label languages=$LanguageMode"
  & mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" "-Dexec.args=$execArgs"

  if ($LASTEXITCODE -ne 0) {
    throw "Premium generation failed for type=$Type label=$Label"
  }
}

Invoke-PremiumGen -Type "wordsearch" -Label $PremiumLabel -ExtraArgs "--rows $WordSearchRows --cols $WordSearchCols --maxEntries $GridEntries"
Invoke-PremiumGen -Type "crossword" -Label $PremiumLabel -ExtraArgs "--rows $CrosswordRows --cols $CrosswordCols --maxEntries $GridEntries"
Invoke-PremiumGen -Type "arrowword" -Label $PremiumLabel -ExtraArgs $arrowwordExtra
Invoke-PremiumGen -Type "domino" -Label $PremiumLabel -ExtraArgs "--maxEntries $MorphoEntries"
Invoke-PremiumGen -Type "memory" -Label $PremiumLabel -ExtraArgs "--maxEntries $MorphoEntries"
Invoke-PremiumGen -Type "scrabble" -Label $PremiumLabel -ExtraArgs "--maxEntries $MorphoEntries"
Invoke-PremiumGen -Type "anagram" -Label $PremiumLabel -ExtraArgs "--maxEntries $MorphoEntries"

Write-Host "Done."
Write-Host "Output folder (tous types premium pour ce cycle) :"
Write-Host "  target/packs/$PremiumLabel  (LONGOKA_CADENCE=$cadence dans meta.exportCadence)"
