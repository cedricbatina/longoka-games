param(
  [string]$CycleId = (Get-Date -Format "yyyyMMdd"),
  [string]$MeaningLang = "fr",
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

$sharedScript = Join-Path $PSScriptRoot "run-premium-cycle.ps1"

& $sharedScript `
  -CycleId $CycleId `
  -MeaningLang $MeaningLang `
  -LanguageMode "ln" `
  -CountPerPack $CountPerPack `
  -WordSearchRows $WordSearchRows `
  -WordSearchCols $WordSearchCols `
  -CrosswordRows $CrosswordRows `
  -CrosswordCols $CrosswordCols `
  -ArrowwordRows $ArrowwordRows `
  -ArrowwordCols $ArrowwordCols `
  -GridEntries $GridEntries `
  -MorphoEntries $MorphoEntries `
  -ArrowwordProfileSet $ArrowwordProfileSet
