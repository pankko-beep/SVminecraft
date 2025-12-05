# Build script para Windows PowerShell
# - Compila com Maven (pulando testes)
# - Copia o(s) JAR(s) gerados para ./dist

Write-Host "Executando build do plugin (mvn -DskipTests package)"
mvn -DskipTests package

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build falhou. Verifique os logs do Maven acima."
    exit $LASTEXITCODE
}

# Cria pasta dist
$dist = Join-Path -Path $PWD -ChildPath "dist"
if (!(Test-Path $dist)) { New-Item -ItemType Directory -Path $dist | Out-Null }

# Copia jars do target
Get-ChildItem -Path "$PWD\target" -Filter "*.jar" | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination $dist -Force
    Write-Host "Copiado: $($_.Name) -> dist\"
}

Write-Host "Build concluído. JAR(s) em: $dist"