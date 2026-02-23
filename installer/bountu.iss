; Bountu Windows Installer (Inno Setup 6)
#define MyAppName "Bountu"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "SN-Mrdatobg"
#define MyAppURL "https://github.com/snmrdatobgstudioz9918-creator"
; Change this to your built client folder
#define SourceDir "..\\dist\\win-unpacked"

[Setup]
AppId={{7E7B4F6B-FD0C-44F0-9F73-6C0B7B3F2C10}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
DefaultDirName={autopf}\\Bountu
DisableDirPage=no
DefaultGroupName=Bountu
OutputDir=Output
OutputBaseFilename=BountuSetup
Compression=lzma
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:"; Flags: unchecked

[Files]
Source: "{#SourceDir}\\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion
; Include update checker script
Source: "scripts\\check_update.ps1"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\\Bountu"; Filename: "{app}\\Bountu.exe"; WorkingDir: "{app}"
Name: "{commondesktop}\\Bountu"; Filename: "{app}\\Bountu.exe"; Tasks: desktopicon; WorkingDir: "{app}"

[Run]
; Optionally launch app after install
Filename: "{app}\\Bountu.exe"; Description: "Launch Bountu"; Flags: nowait postinstall skipifsilent

[Registry]
; RunOnce to check updates at first user logon
Root: HKCU; Subkey: "Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce"; ValueType: string; ValueName: "BountuFirstRunUpdate"; ValueData: "powershell -NoProfile -ExecutionPolicy Bypass -File `"{app}\\check_update.ps1`""; Flags: uninsdeletevalue

[Code]
// Enforce install to local fixed drive (no removable/network)
function IsFixedDrive(Path: string): Boolean;
var
  Drive: string;
begin
  Drive := AddBackslash(ExtractFileDrive(Path));
  Result := (GetDriveType(Drive) = DRIVE_FIXED);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = wpSelectDir then
  begin
    if not IsFixedDrive(WizardForm.DirEdit.Text) then
    begin
      MsgBox('Please choose a local hard drive (fixed disk) for installation.', mbError, MB_OK);
      Result := False;
    end;
  end;
end;
