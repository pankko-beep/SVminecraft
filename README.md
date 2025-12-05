# SVminecraft — Plugin multi-módulos (exemplo)

Este repositório contém um plugin-base para servidores Paper/Spigot que agrupa vários módulos (Loja, Objetivos, PainelHUD, Ranks, Times/Guildas, VIP e Escudo). Este README explica rapidamente como construir, empacotar e testar o plugin localmente.

## ✅ O que foi feito
- Criado `MainPlugin` (ponto de entrada do plugin) que inicializa e coordena todos os módulos
- Refatorados os módulos para funcionarem como *components* (injeção da instância principal)
- Atualizado `plugin.yml` com comandos e permissões em português
 - Adicionada persistência SQLite para Objetivos e Ranks (via `DatabaseManager`)

---

## 🛠️ Requisitos
- Java 17+ (JDK)
- Maven
- Servidor Paper/Spigot compatível com 1.20.x (ou ajuste a versão no `pom.xml`)

---

## 📦 Build (Windows PowerShell)
Abra PowerShell na pasta do projeto e execute:

```powershell
.
# usar o script build.ps1 (recomendado)
powershell -ExecutionPolicy Bypass -File .\build.ps1

# ou rodar diretamente
mvn -DskipTests package
```

O JAR final ficará em `target/` (ex.: `target/plugin-1.0-SNAPSHOT.jar`). Copie-o para a pasta `plugins/` do servidor e reinicie o servidor para testar.

---

## 🧭 Comandos principais
- /objetivos — lista objetivos ativos do servidor
- /objetivos progresso — mostra seu progresso pessoal em objetivos
- /loja — comandos da loja (ex.: /loja criar, /loja <nome>)
- /recompensa — recompensa diária (exemplo)
- /vipset <player> <tipo> — (admin) define VIP do jogador
- /resgatavip — resgata bônus semanal de VIP
- /time <COMETA|ECLIPSE> — escolhe um time
- /guilda criar <nome> — cria guilda (regras internas)

Permissões principais definidas no `plugin.yml`:
- `vip.admin` — permissões para definir VIPs (default: op)
- `loja.admin` — (provisório)
- `guilda.admin` — (provisório)

---

## ✨ Próximos passos / sugestões
- Persistir dados (objetivos, lojas, ranks) em banco (SQLite/MySQL) ao invés do config simples.
 - Persistência: Objetivos e Ranks agora usam SQLite. Você pode estender o `DatabaseManager` para outras tabelas (ex.: lojas).
- Implementar eventos concretos para aumentar progresso de objetivos automaticamente.
- Registrar métricas / telemetria e adicionar testes com MockBukkit.


