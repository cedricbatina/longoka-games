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

$ProfileRotationSets = @{
  wordsearch = @(
    "class-1-singular",
    "class-lu-tu-lu-zi-lu-ma-singular",
    "class-mu-ba-mu-mi-singular",
    "class-bu-ma-ku-ma-singular",
    "mixed-verbs-nouns-singular"
  )
  crossword = @(
    "class-1-singular",
    "class-lu-tu-lu-zi-lu-ma-singular",
    "class-mu-ba-mu-mi-singular",
    "class-bu-ma-ku-ma-singular",
    "mixed-verbs-nouns-singular"
  )
  anagram = @(
    "class-1-singular",
    "class-lu-tu-lu-zi-lu-ma-singular",
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

function Invoke-WeeklyGen {
  param(
    [string]$Type,
    [string]$Label,
    [string]$ProfileId = ""
  )

  $profileArgs = ""
  if (-not [string]::IsNullOrWhiteSpace($ProfileId)) {
    $profileArgs = "--profiles $ProfileId"
  }

  $execArgs = "--cadence weekly --count $Count --rows $Rows --cols $Cols --maxEntries $MaxEntries --meaningLang $MeaningLang --type $Type --label $Label $profileArgs".Trim()
  Write-Host "Running: cadence=weekly type=$Type label=$Label count=$Count profile=$ProfileId"

  & mvn -q -DskipTests exec:java "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" "-Dexec.args=$execArgs"
  if ($LASTEXITCODE -ne 0) {
    throw "Generation failed for type=$Type label=$Label"
  }
}

$weekLabel = "$CycleId-semaine-1"
$rotationIndex = Get-RotationIndex -Value $CycleId
$wordsearchProfile = Get-ProfileForType -Type "wordsearch" -Offset $rotationIndex
$crosswordProfile = Get-ProfileForType -Type "crossword" -Offset $rotationIndex
$anagramProfile = Get-ProfileForType -Type "anagram" -Offset $rotationIndex

Write-Host "Weekly rotation: weekIndex=$rotationIndex wordsearchProfile=$wordsearchProfile crosswordProfile=$crosswordProfile"

# Hebdo: un seul dossier/label
Invoke-WeeklyGen -Type "wordsearch" -Label $weekLabel -ProfileId $wordsearchProfile
Invoke-WeeklyGen -Type "crossword" -Label $weekLabel -ProfileId $crosswordProfile

if ($IncludeMorphoPacks) {
  Write-Host "Weekly morpho rotation: anagramProfile=$anagramProfile"
  Invoke-WeeklyGen -Type "anagram" -Label "$CycleId-morpho-anagram" -ProfileId $anagramProfile
}

Write-Host "Done."
Write-Host "Week folder: target/packs/$weekLabel"
if ($IncludeMorphoPacks) {
  Write-Host "Morpho anagram folder: target/packs/$CycleId-morpho-anagram"
}

