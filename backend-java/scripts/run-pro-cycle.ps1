param(
  [string]$CycleId = (Get-Date -Format "yyyyMMdd"),
  [string]$MeaningLang = "fr",
  [string]$LanguageMode = "kg",
  [int]$CountPerPack = 1,
  [int]$WordSearchRows = 20,
  [int]$WordSearchCols = 20,
  [int]$CrosswordRows = 19,
  [int]$CrosswordCols = 19,
  [int]$ArrowwordRows = 17,
  [int]$ArrowwordCols = 17,
  [int]$GridEntries = 16,
  [int]$MorphoEntries = 12,
  [int]$TypesPerCycle = 2,
  [int]$ProfilesPerType = 2,
  [string[]]$TypePool = @("wordsearch", "crossword", "memory", "domino"),
  [ValidateSet("full", "base")]
  [string]$ArrowwordProfileSet = "full"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Canonical pro entrypoint. Delegates to the historical script for backward compatibility.
$sharedScript = Join-Path $PSScriptRoot "run-premium-cycle.ps1"

& $sharedScript `
  -CycleId $CycleId `
  -MeaningLang $MeaningLang `
  -LanguageMode $LanguageMode `
  -CountPerPack $CountPerPack `
  -WordSearchRows $WordSearchRows `
  -WordSearchCols $WordSearchCols `
  -CrosswordRows $CrosswordRows `
  -CrosswordCols $CrosswordCols `
  -ArrowwordRows $ArrowwordRows `
  -ArrowwordCols $ArrowwordCols `
  -GridEntries $GridEntries `
  -MorphoEntries $MorphoEntries `
  -TypesPerCycle $TypesPerCycle `
  -ProfilesPerType $ProfilesPerType `
  -TypePool $TypePool `
  -ArrowwordProfileSet $ArrowwordProfileSet
