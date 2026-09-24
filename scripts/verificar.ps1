# ============================================================================
# Revisa que tu PC tenga todo lo necesario. No instala nada.
#
# Uso:   .\scripts\verificar.ps1
# ============================================================================

$ErrorActionPreference = "Continue"
$falta = $false

Write-Host ""
Write-Host "  Revisando tu entorno..." -ForegroundColor Cyan
Write-Host ""

# --- Java ------------------------------------------------------------------
$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) {
    $v = (java -version 2>&1 | Out-String)
    $ver = "desconocida"
    if ($v -match '"(\d+)[\.\"]') { $ver = $Matches[1] }
    if ([int]$ver -ge 17) {
        Write-Host "  [OK]    Java $ver" -ForegroundColor Green
    } else {
        Write-Host "  [MAL]   Java $ver - Spring Boot 3 necesita 17 o superior" -ForegroundColor Red
        $falta = $true
    }
} else {
    # Puede estar instalado pero con el PATH sin refrescar en esta terminal.
    $jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -like 'jdk-1*' } | Select-Object -First 1
    if ($jdk) {
        Write-Host "  [OK]    Java (instalado, falta abrir terminal nueva)" -ForegroundColor Yellow
        Write-Host "          $($jdk.Name)" -ForegroundColor DarkGray
    } else {
        Write-Host "  [FALTA] Java" -ForegroundColor Red
        $falta = $true
    }
}

# --- Maven -----------------------------------------------------------------
$mavenLocal = "$env:USERPROFILE\tools\apache-maven-3.9.9\bin\mvn.cmd"
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "  [OK]    Maven (en el PATH)" -ForegroundColor Green
} elseif (Test-Path $mavenLocal) {
    Write-Host "  [OK]    Maven (instalado, falta abrir terminal nueva)" -ForegroundColor Yellow
} else {
    Write-Host "  [FALTA] Maven" -ForegroundColor Red
    $falta = $true
}

# --- MySQL -----------------------------------------------------------------
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
if ((Get-Command mysql -ErrorAction SilentlyContinue) -or (Test-Path "$mysqlBin\mysql.exe")) {
    $corriendo = (Get-Process mysqld -ErrorAction SilentlyContinue) -or
                 (Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'MySQL' -and $_.Status -eq 'Running' })
    if ($corriendo) {
        Write-Host "  [OK]    MySQL (corriendo)" -ForegroundColor Green
    } else {
        Write-Host "  [OK]    MySQL (instalado, APAGADO)" -ForegroundColor Yellow
        Write-Host "          arrancalo con:  .\scripts\mysql-iniciar.ps1" -ForegroundColor DarkGray
    }
} else {
    Write-Host "  [FALTA] MySQL" -ForegroundColor Red
    $falta = $true
}

# --- Git -------------------------------------------------------------------
if (Get-Command git -ErrorAction SilentlyContinue) {
    Write-Host "  [OK]    Git" -ForegroundColor Green
} else {
    Write-Host "  [FALTA] Git" -ForegroundColor Red
    $falta = $true
}

Write-Host ""
if ($falta) {
    Write-Host "  Te falta algo. Corre:   .\scripts\instalar.ps1" -ForegroundColor Yellow
} else {
    Write-Host "  Todo listo. Para levantar el proyecto:" -ForegroundColor Green
    Write-Host ""
    Write-Host "      .\scripts\mysql-iniciar.ps1   (arranca MySQL)"
    Write-Host "      .\scripts\setup-bd.ps1        (crea la base, solo la primera vez)"
    Write-Host "      .\scripts\iniciar.ps1         (levanta la aplicacion)"
}
Write-Host ""
