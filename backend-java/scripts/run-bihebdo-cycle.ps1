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

$ProfileRotationSets = @{
  wordsearch = @(
    "class-1-singular",
    "class-lu-family-singular",
    "class-mu-family-singular",
    "class-bu-ku-family-singular",
    "mixed-verbs-nouns-singular"
  )
  crossword = @(
    "class-1-singular",
    "class-lu-family-singular",
    "class-mu-family-singular",
    "class-bu-ku-family-singular",
    "mixed-verbs-nouns-singular"
  )
  anagram = @(
    "class-1-singular",
    "class-lu-family-singular",
    "radical-sa-verbs",
    "mixed-verbs-nouns-singular",
    "verbs-only"
  )
}

function Get-RotationIndex {
  param([string]$Value)

  $raw = if ([string]::IsNullOrWhiteSpace($Value)) { (Get-Date -Format "yyyyMMdd") } else { $Value.Trim() }
  $dateToken = if ($raw.Length -ge 8) { $raw.Substring(0, 8) } else { $raw }
  if ($dateToken -match '^[0-9]{8}$') {
    try {
      $cycleDate = [datetime]::ParseExact($dateToken, "yyyyMMdd", [System.Globalization.CultureInfo]::InvariantCulture)
    }
    catch {
      $cycleDate = Get-Date
    }
  }
  else {
    $cycleDate = Get-Date
  }

  $epoch = [datetime]::ParseExact("20260105", "yyyyMMdd", [System.Globalization.CultureInfo]::InvariantCulture)
  $days = [math]::Floor(($cycleDate.Date - $epoch.Date).TotalDays)
  if ($days -lt 0) {
    $days = 0
  }
  return [int][math]::Floor($days / 7)
}

function Get-ProfileForType {
  param(
    [string]$Type,
    [int]$Offset
  )

  if (-not $ProfileRotationSets.ContainsKey($Type)) {
    return ""
  }

  $profiles = $ProfileRotationSets[$Type]
  if (-not $profiles -or $profiles.Count -eq 0) {
    return ""
  }

  $index = (($Offset % $profiles.Count) + $profiles.Count) % $profiles.Count
  return $profiles[$index]
}

# Planification: ce script n'est pas un scheduler Windows en soi. Pour un intervalle regulier,
# planifiez-le avec le Planificateur de taches (ou CI) en pointant sur ce repo + Maven.
# Pour la gamme "premium" complete (mots fleches, dominos, memory, scrabble, anagrammes...),
# utilisez run-premium-cycle.ps1 ou run-premium-cycle-kg.ps1 / run-premium-cycle-ln.ps1.

function Invoke-BiweeklyGen {
  param(
    [string]$Type,
    [string]$Label,
    [string]$ProfileId = ""
  )

  $profileArgs = ""
  if (-not [string]::IsNullOrWhiteSpace($ProfileId)) {
    $profileArgs = "--profiles $ProfileId"
  }

  $execArgs = "--cadence weekly --count $CountPerWeek --rows $Rows --cols $Cols --maxEntries $MaxEntries --meaningLang $MeaningLang --type $Type --label $Label $profileArgs".Trim()

  Write-Host "Running: type=$Type label=$Label count=$CountPerWeek profile=$ProfileId"
  & mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" "-Dexec.args=$execArgs"

  if ($LASTEXITCODE -ne 0) {
    throw "Generation failed for type=$Type label=$Label"
  }
}

$week1Label = "$CycleId-semaine-1"
$week2Label = "$CycleId-semaine-2"
$rotationIndex = Get-RotationIndex -Value $CycleId
$wordsearchProfile = Get-ProfileForType -Type "wordsearch" -Offset $rotationIndex
$crosswordProfile = Get-ProfileForType -Type "crossword" -Offset $rotationIndex
$anagramProfile = Get-ProfileForType -Type "anagram" -Offset $rotationIndex

Write-Host "Biweekly rotation: cycleIndex=$rotationIndex week1Profile=$wordsearchProfile week2Profile=$crosswordProfile"

Invoke-BiweeklyGen -Type "wordsearch" -Label $week1Label -ProfileId $wordsearchProfile
Invoke-BiweeklyGen -Type "crossword" -Label $week2Label -ProfileId $crosswordProfile

if ($IncludeMorphoPacks) {
  Write-Host "Biweekly morpho rotation: anagramProfile=$anagramProfile"
  Invoke-BiweeklyGen -Type "anagram" -Label "$CycleId-morpho-anagram" -ProfileId $anagramProfile
}

Write-Host "Done."
Write-Host "Week1 folder: target/packs/$week1Label"
Write-Host "Week2 folder: target/packs/$week2Label"
if ($IncludeMorphoPacks) {
  Write-Host "Morpho anagram folder: target/packs/$CycleId-morpho-anagram"
}
