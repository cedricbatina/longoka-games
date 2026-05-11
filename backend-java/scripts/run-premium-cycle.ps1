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
  [int]$CountPerPack = 48,
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

function Get-LingalaUnlockDate {
  if ($env:LONGOKA_LINGALA_UNLOCK_DATE -and $env:LONGOKA_LINGALA_UNLOCK_DATE.Trim()) {
    return $env:LONGOKA_LINGALA_UNLOCK_DATE.Trim()
  }
  return "20260619"
}

function Assert-LanguageReleaseWindow {
  param(
    [string]$Mode,
    [string]$Value
  )

  if ([string]::IsNullOrWhiteSpace($Mode)) {
    $normalized = "kg"
  }
  else {
    $normalized = $Mode.Trim().ToLowerInvariant()
  }
  if ($normalized -ne "ln" -and $normalized -ne "lingala") {
    return
  }

  $unlockRaw = Get-LingalaUnlockDate
  if (-not ($unlockRaw -match '^[0-9]{8}$')) {
    throw "LONGOKA_LINGALA_UNLOCK_DATE doit être au format yyyyMMdd."
  }

  $currentRaw = if ([string]::IsNullOrWhiteSpace($Value)) { (Get-Date -Format "yyyyMMdd") } else { $Value.Trim() }
  $currentToken = if ($currentRaw.Length -ge 8) { $currentRaw.Substring(0, 8) } else { $currentRaw }
  if (-not ($currentToken -match '^[0-9]{8}$')) {
    throw "CycleId doit être au format yyyyMMdd pour appliquer la fenêtre Lingala."
  }

  $unlockDate = [datetime]::ParseExact($unlockRaw, "yyyyMMdd", [System.Globalization.CultureInfo]::InvariantCulture)
  $currentDate = [datetime]::ParseExact($currentToken, "yyyyMMdd", [System.Globalization.CultureInfo]::InvariantCulture)
  if ($currentDate -lt $unlockDate) {
    throw "La production hebdomadaire Lingala est bloquée jusqu'au $($unlockDate.ToString('yyyy-MM-dd')). Pour l'instant, la rotation reste sur le kikongo uniquement."
  }
}

Assert-LanguageReleaseWindow -Mode $LanguageMode -Value $CycleId

$cadence = "weekly"
if ($env:LONGOKA_CADENCE) {
  $c = $env:LONGOKA_CADENCE.Trim().ToLowerInvariant()
  if ($c -eq "biweekly") {
    Write-Host "LONGOKA_CADENCE=biweekly -> normalisé vers weekly (meta.exportCadence)."
  }
}

# Même logique que le standard hebdo : un libellé de semaine unique pour tout le lot.
# Les anciens packs du même label sont conservés; le script n'efface rien.
$PremiumLabel = "$CycleId-semaine-premium"

$ProfileRotationSets = @{
  wordsearch = @(
    @("class-1-singular", "mixed-verbs-nouns-singular"),
    @("nouns-singular", "verbs-only"),
    @("class-lu-family-singular", "class-mu-family-singular"),
    @("class-bu-ku-family-singular", "mixed-verbs-nouns-singular")
  )
  crossword = @(
    @("nouns-singular", "verbs-only"),
    @("mixed-verbs-nouns-singular", "nouns-singular"),
    @("class-1-singular", "nouns-singular"),
    @("class-lu-family-singular", "verbs-only")
  )
  memory = @(
    @("class-lu-family-singular", "class-mu-family-singular"),
    @("mixed-verbs-nouns-singular", "class-1-singular"),
    @("verbs-only", "nouns-singular"),
    @("class-bu-ku-family-singular", "class-1-singular")
  )
  domino = @(
    @("class-1-singular", "class-lu-family-singular"),
    @("class-mu-family-singular", "class-bu-ku-family-singular"),
    @("radical-sa-verbs", "class-1-singular")
  )
}

$normalizedLanguages = @(
  @($LanguageMode -split ',') | ForEach-Object { $_.Trim().ToLowerInvariant() } | Where-Object { $_ }
)
$isKikongoOnlyCycle = $normalizedLanguages.Count -eq 1 -and $normalizedLanguages[0] -eq "kg"

if ($isKikongoOnlyCycle) {
  $ProfileRotationSets.wordsearch = @(
    @("class-1-singular", "mixed-verbs-nouns-singular"),
    @("nouns-singular", "verbs-only"),
    @("class-lu-family-singular", "class-mu-family-singular"),
    @("class-bu-ku-family-singular", "class-ki-fi-family-singular")
  )
  $ProfileRotationSets.crossword = @(
    @("nouns-singular", "verbs-only"),
    @("mixed-verbs-nouns-singular", "nouns-singular"),
    @("class-1-singular", "nouns-singular"),
    @("class-lu-family-singular", "class-ki-fi-family-singular")
  )
  $ProfileRotationSets.memory = @(
    @("class-lu-family-singular", "class-mu-family-singular"),
    @("mixed-verbs-nouns-singular", "class-1-singular"),
    @("verbs-only", "nouns-singular"),
    @("class-bu-ku-family-singular", "class-ki-fi-family-singular")
  )
  $ProfileRotationSets.domino = @(
    @("class-1-singular", "class-lu-family-singular"),
    @("class-mu-family-singular", "class-ki-fi-family-singular"),
    @("radical-sa-verbs", "class-1-singular")
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

function Get-RotationWindow {
  param(
    [string[]]$Items,
    [int]$Take,
    [int]$Offset
  )

  if (-not $Items -or $Items.Count -eq 0) {
    return @()
  }

  $limit = [Math]::Min([Math]::Max($Take, 1), $Items.Count)
  $start = 0
  if ($Items.Count -gt 0) {
    $start = (($Offset % $Items.Count) + $Items.Count) % $Items.Count
  }

  $selected = [System.Collections.Generic.List[string]]::new()
  for ($i = 0; $i -lt $limit; $i++) {
    $selected.Add($Items[($start + $i) % $Items.Count])
  }
  return $selected.ToArray()
}

function Get-ProfileSelection {
  param(
    [string]$Type,
    [int]$Offset,
    [int]$Take
  )

  if (-not $ProfileRotationSets.ContainsKey($Type)) {
    return @()
  }

  $sets = $ProfileRotationSets[$Type]
  if (-not $sets -or $sets.Count -eq 0) {
    return @()
  }

  $index = (($Offset % $sets.Count) + $sets.Count) % $sets.Count
  $selected = $sets[$index]
  if (-not $selected) {
    return @()
  }

  return $selected | Select-Object -First ([Math]::Max($Take, 1))
}

$arrowwordExtra = "--rows $ArrowwordRows --cols $ArrowwordCols --maxEntries $GridEntries"
if ($ArrowwordProfileSet -eq "base") {
  $arrowwordExtra = "$arrowwordExtra --profileSet base"
}

function Invoke-PremiumGen {
  param(
    [string]$Type,
    [string]$Label,
    [string]$ExtraArgs = "",
    [string[]]$Profiles = @()
  )

  $profileArgs = ""
  if ($Profiles -and $Profiles.Count -gt 0) {
    $profileArgs = "--profiles " + ($Profiles -join ",")
  }

  $execArgs = "--cadence $cadence --count $CountPerPack --meaningLang $MeaningLang --language $LanguageMode --type $Type --label $Label --tier premium --difficulty expert $ExtraArgs $profileArgs".Trim()

  Write-Host "Running premium generation: cadence=$cadence type=$Type label=$Label languages=$LanguageMode"
  if ($Profiles -and $Profiles.Count -gt 0) {
    Write-Host "  profiles=$($Profiles -join ', ')"
  }
  & mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" "-Dexec.args=$execArgs"

  if ($LASTEXITCODE -ne 0) {
    throw "Premium generation failed for type=$Type label=$Label"
  }
}

$rotationIndex = Get-RotationIndex -Value $CycleId
$selectedTypes = Get-RotationWindow -Items $TypePool -Take $TypesPerCycle -Offset $rotationIndex

Write-Host "Weekly premium rotation: weekIndex=$rotationIndex selectedTypes=$($selectedTypes -join ', ')"

foreach ($type in $selectedTypes) {
  $profiles = Get-ProfileSelection -Type $type -Offset ($rotationIndex + [array]::IndexOf($selectedTypes, $type)) -Take $ProfilesPerType

  switch ($type) {
    "wordsearch" {
      Invoke-PremiumGen -Type $type -Label $PremiumLabel -ExtraArgs "--rows $WordSearchRows --cols $WordSearchCols --maxEntries $GridEntries" -Profiles $profiles
    }
    "crossword" {
      Invoke-PremiumGen -Type $type -Label $PremiumLabel -ExtraArgs "--rows $CrosswordRows --cols $CrosswordCols --maxEntries $GridEntries" -Profiles $profiles
    }
    "arrowword" {
      Invoke-PremiumGen -Type $type -Label $PremiumLabel -ExtraArgs $arrowwordExtra -Profiles $profiles
    }
    "domino" {
      Invoke-PremiumGen -Type $type -Label $PremiumLabel -ExtraArgs "--maxEntries $MorphoEntries" -Profiles $profiles
    }
    "memory" {
      Invoke-PremiumGen -Type $type -Label $PremiumLabel -ExtraArgs "--maxEntries $MorphoEntries" -Profiles $profiles
    }
    "scrabble" {
      Invoke-PremiumGen -Type $type -Label $PremiumLabel -ExtraArgs "--maxEntries $MorphoEntries" -Profiles $profiles
    }
    "anagram" {
      Invoke-PremiumGen -Type $type -Label $PremiumLabel -ExtraArgs "--maxEntries $MorphoEntries" -Profiles $profiles
    }
    default {
      throw "Unsupported premium type in TypePool: $type"
    }
  }
}

Write-Host "Done."
Write-Host "Output folder (tous types premium pour ce cycle) :"
Write-Host "  target/packs/$PremiumLabel  (LONGOKA_CADENCE=$cadence dans meta.exportCadence)"
