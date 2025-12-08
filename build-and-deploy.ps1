# Script para compilar e deployar o plugin
Write-Host "🔨 Compilando plugin..." -ForegroundColor Cyan
Set-Location "C:\Users\poyya\OneDrive\Documentos\SVminecraft\backend\Plugin"
mvn clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Compilação concluída!" -ForegroundColor Green
    Write-Host "📦 Copiando para o servidor..." -ForegroundColor Cyan
    Copy-Item "target\nexus-plugin-0.1.0-SNAPSHOT.jar" "C:\MinecraftServer\plugins\nexus-plugin.jar" -Force
    Write-Host "✅ Plugin copiado! Use 'reload confirm' no servidor." -ForegroundColor Green
}
else {
    Write-Host "❌ Erro na compilação!" -ForegroundColor Red
}
