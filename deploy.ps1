<#
.SYNOPSIS
    One-click build + hot-inject for the Game Jam NeoForge mod.

.DESCRIPTION
    1. Ensures JAVA_HOME points at a valid JDK 21 (falls back to the team's
       standard Temurin/JBR install path).
    2. Runs .\gradlew.bat build; aborts with the build exit code on failure.
    3. Refuses to deploy while a Minecraft client for the target instance is
       still running. Hot-overwriting the mod jar under a live client makes
       NeoForge fail to resolve new/changed classes and crash with
       java.lang.NoClassDefFoundError (e.g. ModTags$Blocks). Use -Force to
       bypass this guard explicitly.
    4. Picks the newest deployable jar in build\libs\. The archive is named
       "<mod_id>-<minecraft_version>-<mod_version>.jar" (see build.gradle
       "base { archivesName = \"${mod_id}-${minecraft_version}\" }"), i.e.
       weather_realm-1.21.1-1.0.0.jar today; -sources/-javadoc/-dev jars are
       excluded.
    5. Copies it (overwriting) into the PCL client version's mods folder and
       removes stale weather_realm-*.jar leftovers (plus legacy examplemod-*.jar
       from the pre-rename era).
    6. Reminds you to fully restart the client: new registrations/classes are
       not hot-reloadable (see AGENTS.md section 6).

.EXAMPLE
    .\deploy.ps1

.EXAMPLE
    .\deploy.ps1 -TargetModsDir "D:\other\.minecraft\versions\X\mods"

.EXAMPLE
    .\deploy.ps1 -Force   # deploy even if a client appears to be running
#>
[CmdletBinding()]
param(
    [string]$TargetModsDir = "C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\",
    [string]$PreferredJavaHome = "C:\Program Files\Microsoft\jdk-21.0.7.6-hotspot",
    [switch]$Force
)

$RepoRoot = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RepoRoot)) { $RepoRoot = (Get-Location).Path }
Set-Location $RepoRoot

function Test-Jdk21 {
    param([string]$JavaHomePath)
    if ([string]::IsNullOrWhiteSpace($JavaHomePath)) { return $false }
    $javaExe = Join-Path $JavaHomePath "bin\java.exe"
    if (-not (Test-Path -LiteralPath $javaExe)) { return $false }
    $verLine = (& $javaExe -version 2>&1 | Select-Object -First 1) -as [string]
    if ([string]::IsNullOrWhiteSpace($verLine)) { return $false }
    return ($verLine -match 'version "21\.')
}

# --- 1. Ensure JAVA_HOME points at a valid JDK 21 ----------------------------
if (Test-Jdk21 $env:JAVA_HOME) {
    Write-Host "[deploy] JAVA_HOME OK: $env:JAVA_HOME"
} elseif (Test-Jdk21 $PreferredJavaHome) {
    Write-Host "[deploy] JAVA_HOME '$env:JAVA_HOME' is not JDK 21; switching to $PreferredJavaHome" -ForegroundColor Yellow
    $env:JAVA_HOME = $PreferredJavaHome
} else {
    Write-Host "[deploy] ERROR: no valid JDK 21 found (checked JAVA_HOME and '$PreferredJavaHome')." -ForegroundColor Red
    exit 1
}

# --- 2. Build ----------------------------------------------------------------
Write-Host "[deploy] Building: .\gradlew.bat build"
& (Join-Path $RepoRoot "gradlew.bat") build
$buildExit = $LASTEXITCODE
if ($buildExit -ne 0) {
    Write-Host "[deploy] BUILD FAILED (exit code $buildExit). Aborting; nothing deployed." -ForegroundColor Red
    exit $buildExit
}

# --- 3. Refuse to hot-swap the jar while the client is running ---------------
# Overwriting the mod jar under a running client makes NeoForge unable to
# resolve newly added / changed classes (java.lang.NoClassDefFoundError). The
# identity of the target instance is derived from $TargetModsDir instead of
# being hard-coded: <mods> -> <version dir> -> <.../versions> -> <.minecraft>.
$targetTrimmed = $TargetModsDir.TrimEnd('\', '/')
$versionDir    = Split-Path -Path $targetTrimmed -Parent                       # ...\versions\<version>
$gameIdentity  = Split-Path -Path $versionDir -Leaf                            # e.g. 1.21.1-NeoForge_21.1.252
$gameRoot      = Split-Path -Path (Split-Path -Path $versionDir -Parent) -Parent  # ...\.minecraft

function Get-JavaProcesses {
    # Prefer CIM so we can read the command line; fall back to Get-Process
    # (which cannot expose CommandLine) when CIM is unavailable.
    try {
        $cim = Get-CimInstance Win32_Process -Filter "Name = 'javaw.exe' OR Name = 'java.exe'" -ErrorAction Stop
        return $cim | ForEach-Object {
            [pscustomobject]@{
                ProcessId      = [int]$_.ProcessId
                Name           = $_.Name
                CommandLine    = $_.CommandLine
                HasCommandLine = $true
            }
        }
    } catch {
        Write-Host "[deploy] WARN: Get-CimInstance failed ($($_.Exception.Message)); falling back to Get-Process (command line unavailable)." -ForegroundColor Yellow
        return Get-Process -Name 'javaw','java' -ErrorAction SilentlyContinue | ForEach-Object {
            [pscustomobject]@{
                ProcessId      = $_.Id
                Name           = "$($_.ProcessName).exe"
                CommandLine    = $null
                HasCommandLine = $false
            }
        }
    }
}

function Test-CommandLineMatchesGame {
    param([string]$CommandLine, [string[]]$Markers)
    if ([string]::IsNullOrWhiteSpace($CommandLine)) { return $false }
    foreach ($marker in $Markers) {
        if ([string]::IsNullOrWhiteSpace($marker)) { continue }
        if ($CommandLine.IndexOf($marker, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) { return $true }
    }
    return $false
}

if ($Force) {
    Write-Host "[deploy] WARN: -Force 已指定,跳过运行中客户端检测。" -ForegroundColor Yellow
} else {
    $markers   = @($gameIdentity, $gameRoot, '.minecraft')
    $javaProcs = @(Get-JavaProcesses)
    $blocked   = @()
    $warned    = @()
    foreach ($p in $javaProcs) {
        if ($p.HasCommandLine -and (Test-CommandLineMatchesGame -CommandLine $p.CommandLine -Markers $markers)) {
            $blocked += $p
        } else {
            $warned += $p
        }
    }
    if ($blocked.Count -gt 0) {
        Write-Host ""
        Write-Host "[deploy] ERROR: 检测到 Minecraft 客户端仍在运行,已中止部署(拒绝热覆盖 jar)。" -ForegroundColor Red
        Write-Host "         热覆盖运行中的模组 jar 会导致 NeoForge 无法解析新增/变更的类," -ForegroundColor Red
        Write-Host "         触发 java.lang.NoClassDefFoundError(如 com/example/weather_realm/ModTags`$Blocks)。" -ForegroundColor Red
        foreach ($p in $blocked) {
            $cmd = [string]$p.CommandLine
            if ($cmd.Length -gt 300) { $cmd = $cmd.Substring(0, 300) + ' ...' }
            Write-Host ("         - PID {0} ({1}): {2}" -f $p.ProcessId, $p.Name, $cmd) -ForegroundColor Red
        }
        Write-Host "         请【完全退出 Minecraft】后重新执行 .\deploy.ps1;" -ForegroundColor Yellow
        Write-Host "         确需强制部署时可显式使用 .\deploy.ps1 -Force(仍可能导致上述崩溃)。" -ForegroundColor Yellow
        Write-Host ""
        exit 1
    }
    foreach ($p in $warned) {
        Write-Host ("[deploy] WARN: 检测到 Java 进程 PID {0} ({1});如正在运行 Minecraft 请先完全退出。" -f $p.ProcessId, $p.Name) -ForegroundColor Yellow
    }
}

# --- 4. Locate the newest deployable jar ------------------------------------
$libsDir = Join-Path $RepoRoot "build\libs"
if (-not (Test-Path -LiteralPath $libsDir)) {
    Write-Host "[deploy] ERROR: '$libsDir' not found after build." -ForegroundColor Red
    exit 1
}
function Test-Deployable {
    param([System.IO.FileInfo]$Jar)
    return -not ($Jar.Name -like "*-sources.jar" -or
                $Jar.Name -like "*-javadoc.jar" -or
                $Jar.Name -like "*-dev.jar")
}
$jar = Get-ChildItem -LiteralPath $libsDir -Filter "weather_realm-*.jar" -File |
    Where-Object { Test-Deployable $_ } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $jar) {
    $jar = Get-ChildItem -LiteralPath $libsDir -Filter "*.jar" -File |
        Where-Object { Test-Deployable $_ } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}
if (-not $jar) {
    Write-Host "[deploy] ERROR: no deployable jar found in '$libsDir'." -ForegroundColor Red
    exit 1
}

# --- 5. Ensure the target mods directory exists -----------------------------
if (-not (Test-Path -LiteralPath $TargetModsDir)) {
    New-Item -ItemType Directory -Path $TargetModsDir -Force -ErrorAction Stop | Out-Null
    Write-Host "[deploy] Created target dir: $TargetModsDir"
}

# --- 6. Remove stale jars from previous naming / versions -------------------
# Keeps the current weather_realm-*.jar; also clears legacy examplemod-*.jar
# left over from the pre-rename era. Selection above is unchanged.
Get-ChildItem -LiteralPath $TargetModsDir -Filter "*.jar" -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like "examplemod-*.jar" -or ($_.Name -like "weather_realm-*.jar" -and $_.Name -ne $jar.Name) } |
    ForEach-Object {
        Write-Host "[deploy] Removing stale jar: $($_.Name)" -ForegroundColor Yellow
        Remove-Item -LiteralPath $_.FullName -Force
    }

# --- 7. Copy the jar (overwrite previous version) ---------------------------
$dest = Join-Path $TargetModsDir $jar.Name
Copy-Item -LiteralPath $jar.FullName -Destination $dest -Force -ErrorAction Stop
$deployed = Get-Item -LiteralPath $dest

# --- 8. Status ---------------------------------------------------------------
Write-Host ""
Write-Host "[deploy] ===== DEPLOY OK =====" -ForegroundColor Green
Write-Host ("[deploy] jar name   : {0}" -f $deployed.Name)
Write-Host ("[deploy] size       : {0} bytes ({1:N1} KB)" -f $deployed.Length, ($deployed.Length / 1KB))
Write-Host ("[deploy] last write : {0}" -f $deployed.LastWriteTime)
Write-Host ("[deploy] target dir : {0}" -f $TargetModsDir)
Write-Host "[deploy] NOTE       : 已覆盖 mods 目录里的 jar;必须【完全重启客户端】才会生效(新增注册项/新类不可热更,见 AGENTS.md 第 6 节)。" -ForegroundColor Yellow
exit 0
