# ============================================================================
# Levanta la aplicacion.
#
# Uso:   .\scripts\iniciar.ps1                    (root sin clave, local)
#        .\scripts\iniciar.ps1 -Clave miclave     (si tu root tiene clave)
#
# La clave se pasa a Spring como argumento, no como variable de entorno.
# Motivo: en PowerShell, asignar '' a una variable de entorno la BORRA, y
# entonces Spring cae al valor por defecto y el login falla con
# "Access denied ... (using password: YES)". Ya nos paso.
# ============================================================================

param(
    [string]$Usuario = "root",
    [string]$Clave   = ""
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot

# Maven puede estar en el PATH o recien instalado en tools
$mavenLocal = "$env:USERPROFILE\tools\apache-maven-3.9.9\bin"
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    if (Test-Path "$mavenLocal\mvn.cmd") {
        $env:Path = $env:Path + ";" + $mavenLocal
    } else {
        Write-Host ""
        Write-Host "  No encuentro Maven. Corre primero:  .\scripts\instalar.ps1" -ForegroundColor Red
        Write-Host ""
        exit 1
    }
}

if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -like 'jdk-17*' } | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}

# MySQL levantado?
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
$corriendo = (Get-Process mysqld -ErrorAction SilentlyContinue) -or
             (Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'MySQL' -and $_.Status -eq 'Running' })
if (-not $corriendo) {
    Write-Host ""
    Write-Host "  MySQL no esta corriendo. Corre primero:  .\scripts\mysql-iniciar.ps1" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "  Levantando la aplicacion..." -ForegroundColor Cyan
Write-Host ""
Write-Host "  Cuando veas 'Started BodegaApplication', abre:"
Write-Host "      http://localhost:8080/health/db" -ForegroundColor White
Write-Host ""
Write-Host "  Para detenerla: Ctrl+C"
Write-Host ""

$argumentos = "--spring.datasource.username=$Usuario --spring.datasource.password=$Clave"

Push-Location $raiz
try {
    mvn -B spring-boot:run "-Dspring-boot.run.arguments=$argumentos"
} finally {
    Pop-Location
}
