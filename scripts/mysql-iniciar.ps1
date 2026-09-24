# ============================================================================
# Arranca el servidor MySQL.
#
# Uso:   .\scripts\mysql-iniciar.ps1
#
# Por que hace falta este script: el instalador silencioso de MySQL solo
# copia los archivos. NO inicializa el servidor ni registra el servicio de
# Windows, porque eso requiere permisos de administrador.
#
# Este script inicializa la base la primera vez (root SIN clave, que es lo
# normal en desarrollo local) y deja el servidor corriendo como proceso.
#
# OJO: al ser un proceso y no un servicio, se apaga al reiniciar la PC.
# Hay que volver a correr este script. Si prefieres que arranque solo con
# Windows, mira "Registrarlo como servicio" al final del archivo.
# ============================================================================

$ErrorActionPreference = "Stop"

$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
$base     = "$env:USERPROFILE\tools\mysql"
$data     = "$base\data"
$ini      = "$base\my.ini"

if (-not (Test-Path "$mysqlBin\mysqld.exe")) {
    Write-Host ""
    Write-Host "  No encuentro MySQL. Corre primero:  .\scripts\instalar.ps1" -ForegroundColor Red
    Write-Host ""
    exit 1
}

# Si ya hay un servicio de Windows corriendo, no hacemos nada.
$svc = Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'MySQL' -and $_.Status -eq 'Running' }
if ($svc) {
    Write-Host "  El servicio $($svc.Name) ya esta corriendo." -ForegroundColor Green
    exit 0
}

# Ya esta levantado como proceso?
if (Get-Process mysqld -ErrorAction SilentlyContinue) {
    Write-Host "  MySQL ya esta corriendo." -ForegroundColor Green
    exit 0
}

# --- Primera vez: crear configuracion e inicializar ------------------------
if (-not (Test-Path $ini)) {
    if (-not (Test-Path $base)) { New-Item -ItemType Directory -Path $base -Force | Out-Null }
    $contenido = @"
[mysqld]
basedir=C:/Program Files/MySQL/MySQL Server 8.4
datadir=$($data -replace '\\','/')
port=3306
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
[client]
port=3306
default-character-set=utf8mb4
"@
    Set-Content -Path $ini -Value $contenido -Encoding ASCII
    Write-Host "  Configuracion creada en $ini" -ForegroundColor Green
}

if (-not (Test-Path "$data\mysql")) {
    Write-Host "  Inicializando la base por primera vez (tarda unos segundos)..." -ForegroundColor Cyan
    & "$mysqlBin\mysqld.exe" --defaults-file="$ini" --initialize-insecure --console 2>&1 | Out-Null
    if (Test-Path "$data\mysql") {
        Write-Host "  Inicializada. El usuario root queda SIN clave (normal en local)." -ForegroundColor Green
    } else {
        Write-Host "  Fallo la inicializacion." -ForegroundColor Red
        exit 1
    }
}

# --- Arrancar --------------------------------------------------------------
Write-Host "  Arrancando MySQL..." -ForegroundColor Cyan
Start-Process -FilePath "$mysqlBin\mysqld.exe" -ArgumentList "--defaults-file=`"$ini`"" -WindowStyle Hidden

$ok = $false
for ($i = 1; $i -le 30; $i++) {
    & "$mysqlBin\mysqladmin.exe" --protocol=tcp -h 127.0.0.1 -u root ping 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $ok = $true; break }
}

Write-Host ""
if ($ok) {
    Write-Host "  MySQL corriendo en localhost:3306 (usuario root, sin clave)." -ForegroundColor Green
    Write-Host ""
    Write-Host "  Siguiente paso:   .\scripts\setup-bd.ps1"
} else {
    Write-Host "  MySQL no respondio. Revisa el log en:" -ForegroundColor Red
    Write-Host "      $data\*.err"
}
Write-Host ""

# ============================================================================
# Registrarlo como servicio de Windows (opcional, arranca solo con la PC)
#
# Abre PowerShell COMO ADMINISTRADOR y corre estas dos lineas:
#
#   & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --install MySQL84 --defaults-file="$env:USERPROFILE\tools\mysql\my.ini"
#   net start MySQL84
#
# Para quitarlo despues:
#
#   net stop MySQL84
#   & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --remove MySQL84
# ============================================================================
