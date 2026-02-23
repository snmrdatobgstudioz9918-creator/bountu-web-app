param(
  [string]$Owner = 'snmrdatobgstudioz9918-creator',
  [string]$Repo = 'bountu-desktop',
  [string]$CurrentVersion = '1.0.0'
)

$ErrorActionPreference = 'SilentlyContinue'

function Get-LatestReleaseTag($Owner, $Repo) {
  $url = "https://api.github.com/repos/$Owner/$Repo/releases/latest"
  try {
    $resp = Invoke-WebRequest -UseBasicParsing -Uri $url -Headers @{ 'User-Agent' = 'Bountu-Updater' }
    $json = $resp.Content | ConvertFrom-Json
    return $json.tag_name
  } catch {
    return $null
  }
}

function Compare-SemVer($a, $b) {
  # returns -1 if a<b, 0 if equal, 1 if a>b
  try {
    [version]$va = ($a -replace '[^0-9\.]','')
    [version]$vb = ($b -replace '[^0-9\.]','')
    if ($va -lt $vb) { return -1 }
    if ($va -gt $vb) { return 1 }
    return 0
  } catch {
    return 0
  }
}

$latest = Get-LatestReleaseTag $Owner $Repo
if ($latest) {
  $cmp = Compare-SemVer $CurrentVersion $latest
  if ($cmp -lt 0) {
    $msg = "A new version of Bountu is available: $latest (you have $CurrentVersion). Open download page?"
    $wshell = New-Object -ComObject Wscript.Shell
    $ans = $wshell.Popup($msg, 0, 'Bountu Update', 4 + 32)
    if ($ans -eq 6) {
      Start-Process "https://github.com/$Owner/$Repo/releases/latest"
    }
  }
}
