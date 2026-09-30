<#
.SYNOPSIS
    One-click build + hot-inject for the Game Jam NeoForge mod.

.DESCRIPTION
    1. Ensures JAVA_HOME points at a valid JDK 21 (falls back to the team's
       standard Temurin/JBR install path).
    2. Runs .\gradlew.bat build; aborts with the build exit code on failure.
    3. Picks the newest deployable jar in build\libs\ (weather_realm-*.jar,
       excluding -sources/-javadoc/-dev jars).
    4. Copies it (overwriting) into the PCL client version's mods folder and
       removes stale examplemod-*.jar / weather_realm-*.jar leftovers.

.EXAMPLE
    .\deploy.ps1

.EXAMPLE
    .\deploy.ps1 -TargetModsDir "D:\other\.minecraft\versions\X\mods"
#>
[CmdletBinding()]
param(
    [string]$TargetModsDir = "C:\Users\31087\Desktop\mc\.minecraft\versions\1.21.1-NeoForge_21.1.252\mods\",
    [string]$PreferredJavaHome = "C:\Program Files\Microsoft\jdk-21.0.7.6-hotspot"
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

# --- 3. Locate the newest deployable jar ------------------------------------
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

# --- 4. Ensure the target mods directory exists -----------------------------
if (-not (Test-Path -LiteralPath $TargetModsDir)) {
    New-Item -ItemType Directory -Path $TargetModsDir -Force -ErrorAction Stop | Out-Null
    Write-Host "[deploy] Created target dir: $TargetModsDir"
}

# --- 5. Remove stale jars from previous naming / versions -------------------
Get-ChildItem -LiteralPath $TargetModsDir -Filter "*.jar" -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like "examplemod-*.jar" -or ($_.Name -like "weather_realm-*.jar" -and $_.Name -ne $jar.Name) } |
    ForEach-Object {
        Write-Host "[deploy] Removing stale jar: $($_.Name)" -ForegroundColor Yellow
        Remove-Item -LiteralPath $_.FullName -Force
    }

# --- 6. Copy the jar (overwrite previous version) ---------------------------
$dest = Join-Path $TargetModsDir $jar.Name
Copy-Item -LiteralPath $jar.FullName -Destination $dest -Force -ErrorAction Stop
$deployed = Get-Item -LiteralPath $dest

# --- 7. Status ---------------------------------------------------------------
Write-Host ""
Write-Host "[deploy] ===== DEPLOY OK =====" -ForegroundColor Green
Write-Host ("[deploy] jar name   : {0}" -f $deployed.Name)
Write-Host ("[deploy] size       : {0} bytes ({1:N1} KB)" -f $deployed.Length, ($deployed.Length / 1KB))
Write-Host ("[deploy] last write : {0}" -f $deployed.LastWriteTime)
Write-Host ("[deploy] target dir : {0}" -f $TargetModsDir)
exit 0
