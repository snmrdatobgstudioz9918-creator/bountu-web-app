$root = 'C:\Users\dato\bountu-packages-global\packages'
if (-not (Test-Path $root)) {
  Write-Host "Repo path not found: $root"
  exit 0
}

Get-ChildItem -Path $root -Recurse -Filter metadata.json | ForEach-Object {
  $p = $_.FullName
  $t = Get-Content -Raw -LiteralPath $p
  if ($t -match '\\"') {
    $u = $t -replace '\\"','"'
    if ($u.StartsWith('"') -and $u.EndsWith('"')) { $u = $u.Substring(1, $u.Length-2) }
    $u = $u -replace '\\/','/'
    $Utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($p, $u, $Utf8NoBom)
    Write-Host "Fixed $p"
  }
}
