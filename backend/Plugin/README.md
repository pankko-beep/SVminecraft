# Nexus — Plugin Agregador (Fase 1)

Plugin para Paper/Spigot (1.20.x) agregando sistemas em português:

## Documentação de Status
Consulte `docs/STATUS_ATUAL.md` para o estado atual do servidor, comandos existentes e componentes implementados.

## Mecânicas (Módulos)
Veja `docs/MECANICAS.md` para a lista de blocos de funcionalidades ativáveis (módulos), seu estado e como habilitar/desabilitar em `config.yml`.

## Requisitos

## Comandos

Tab-complete implementado para todos os comandos.

## Instalação
1. Compile:
```powershell
mvn -q -e -DskipTests package
```
2. Copie `target/nexus-plugin-0.1.0-SNAPSHOT.jar` para `plugins/` do servidor.
3. Garanta que `Vault` e um provedor de economia estejam instalados.
4. (Opcional) Instale `DecentHolograms` para hologramas dos painéis.

## Configuração
Arquivo: `plugins/Nexus/config.yml`

## Próximos Passos (Fase 2+)

## Aviso
Esta é uma base funcional mínima para testes locais e expansão.
