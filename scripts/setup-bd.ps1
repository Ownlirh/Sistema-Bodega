# ============================================================================
# Crea la base de datos y carga los datos de prueba.
#
# Uso:   .\scripts\setup-bd.ps1                  (root sin clave, local)
#        .\scripts\setup-bd.ps1 -ConClave        (si tu root tiene clave)
#
# OJO: BORRA la base bodegadb si ya existe y la recrea desde cero.
# ============================================================================

param(
    [string]$Usuario = "root",
    [switch]$ConClave
)

$ErrorActionPreference = "Stop"

$raiz     = Split-Path -Parent $PSScriptRoot
$script   = Join-Path $raiz "db\01_script_bd.sql"
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'

# mysql.exe puede estar en el PATH o solo en su carpeta de instalacion
$mysql = "mysql"
if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    if (Test-Path "$mysqlBin\mysql.exe") {
        $mysql = "$mysqlBin\mysql.exe"
    } else {
        Write-Host ""
        Write-Host "  No encuentro mysql.exe. Corre primero:  .\scripts\instalar.ps1" -ForegroundColor Red
        Write-Host ""
        exit 1
    }
}

if (-not (Test-Path $script)) {
    Write-Host "  No encuentro el script: $script" -ForegroundColor Red
    exit 1
}

$corriendo = (Get-Process mysqld -ErrorAction SilentlyContinue) -or
             (Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'MySQL' -and $_.Status -eq 'Running' })
if (-not $corriendo) {
    Write-Host ""
    Write-Host "  MySQL no esta corriendo. Corre primero:  .\scripts\mysql-iniciar.ps1" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "  Esto BORRA la base 'bodegadb' si ya existe y la recrea." -ForegroundColor Yellow
$resp = Read-Host "  Continuar? (s/n)"
if ($resp -ne "s") {
    Write-Host "  Cancelado."
    exit 0
}

$demo = Join-Path $raiz "db\02_datos_demo.sql"

Write-Host ""
Write-Host "  [1/2] Creando tablas y catalogos..." -ForegroundColor Cyan

if ($ConClave) {
    Write-Host "  (MySQL te va a pedir la clave de $Usuario)"
    Get-Content $script -Raw -Encoding UTF8 | & $mysql --protocol=tcp -h 127.0.0.1 -u $Usuario -p
} else {
    Get-Content $script -Raw -Encoding UTF8 | & $mysql --protocol=tcp -h 127.0.0.1 -u $Usuario
}

if (($LASTEXITCODE -eq 0) -and (Test-Path $demo)) {
    Write-Host ""
    Write-Host "  [2/2] Generando ventas y compras de demostracion..." -ForegroundColor Cyan
    Write-Host "  (tarda unos segundos: son ~1400 ventas)"
    if ($ConClave) {
        Get-Content $demo -Raw -Encoding UTF8 | & $mysql --protocol=tcp -h 127.0.0.1 -u $Usuario -p
    } else {
        Get-Content $demo -Raw -Encoding UTF8 | & $mysql --protocol=tcp -h 127.0.0.1 -u $Usuario
    }
}

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "  Listo. La base quedo con:" -ForegroundColor Green
    Write-Host "  3 tiendas, 8 empleados, 20 productos, ~1400 ventas y 3 meses de movimiento."
    Write-Host ""
    Write-Host "  Siguiente paso:   .\scripts\iniciar.ps1"
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "  Fallo la carga. Lo mas comun:" -ForegroundColor Red
    Write-Host "    - tu root SI tiene clave  ->  usa:  .\scripts\setup-bd.ps1 -ConClave"
    Write-Host "    - MySQL no esta levantado ->  usa:  .\scripts\mysql-iniciar.ps1"
    Write-Host ""
}
