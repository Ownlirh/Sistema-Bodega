# ============================================================================
# Instala las herramientas que faltan.
#
# Uso:   .\scripts\instalar.ps1
#
# IMPORTANTE: al terminar hay que CERRAR Y ABRIR la terminal. Windows no
# refresca el PATH en ventanas que ya estaban abiertas.
#
# Probado en Windows 11 con winget 1.29. Maven NO esta en winget, asi que
# se descarga de Apache y se extrae en %USERPROFILE%\tools.
# ============================================================================

$ErrorActionPreference = "Continue"

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Write-Host "  No tienes winget. Instala 'Instalador de aplicacion' desde la Microsoft Store." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "  Instalando lo que falte..." -ForegroundColor Cyan
Write-Host ""

# ---------------------------------------------------------------- JDK 17 ---
if (Get-Command java -ErrorAction SilentlyContinue) {
    Write-Host "  [ya esta]  JDK" -ForegroundColor DarkGray
} else {
    Write-Host "  [instalando]  JDK 17 (Temurin), ~180 MB ..." -ForegroundColor Yellow
    winget install --id EclipseAdoptium.Temurin.17.JDK -e --silent --accept-package-agreements --accept-source-agreements
}

# JAVA_HOME lo necesita Maven
$jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
       Where-Object { $_.Name -like 'jdk-17*' } | Select-Object -First 1
if ($jdk) {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $jdk.FullName, "User")
    Write-Host "  [listo]    JAVA_HOME = $($jdk.FullName)" -ForegroundColor Green
}

# ----------------------------------------------------------------- Maven ---
# Apache Maven no existe como paquete winget: se baja el zip oficial.
$mavenVer  = "3.9.9"
$tools     = "$env:USERPROFILE\tools"
$mavenHome = "$tools\apache-maven-$mavenVer"

if (Test-Path "$mavenHome\bin\mvn.cmd") {
    Write-Host "  [ya esta]  Maven" -ForegroundColor DarkGray
} else {
    Write-Host "  [instalando]  Maven $mavenVer, ~9 MB ..." -ForegroundColor Yellow
    if (-not (Test-Path $tools)) { New-Item -ItemType Directory -Path $tools -Force | Out-Null }
    $zip = "$env:TEMP\apache-maven-$mavenVer-bin.zip"
    $url = "https://archive.apache.org/dist/maven/maven-3/$mavenVer/binaries/apache-maven-$mavenVer-bin.zip"
    try {
        Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
        Expand-Archive -Path $zip -DestinationPath $tools -Force
        Remove-Item $zip -Force -ErrorAction SilentlyContinue
        Write-Host "  [listo]    Maven en $mavenHome" -ForegroundColor Green
    } catch {
        Write-Host "  [fallo]    Maven: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Maven al PATH del usuario
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($userPath -notlike "*apache-maven*") {
    [Environment]::SetEnvironmentVariable("Path", ($userPath + ";" + "$mavenHome\bin"), "User")
    Write-Host "  [listo]    Maven agregado al PATH" -ForegroundColor Green
}

# ----------------------------------------------------------------- MySQL ---
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
if (Test-Path "$mysqlBin\mysqld.exe") {
    Write-Host "  [ya esta]  MySQL" -ForegroundColor DarkGray
} else {
    Write-Host "  [instalando]  MySQL 8.4, ~450 MB ..." -ForegroundColor Yellow
    winget install --id Oracle.MySQL -e --silent --accept-package-agreements --accept-source-agreements
}

# El MSI solo copia archivos: el servidor queda SIN inicializar.
$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ((Test-Path $mysqlBin) -and ($userPath -notlike "*MySQL Server*")) {
    [Environment]::SetEnvironmentVariable("Path", ($userPath + ";" + $mysqlBin), "User")
    Write-Host "  [listo]    MySQL agregado al PATH" -ForegroundColor Green
}

# ------------------------------------------------------------- VS Code -----
if (Get-Command code -ErrorAction SilentlyContinue) {
    Write-Host ""
    Write-Host "  Agregando extensiones de Java a VS Code..." -ForegroundColor Cyan
    code --install-extension vscjava.vscode-java-pack --force | Out-Null
    code --install-extension vmware.vscode-boot-dev-pack --force | Out-Null
    Write-Host "  [listo]    extensiones de VS Code" -ForegroundColor Green
}

Write-Host ""
Write-Host "  ----------------------------------------------------------------"
Write-Host "  CIERRA ESTA TERMINAL Y ABRE UNA NUEVA." -ForegroundColor Yellow
Write-Host "  Windows no refresca el PATH en ventanas ya abiertas."
Write-Host "  ----------------------------------------------------------------"
Write-Host ""
Write-Host "  Despues, en la terminal nueva:"
Write-Host ""
Write-Host "      .\scripts\mysql-iniciar.ps1   (arranca el servidor MySQL)"
Write-Host "      .\scripts\setup-bd.ps1        (crea la base de datos)"
Write-Host "      .\scripts\iniciar.ps1         (levanta la aplicacion)"
Write-Host ""
