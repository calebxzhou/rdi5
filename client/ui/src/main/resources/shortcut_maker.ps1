$desktop = [Environment]::GetFolderPath('Desktop')
$path = Join-Path $desktop 'RDI.lnk'
$ws = New-Object -ComObject WScript.Shell
$lnk = $ws.CreateShortcut($path)
$lnk.TargetPath = '__JAVAW__'
$lnk.Arguments = '__ARGS__'
$lnk.WorkingDirectory = '__WORKDIR__'
$lnk.IconLocation = '__ICON__'
$lnk.Save()
$bytes = [System.IO.File]::ReadAllBytes($path)
$bytes[0x15] = $bytes[0x15] -bor 0x20
[System.IO.File]::WriteAllBytes($path, $bytes)
