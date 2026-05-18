; Le Festin — Inno Setup Script
; Tool : Inno Setup 6.x  https://jrsoftware.org/isdl.php
; Build: open this file in Inno Setup Compiler, then press Ctrl+F9
;        Output will be written to installers/output/

#define AppName      "Le Festin"
#define AppVersion   "1.0"
#define AppPublisher "Team Chef"
#define JarFile      "le-festin.jar"
#define Launcher     "LeFestin.bat"

; ── Setup metadata ────────────────────────────────────────────────────────────

[Setup]
AppId={{F3A2B1C0-D4E5-6F78-9ABC-DEF012345678}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
OutputDir=output
OutputBaseFilename=le-festin-{#AppVersion}-setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
MinVersion=10.0

; ── Language ──────────────────────────────────────────────────────────────────

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

; ── Optional tasks ────────────────────────────────────────────────────────────

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

; ── Files ─────────────────────────────────────────────────────────────────────
; SQL files and config.properties are handled entirely at runtime by the
; in-app Setup Wizard — only the JAR and launcher need to be installed.

[Files]
Source: "{#JarFile}";  DestDir: "{app}"; Flags: ignoreversion
Source: "{#Launcher}"; DestDir: "{app}"; Flags: ignoreversion

; ── Shortcuts ─────────────────────────────────────────────────────────────────

[Icons]
Name: "{group}\{#AppName}";           Filename: "{app}\{#Launcher}"
Name: "{group}\Uninstall {#AppName}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#AppName}";   Filename: "{app}\{#Launcher}"; Tasks: desktopicon

; ── Post-install launch ───────────────────────────────────────────────────────

[Run]
Filename: "{app}\{#Launcher}"; Description: "{cm:LaunchProgram,{#AppName}}"; \
    Flags: nowait postinstall skipifsilent shellexec

; ── Java 17 check ─────────────────────────────────────────────────────────────

[Code]
function InitializeSetup(): Boolean;
var
  JavaVer: String;
  Found:   Boolean;
begin
  Found := RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', JavaVer)
        or RegQueryStringValue(HKLM32, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', JavaVer);

  if not Found then
    Result := MsgBox(
      'Java 17 or higher is required but was not detected on this machine.' + #13#10 +
      'Download it from: https://adoptium.net' + #13#10#13#10 +
      'Continue the installation anyway?',
      mbConfirmation, MB_YESNO) = IDYES
  else
    Result := True;
end;
