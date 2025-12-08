# 📚 Documentação Completa do Projeto SVminecraft

**Data:** 7 de dezembro de 2025  
**Versão Java:** 21 LTS (recém-atualizado)  
**Plataforma:** Spigot/Paper 1.20.4

---

## 🎯 Visão Geral do Projeto

O **SVminecraft** é um projeto completo de servidor Minecraft que combina:
1. **Backend Node.js** - Sistema de pagamentos PIX via Mercado Pago
2. **Plugin Java (Nexus)** - Sistema modular de gameplay para Minecraft
3. **Scripts de Deploy** - Automação de compilação e implantação

O projeto integra economia real (pagamentos PIX) com economia de jogo (moedas Nexus), criando um ecossistema completo para servidores de Minecraft.

---

## 📁 Estrutura de Diretórios

```
SVminecraft/
├── backend/                          # Servidor Node.js
│   ├── server.js                     # API REST principal
│   ├── package.json                  # Dependências Node.js
│   ├── utils/
│   │   └── database.js              # Funções de persistência JSON
│   ├── database/                     # Armazenamento JSON
│   │   ├── pedidos.json             # Pedidos criados
│   │   ├── pagamentos.json          # IDs processados (anti-duplicação)
│   │   └── vips_ativos.json         # VIPs ativados
│   ├── logs/                         # Logs de operação
│   └── Plugin/                       # Plugin Java Nexus
│       ├── pom.xml                   # Configuração Maven
│       ├── src/main/
│       │   ├── java/br/com/nexus/
│       │   │   ├── NexusPlugin.java # Classe principal
│       │   │   ├── commands/        # Comandos do jogo
│       │   │   ├── listeners/       # Event handlers
│       │   │   ├── services/        # Lógica de negócio
│       │   │   └── panels/          # Sistema de painéis
│       │   └── resources/
│       │       ├── plugin.yml       # Metadados do plugin
│       │       └── config.yml       # Configurações
│       ├── docs/                     # Documentação técnica
│       └── target/                   # Builds compilados
├── build-and-deploy.ps1             # Script de deploy
└── package.json                      # Dependências raiz
```

---

## 🔧 PARTE 1: Backend Node.js (Sistema de Pagamentos)

### 📄 `backend/server.js`

**Propósito:** Servidor Express que gerencia pagamentos PIX via Mercado Pago.

#### Funcionalidades:

1. **Rota de Status** (`GET /`)
   - Verifica se o servidor está online
   - Retorna: `"✅ Servidor online"`

2. **Criar Pagamento PIX** (`POST /criar-pix`)
   - **Entrada:** `{ valor, nick, uuid, vip }`
   - **Processo:**
     - Gera um UUID único para o pedido
     - Salva pedido em `pedidos.json`
     - Chama API do Mercado Pago para criar pagamento PIX
     - Inclui `notification_url` para receber webhook
   - **Saída:** Dados do PIX (QR Code, código copia-e-cola, etc.)

3. **Webhook de Notificação** (`POST /webhook`)
   - **Gatilho:** Mercado Pago envia notificação quando pagamento muda de status
   - **Processo:**
     - Recebe notificação (suporta JSON ou texto)
     - Extrai `paymentId`
     - Verifica se já foi processado (anti-duplicação)
     - Busca dados completos do pagamento na API do Mercado Pago
     - Se aprovado e PIX:
       - Marca como processado
       - Salva VIP em `vips_ativos.json`
       - Registra log em `logs/webhook.log`
   - **Saída:** Status 200 (sempre, para evitar reenvios)

#### Variáveis de Ambiente (.env):
```
MP_ACCESS_TOKEN=seu_token_mercado_pago
NGROK_URL=https://seu-dominio.ngrok.io
```

#### Fluxo de Dados:
```
Jogador no jogo → POST /criar-pix → Mercado Pago → QR Code PIX
Jogador paga → Mercado Pago → POST /webhook → vips_ativos.json → Plugin lê arquivo
```

---

### 📄 `backend/utils/database.js`

**Propósito:** Camada de abstração para persistência em arquivos JSON.

#### Funções:

1. **`lerArquivo(caminho)`**
   - Lê arquivo JSON
   - Retorna array vazio se não existir

2. **`salvarArquivo(caminho, dados)`**
   - Escreve dados em JSON formatado (indent 2)

3. **`salvarPedido(pedido)`**
   - Adiciona pedido a `pedidos.json`
   - Usado para rastreamento de pedidos criados

4. **`jaFoiProcessado(id)`**
   - Verifica se `paymentId` está em `pagamentos.json`
   - Previne processamento duplicado de webhooks

5. **`marcarComoProcessado(id)`**
   - Adiciona `paymentId` a `pagamentos.json`

6. **`ativarVipArquivo(vipData)`**
   - Adiciona registro de VIP a `vips_ativos.json`
   - Plugin Java monitora este arquivo

#### Arquivos Gerenciados:
- **`pedidos.json`**: Histórico de pedidos criados
- **`pagamentos.json`**: Lista de IDs processados (anti-duplicação)
- **`vips_ativos.json`**: VIPs aprovados aguardando ativação no jogo

---

### 📄 `backend/package.json`

**Dependências:**
- **express**: Framework web para APIs REST
- **dotenv**: Carrega variáveis de ambiente
- **node-fetch**: Cliente HTTP para chamar API do Mercado Pago

**Scripts:**
- `start`: Inicia o servidor na porta 3333

---

## 🎮 PARTE 2: Plugin Java Nexus (Sistema de Gameplay)

### 📄 `backend/Plugin/pom.xml`

**Propósito:** Configuração Maven para build do plugin.

#### Configurações Principais:

**Propriedades:**
```xml
<java.version>21</java.version>
<maven.compiler.release>21</maven.compiler.release>
<spigot.api.version>1.20.4-R0.1-SNAPSHOT</spigot.api.version>
```

**Dependências:**
1. **spigot-api**: API do servidor Minecraft (provided)
2. **VaultAPI**: Sistema de economia (provided)
3. **HikariCP**: Pool de conexões para banco de dados
4. **sqlite-jdbc**: Driver SQLite
5. **gson**: Serialização JSON

**Plugins de Build:**
1. **maven-compiler-plugin**: Compila para Java 21
2. **maven-shade-plugin**: Empacota dependências no JAR final

**Repositórios:**
- Spigot (snapshots)
- Sonatype
- JitPack

---

### 📄 `backend/Plugin/src/main/resources/plugin.yml`

**Propósito:** Metadados do plugin para o Bukkit/Spigot.

#### Configurações:

**Informações Básicas:**
```yaml
name: Nexus
version: 0.1.0-SNAPSHOT
main: br.com.nexus.NexusPlugin
api-version: 1.20
load: POSTWORLD
```

**Dependências Opcionais (softdepend):**
- Vault, DecentHolograms, Essentials, LuckPerms
- SimpleLogin, EconomyShopGUI, spark
- CombatLogX, PvPManager, PlayerLevels, etc.

**Comandos Registrados:**
- `/saldo` - Consulta saldo
- `/pagar` - Transfere moedas
- `/historico` - Histórico de transações
- `/time` - Gerencia times (Solar/Lunar)
- `/guild` - Sistema de guildas
- `/fly` - Ativa/desativa voo (admin)
- `/econ` - Administração de economia
- `/painel` - Gerencia painéis holográficos
- `/auditoria` - Consulta eventos
- `/_transacoes` - Consulta transações (admin)

**Permissões:**
Cada comando tem sua permissão associada (ex: `nexus.saldo`, `nexus.pagar`)

---

### 📄 `backend/Plugin/src/main/resources/config.yml`

**Propósito:** Configurações customizáveis do plugin.

#### Seções Principais:

1. **Moeda e Limites:**
```yaml
moeda-nome: "moedas"
limites:
  pagar:
    max-por-transacao: 500000
    cooldown-segundos: 10
  time:
    custo-troca: 1000000
```

2. **Auditoria:**
```yaml
auditoria:
  historico-por-jogador: 30
  salvar-intervalo-segundos: 60
```

3. **Painéis Holográficos:**
```yaml
painel:
  usar-decent-holograms: true
  arquivo-persistencia: panels.json
  refresh-segundos: 30
  metricas:
    janela-minutos: 60
    top: 6
```

4. **Módulos Ativáveis:**
```yaml
modulos:
  economia: true
  times: true
  guildas: true
  paineis: true
  auditoria: true
  transacoes: true
  login: true
```

5. **Storage (Banco de Dados):**
```yaml
storage:
  tipo: sqlite  # ou mysql
  sqlite:
    arquivo: database.db
  mysql:
    host: 127.0.0.1
    porta: 3306
    database: nexus
    usuario: root
    senha: ""
```

6. **Mensagens Personalizáveis:**
Todas as mensagens do plugin são configuráveis (prefixo, cores, textos).

---

### 📄 `backend/Plugin/src/main/java/br/com/nexus/NexusPlugin.java`

**Propósito:** Classe principal do plugin - inicializa todos os sistemas.

#### Ciclo de Vida:

**`onEnable()`:**
1. **Carrega configuração padrão**
2. **Inicializa Services:**
   - `TransactionService` - Gerencia transações
   - `EconomyService` - Integra Vault
   - `TeamService` - Sistema de times
   - `GuildService` - Sistema de guildas
   - `PanelService` - Painéis holográficos
   - `PlayerDataService` - Dados dos jogadores
   - `DatabaseService` - Conexão com banco
   - `AuditService` - Registro de eventos

3. **Verifica módulos ativos** (via config.yml)
4. **Registra comandos** (apenas se módulo ativo)
5. **Registra listeners** (eventos do jogo)
6. **Configura hooks** (SimpleLogin, AuthMe)
7. **Agenda tarefas periódicas** (refresh de painéis)

**`onDisable()`:**
- Salva dados de painéis
- Fecha conexões do banco
- Finaliza services

#### Métodos Públicos:
```java
public EconomyService economy()        // Acessa serviço de economia
public TeamService teams()              // Acessa serviço de times
public GuildService guilds()            // Acessa serviço de guildas
public PanelService panels()            // Acessa serviço de painéis
public TransactionService transactions() // Acessa serviço de transações
public PlayerDataService data()         // Acessa dados de jogadores
public DatabaseService db()             // Acessa banco de dados
public AuditService audit()             // Acessa auditoria
```

---

## 🔌 PARTE 3: Services (Lógica de Negócio)

### 📄 `EconomyService.java`

**Funções:**
- Integra com Vault API
- Gerencia saldo dos jogadores
- Executa transferências (com validações)
- Congela/descongela economia de jogadores
- Registra todas as operações em auditoria e transações

**Métodos Principais:**
- `getBalance(uuid)` - Consulta saldo
- `withdraw(uuid, amount)` - Remove moedas
- `deposit(uuid, amount)` - Adiciona moedas
- `transfer(from, to, amount, note)` - Transferência entre jogadores
- `freeze/unfreeze(uuid)` - Bloqueia economia

---

### 📄 `TeamService.java`

**Funções:**
- Gerencia times Solar e Lunar
- Impõe escolha obrigatória ao entrar
- Permite troca de time (com custo)
- Aplica cores aos nomes dos jogadores
- Persiste dados em YAML

**Métodos Principais:**
- `getTeam(uuid)` - Consulta time do jogador
- `setTeam(uuid, team)` - Define time
- `hasTeam(uuid)` - Verifica se escolheu time
- `switchTeam(uuid, newTeam, cost)` - Troca de time

---

### 📄 `GuildService.java`

**Funções:**
- Sistema de guildas (clãs)
- Criar, convidar, aceitar, sair
- Gerencia membros e líderes
- Persiste dados em YAML

**Métodos Principais:**
- `createGuild(name, leader)` - Cria guilda
- `invite(guild, target)` - Convida jogador
- `accept(player, guild)` - Aceita convite
- `leave(player)` - Sai da guilda
- `getGuild(player)` - Consulta guilda

---

### 📄 `PanelService.java`

**Funções:**
- Cria painéis holográficos (GLOBAL, TIME, GUILDA)
- Suporta DecentHolograms ou TextDisplay nativo
- Atualiza métricas periodicamente
- Exibe estatísticas de auditoria e transações

**Tipos de Painéis:**
1. **GLOBAL**: Métricas gerais do servidor
2. **TIME**: Métricas por time (Solar/Lunar)
3. **GUILDA**: Métricas de uma guilda específica

**Métodos Principais:**
- `create(type, location)` - Cria painel
- `createGuildPanel(name, location)` - Cria painel de guilda
- `delete(id)` - Remove painel
- `refreshAll()` - Atualiza todos os painéis

---

### 📄 `TransactionService.java`

**Funções:**
- Registra transações no banco de dados
- Fornece histórico e consultas
- Exporta dados em CSV/JSON

**Tabela `transactions`:**
```sql
id | from_uuid | to_uuid | amount | note | timestamp
```

**Métodos Principais:**
- `record(from, to, amount, note)` - Registra transação
- `getHistory(player, minutes, limit)` - Histórico pessoal
- `listAll(filters)` - Lista com filtros
- `export(format, filters)` - Exporta dados

---

### 📄 `AuditService.java`

**Funções:**
- Registra todos os eventos do servidor
- Armazena em banco de dados
- Fornece consultas e métricas
- Exporta relatórios

**Tabela `audit_events`:**
```sql
id | type | player | target | context | timestamp
```

**Tipos de Eventos:**
- `PLAYER_JOIN`, `PLAYER_QUIT`
- `MONEY_TRANSFER`, `TEAM_CHANGE`
- `GUILD_CREATE`, `GUILD_JOIN`
- `PANEL_CREATE`, `PANEL_DELETE`
- E muitos outros...

**Métodos Principais:**
- `log(type, player, target, context)` - Registra evento
- `countByType(after, limit)` - Conta eventos por tipo
- `countForTeam(team, after, limit)` - Métricas de time
- `countForGuild(guild, after, limit)` - Métricas de guilda

---

### 📄 `DatabaseService.java`

**Funções:**
- Gerencia conexão com banco (SQLite ou MySQL)
- Usa HikariCP para pool de conexões
- Cria tabelas automaticamente
- Fornece API para queries

**Tabelas Criadas:**
- `audit_events`
- `transactions`

**Métodos Principais:**
- `getConnection()` - Obtém conexão do pool
- `execute(sql, params)` - Executa query
- `query(sql, params)` - Consulta com ResultSet
- `close()` - Fecha pool

---

### 📄 `PlayerDataService.java`

**Funções:**
- Persiste dados dos jogadores em YAML
- Gerencia saldo inicial
- Salva/carrega automaticamente

**Dados Armazenados:**
- UUID
- Nome
- Saldo
- Time
- Guilda
- Timestamps

---

## 🎮 PARTE 4: Commands (Comandos do Jogo)

### Comandos de Economia:

**`SaldoCommand.java`**
- Comando: `/saldo`
- Exibe saldo atual do jogador

**`PagarCommand.java`**
- Comando: `/pagar <jogador> <valor> [nota]`
- Transfere moedas entre jogadores
- Validações: saldo suficiente, limites, cooldown
- Registra em transações e auditoria

**`HistoricoCommand.java`**
- Comando: `/historico [minutos] [limite]`
- Exibe histórico pessoal de transações

**`EconCommand.java`**
- Comando: `/econ <freeze|unfreeze|status> <jogador>`
- Administração de economia (apenas admins)

---

### Comandos de Times:

**`TimeCommand.java`**
- Comando: `/time <escolher|trocar> [Solar|Lunar]`
- Escolha obrigatória no primeiro login
- Troca de time (com custo configurável)
- Aplica cores ao nome

---

### Comandos de Guildas:

**`GuildCommand.java`**
- Comando: `/guild <criar|convidar|aceitar|sair> [args]`
- Sistema completo de guildas
- Gerencia convites e membros

---

### Comandos de Painéis:

**`PainelCommand.java`**
- Comando: `/painel <criar|criar-guilda|deletar|listar|info|refresh>`
- Gerencia painéis holográficos
- Tipos: GLOBAL, TIME, GUILDA

---

### Comandos Administrativos:

**`AuditoriaCommand.java`**
- Comando: `/auditoria <listar|export> [filtros]`
- Consulta eventos de auditoria
- Exporta relatórios

**`TransacoesCommand.java`**
- Comando: `/_transacoes <listar|export> [filtros]`
- Consulta transações
- Exporta em CSV/JSON

**`FlyCommand.java`**
- Comando: `/fly [on|off]`
- Ativa/desativa voo (admins)

---

## 👂 PARTE 5: Listeners (Event Handlers)

### 📄 `PlayerLifecycleListener.java`

**Eventos Tratados:**
- `PlayerJoinEvent` - Jogador entra no servidor
- `PlayerQuitEvent` - Jogador sai do servidor

**Ações:**
- Carrega dados do jogador
- Salva dados ao sair
- Registra em auditoria

---

### 📄 `NoTeamMovementListener.java`

**Evento Tratado:**
- `PlayerMoveEvent`

**Ação:**
- Bloqueia movimento de jogadores sem time definido
- Exibe mensagem para escolher time

---

### 📄 `SimpleLoginHook.java`

**Eventos Tratados:**
- `LoginEvent` (do plugin SimpleLogin)
- `LogoutEvent`

**Ações:**
- Registra login em auditoria
- Registra logout em auditoria

---

### 📄 `AuthMeHook.java`

**Eventos Tratados (via reflexão):**
- `fr.xephi.authme.events.LoginEvent`
- `fr.xephi.authme.events.LogoutEvent`

**Ações:**
- Registra autenticação em auditoria
- Funciona apenas se AuthMe estiver instalado

---

## 🚀 PARTE 6: Scripts de Deploy

### 📄 `build-and-deploy.ps1`

**Propósito:** Automatiza compilação e implantação do plugin.

**Processo:**
1. Navega para diretório do plugin
2. Executa `mvn clean package -DskipTests`
3. Se sucesso:
   - Copia JAR para `C:\MinecraftServer\plugins\`
   - Renomeia para `nexus-plugin.jar`
   - Exibe mensagem de sucesso
4. Se falha:
   - Exibe erro

**Uso:**
```powershell
.\build-and-deploy.ps1
```

---

## 🔄 PARTE 7: Fluxos de Interação

### Fluxo 1: Compra de VIP

```
1. Jogador no jogo → Comando/Interface de compra
2. Cliente envia POST /criar-pix → Backend Node.js
3. Backend → API Mercado Pago → QR Code PIX
4. QR Code exibido no jogo
5. Jogador paga via app bancário
6. Mercado Pago → POST /webhook → Backend Node.js
7. Backend valida pagamento
8. Backend salva em vips_ativos.json
9. Plugin Java monitora arquivo (ou consulta via API)
10. Plugin ativa VIP no jogo
```

---

### Fluxo 2: Transferência de Moedas

```
1. Jogador executa /pagar <alvo> <valor> [nota]
2. PagarCommand → EconomyService.transfer()
3. EconomyService valida:
   - Saldo suficiente
   - Limite por transação
   - Cooldown
4. Se válido:
   - Retira moedas do pagador (Vault)
   - Adiciona moedas ao alvo (Vault)
   - TransactionService.record() → Banco de dados
   - AuditService.log() → Banco de dados
5. Mensagens enviadas aos jogadores
```

---

### Fluxo 3: Criação de Painel

```
1. Admin executa /painel criar GLOBAL
2. PainelCommand → PanelService.create()
3. PanelService cria painel:
   - Se DecentHolograms disponível:
     → Comando do DH (holograma multi-linha)
   - Se não:
     → TextDisplay nativo (billboard centralizado)
4. Painel exibe métricas:
   - AuditService.countByType() → Consulta eventos
   - TransactionService.getMetrics() → Consulta transações
5. Painel atualiza a cada 30 segundos (configurável)
6. Dados salvos em panels.json
```

---

### Fluxo 4: Escolha de Time

```
1. Jogador entra pela primeira vez
2. NoTeamMovementListener bloqueia movimento
3. Jogador executa /time escolher Solar
4. TimeCommand → TeamService.setTeam()
5. TeamService:
   - Salva time em YAML
   - AuditService.log(TEAM_CHOOSE)
   - Aplica cor ao nome (prefixo/sufixo)
6. Mensagem de confirmação
7. Movimento liberado
```

---

### Fluxo 5: Auditoria

```
1. Qualquer ação importante ocorre no jogo
2. Service correspondente chama AuditService.log()
3. AuditService insere registro em audit_events:
   - type: tipo do evento
   - player: UUID do jogador
   - target: UUID do alvo (se aplicável)
   - context: dados adicionais (JSON)
   - timestamp: momento do evento
4. Admin consulta: /auditoria listar [filtros]
5. AuditoriaCommand → AuditService.query()
6. Resultados exibidos ou exportados
```

---

## 🗄️ PARTE 8: Estrutura do Banco de Dados

### Tabela: `audit_events`

```sql
CREATE TABLE audit_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(50) NOT NULL,
    player VARCHAR(36),
    target VARCHAR(36),
    context TEXT,
    timestamp BIGINT NOT NULL
);
```

**Índices:**
```sql
CREATE INDEX idx_audit_timestamp ON audit_events(timestamp);
CREATE INDEX idx_audit_type ON audit_events(type);
CREATE INDEX idx_audit_player ON audit_events(player);
```

**Eventos Comuns:**
- `PLAYER_JOIN`, `PLAYER_QUIT`
- `MONEY_TRANSFER`, `MONEY_DEPOSIT`, `MONEY_WITHDRAW`
- `TEAM_CHOOSE`, `TEAM_SWITCH`
- `GUILD_CREATE`, `GUILD_JOIN`, `GUILD_LEAVE`
- `PANEL_CREATE`, `PANEL_DELETE`, `PANEL_REFRESH`

---

### Tabela: `transactions`

```sql
CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    from_uuid VARCHAR(36) NOT NULL,
    to_uuid VARCHAR(36) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    note TEXT,
    timestamp BIGINT NOT NULL
);
```

**Índices:**
```sql
CREATE INDEX idx_trans_from ON transactions(from_uuid);
CREATE INDEX idx_trans_to ON transactions(to_uuid);
CREATE INDEX idx_trans_timestamp ON transactions(timestamp);
```

---

## 📊 PARTE 9: Arquivos de Dados

### `backend/database/pedidos.json`

**Estrutura:**
```json
[
  {
    "orderId": "uuid-do-pedido",
    "nick": "Player123",
    "uuid": "uuid-do-jogador",
    "vip": "VIP_PREMIUM",
    "valor": 29.90,
    "status": "pending",
    "data": "2025-12-07T10:30:00.000Z"
  }
]
```

**Uso:** Rastreamento de pedidos criados

---

### `backend/database/pagamentos.json`

**Estrutura:**
```json
[
  "payment_id_123",
  "payment_id_456"
]
```

**Uso:** Lista de IDs já processados (previne duplicação)

---

### `backend/database/vips_ativos.json`

**Estrutura:**
```json
[
  {
    "nick": "Player123",
    "uuid": "uuid-do-jogador",
    "vip": "VIP_PREMIUM",
    "payment_id": "mp_payment_id",
    "valor": 29.90,
    "data": "2025-12-07T10:35:00.000Z"
  }
]
```

**Uso:** VIPs aprovados aguardando ativação no plugin

---

### `plugins/Nexus/players/*.yml`

**Estrutura:**
```yaml
uuid: "player-uuid"
name: "Player123"
balance: 5000.0
team: "SOLAR"
guild: "DragonSlayers"
firstJoin: 1234567890000
lastSeen: 1234567899999
```

**Uso:** Dados persistentes dos jogadores

---

### `plugins/Nexus/panels.json`

**Estrutura:**
```json
{
  "panel-id-1": {
    "world": "world",
    "x": 100.5,
    "y": 65.0,
    "z": 200.5,
    "yaw": 0.0,
    "pitch": 0.0,
    "type": "GLOBAL",
    "guildName": null
  }
}
```

**Uso:** Localização e configuração dos painéis

---

## 🔐 PARTE 10: Segurança e Validações

### Validações de Economia:

1. **Saldo Suficiente:** Verifica antes de transferir
2. **Limites por Transação:** Configura em `config.yml`
3. **Cooldown:** Previne spam de transferências
4. **Anti-Duplicação:** Usa `pagamentos.json`
5. **Congelamento:** Admin pode bloquear economia de jogadores

---

### Validações de Times:

1. **Escolha Obrigatória:** Bloqueia movimento até escolher
2. **Custo de Troca:** Cobra moedas para trocar de time
3. **Cores Exclusivas:** Solar = amarelo, Lunar = roxo

---

### Validações de Guildas:

1. **Nome Único:** Não permite guildas com mesmo nome
2. **Limite de Membros:** Configurável
3. **Permissões:** Apenas líder pode convidar

---

### Validações de Pagamentos:

1. **Webhook Signature:** Mercado Pago assina webhooks
2. **Anti-Replay:** `jaFoiProcessado()` previne reprocessamento
3. **Status Check:** Só processa pagamentos `approved`
4. **Method Check:** Só processa PIX

---

## ⚙️ PARTE 11: Configurações e Customização

### Módulos Ativáveis:

```yaml
modulos:
  economia: true      # Sistema de moedas
  times: true         # Times Solar/Lunar
  guildas: true       # Sistema de clãs
  paineis: true       # Hologramas
  auditoria: true     # Registro de eventos
  transacoes: true    # Histórico financeiro
  login: true         # Hooks de autenticação
```

**Desativar módulo:**
1. Edita `config.yml`
2. Muda para `false`
3. Recarrega plugin

---

### Mensagens Customizáveis:

Todas as mensagens são configuráveis:

```yaml
mensagens:
  prefixo: "§7[§bNexus§7] §r"
  sem-permissao: "§cVocê não tem permissão."
  saldo: "§aSeu saldo: §e%valor% %moeda%"
  pagar-sucesso: "§aVocê pagou §e%valor% §apara §e%alvo%§a."
  # ... dezenas de outras mensagens
```

**Placeholders:**
- `%valor%` - Valor numérico
- `%moeda%` - Nome da moeda
- `%jogador%` - Nome do jogador
- `%time%` - Nome do time
- `%guild%` - Nome da guilda

---

### Storage (Banco de Dados):

**SQLite (Padrão):**
```yaml
storage:
  tipo: sqlite
  sqlite:
    arquivo: database.db
```

**MySQL (Produção):**
```yaml
storage:
  tipo: mysql
  mysql:
    host: 127.0.0.1
    porta: 3306
    database: nexus
    usuario: root
    senha: "senha_segura"
```

---

## 🔄 PARTE 12: Integrações com Plugins Externos

### Vault (Economia):
- **Propósito:** Integração com economia do servidor
- **Uso:** `EconomyService` usa Vault para saldo
- **Fallback:** Sistema próprio se Vault não disponível

### DecentHolograms (Painéis):
- **Propósito:** Hologramas avançados
- **Uso:** `PanelService` prefere DH se disponível
- **Fallback:** TextDisplay nativo do Minecraft

### SimpleLogin (Autenticação):
- **Propósito:** Sistema de login
- **Uso:** `SimpleLoginHook` registra logins/logouts
- **Opcional:** Só ativa se plugin presente

### AuthMe (Autenticação):
- **Propósito:** Sistema de autenticação alternativo
- **Uso:** `AuthMeHook` usa reflexão para hooks
- **Opcional:** Só ativa se plugin presente

### LuckPerms (Permissões):
- **Propósito:** Sistema de permissões
- **Uso:** Comandos verificam permissões via Bukkit
- **Compatível:** Funciona com qualquer plugin de permissões

---

## 🚀 PARTE 13: Processo de Build e Deploy

### Build Local:

**Maven:**
```powershell
cd backend/Plugin
mvn clean package
```

**Saída:**
- `target/nexus-plugin-0.1.0-SNAPSHOT.jar` (com dependências)

---

### Deploy Automatizado:

**Script PowerShell:**
```powershell
.\build-and-deploy.ps1
```

**Processo:**
1. Compila com Maven
2. Copia JAR para pasta do servidor
3. Renomeia para `nexus-plugin.jar`
4. Exibe status

---

### Build Manual:

**Windows:**
```powershell
cd "C:\Users\poyya\OneDrive\Documentos\SVminecraft\backend\Plugin"
mvn clean package -DskipTests
Copy-Item "target\nexus-plugin-0.1.0-SNAPSHOT.jar" "C:\MinecraftServer\plugins\nexus-plugin.jar" -Force
```

**Linux:**
```bash
cd /path/to/SVminecraft/backend/Plugin
mvn clean package -DskipTests
cp target/nexus-plugin-0.1.0-SNAPSHOT.jar /path/to/server/plugins/nexus-plugin.jar
```

---

## 📈 PARTE 14: Monitoramento e Logs

### Logs do Backend Node.js:

**`backend/logs/webhook.log`**
```
APROVADO: Player123 - VIP_PREMIUM - mp_payment_12345
```

**`backend/logs/erros.log`**
```
Error: Failed to connect to Mercado Pago API
    at fetch...
```

---

### Logs do Plugin Java:

**Console do Servidor:**
```
[Nexus] Nexus habilitado.
[Nexus] Hook SimpleLogin ativo.
[Nexus] Banco de dados conectado (SQLite)
[Nexus] 3 painéis carregados
```

**Auditoria (Banco):**
- Todos os eventos registrados em `audit_events`
- Consulta via `/auditoria listar`

---

### Métricas de Performance:

**Plugin Spark:**
- Profiling de CPU/memória
- Identificação de lags
- Integração opcional com Nexus

---

## 🎯 PARTE 15: Casos de Uso Completos

### Caso 1: Novo Jogador Entra

```
1. PlayerJoinEvent disparado
2. PlayerLifecycleListener.onJoin():
   - Carrega dados (ou cria novo registro)
   - AuditService.log(PLAYER_JOIN)
3. NoTeamMovementListener bloqueia movimento
4. Mensagem: "Escolha seu time: /time escolher <Solar|Lunar>"
5. Jogador executa /time escolher Solar
6. TeamService.setTeam():
   - Salva em players/<uuid>.yml
   - Aplica cor amarela ao nome
   - AuditService.log(TEAM_CHOOSE)
7. Movimento liberado
8. Jogador pode jogar normalmente
```

---

### Caso 2: Compra e Ativação de VIP

```
1. Jogador abre interface de VIPs no jogo
2. Seleciona VIP_PREMIUM (R$ 29,90)
3. Cliente envia:
   POST /criar-pix
   { nick: "Player123", uuid: "...", vip: "VIP_PREMIUM", valor: 29.90 }
4. Backend:
   - Salva pedido em pedidos.json
   - Chama API Mercado Pago
   - Retorna QR Code
5. QR Code exibido no jogo
6. Jogador paga via app bancário
7. Mercado Pago envia webhook:
   POST /webhook
   { type: "payment", data: { id: "12345" } }
8. Backend:
   - Busca detalhes do pagamento
   - Verifica status = approved
   - marcarComoProcessado("12345")
   - ativarVipArquivo({ nick, uuid, vip, ... })
9. Plugin Java:
   - Monitora vips_ativos.json (ou consulta API)
   - Detecta novo VIP
   - Ativa permissões/recursos
   - Notifica jogador
```

---

### Caso 3: Transferência de Moedas

```
1. Player123 tem 10.000 moedas
2. Player456 tem 5.000 moedas
3. Player123 executa:
   /pagar Player456 1000 Presente de aniversário
4. PagarCommand.onCommand():
   - Valida argumentos
   - Verifica permissão
5. EconomyService.transfer():
   - Verifica saldo: 10.000 >= 1.000 ✓
   - Verifica limite: 1.000 <= 500.000 ✓
   - Verifica cooldown: OK ✓
   - Vault.withdraw(Player123, 1000)
   - Vault.deposit(Player456, 1000)
6. TransactionService.record():
   INSERT INTO transactions
   (Player123, Player456, 1000, "Presente", timestamp)
7. AuditService.log(MONEY_TRANSFER):
   INSERT INTO audit_events
   (TRANSFER, Player123, Player456, {amount:1000}, timestamp)
8. Mensagens:
   - Player123: "Você pagou 1000 moedas para Player456"
   - Player456: "Você recebeu 1000 moedas de Player123"
9. Saldos atualizados:
   - Player123: 9.000 moedas
   - Player456: 6.000 moedas
```

---

### Caso 4: Painel com Métricas

```
1. Admin executa:
   /painel criar GLOBAL
2. PainelCommand → PanelService.create(GLOBAL, location)
3. PanelService:
   - Gera ID: "abc12345"
   - Cria Panel { id, type:GLOBAL, loc }
   - spawn(panel, linesFor(panel))
4. linesFor(GLOBAL):
   - Consulta últimos 60 minutos
   - AuditService.countByType(after, 6):
     * MONEY_TRANSFER: 45
     * PLAYER_JOIN: 23
     * TEAM_CHOOSE: 12
     * GUILD_CREATE: 3
     * PANEL_CREATE: 2
     * PLAYER_QUIT: 20
   - Formata linhas:
     "§bNexus — Global"
     "§7Últimos 60 min — total §f105"
     "§b• §fMONEY_TRANSFER: §e45"
     "§b• §fPLAYER_JOIN: §e23"
     "§b• §fTEAM_CHOOSE: §e12"
     "..."
5. Se DecentHolograms disponível:
   - Executa: dh create nexus_abc12345
   - Executa: dh setlocation nexus_abc12345 world x y z
   - Para cada linha: dh addline nexus_abc12345 <texto>
6. Se não disponível:
   - Cria TextDisplay no local
   - Define billboard: CENTER
   - Define texto: join("\n", lines)
7. Salva em panels.json
8. A cada 30 segundos:
   - refresh(panel)
   - Recalcula métricas
   - Atualiza texto
```

---

## 🛠️ PARTE 16: Troubleshooting

### Problema: Plugin não carrega

**Sintomas:**
- Plugin não aparece em `/plugins`
- Erro no console

**Soluções:**
1. Verificar versão do Java (deve ser 21)
2. Verificar `plugin.yml` (sintaxe YAML)
3. Verificar dependências (Spigot API)
4. Verificar logs: `logs/latest.log`

---

### Problema: Banco de dados não conecta

**Sintomas:**
- Erro: "Failed to connect to database"

**Soluções:**
1. **SQLite:**
   - Verificar permissões de escrita
   - Verificar path em `config.yml`
2. **MySQL:**
   - Verificar host/porta
   - Verificar usuário/senha
   - Verificar se database existe
   - Testar conexão: `mysql -u root -p`

---

### Problema: Webhook não recebe notificações

**Sintomas:**
- Pagamento aprovado mas VIP não ativa

**Soluções:**
1. Verificar URL do webhook no Mercado Pago
2. Verificar se ngrok está rodando
3. Verificar logs: `backend/logs/webhook.log`
4. Testar manualmente:
   ```powershell
   Invoke-WebRequest -Uri "http://localhost:3333/webhook" `
     -Method POST -Body '{"id":"12345","status":"approved","payment_method_id":"pix"}' `
     -ContentType "application/json"
   ```

---

### Problema: Painéis não atualizam

**Sintomas:**
- Hologramas mostram dados antigos

**Soluções:**
1. Verificar `config.yml`: `painel.refresh-segundos`
2. Forçar refresh: `/painel refresh`
3. Se DecentHolograms:
   - Verificar versão compatível
   - Verificar permissões do console
4. Se TextDisplay:
   - Verificar chunk carregado
   - Recriar painel

---

### Problema: Transferências bloqueadas

**Sintomas:**
- Erro: "Aguarde X segundos"

**Soluções:**
1. Verificar cooldown: `config.yml` → `limites.pagar.cooldown-segundos`
2. Verificar limite: `config.yml` → `limites.pagar.max-por-transacao`
3. Aguardar cooldown terminar
4. Admins podem resetar: `/econ unfreeze <jogador>`

---

## 📚 PARTE 17: Referências e Documentação Adicional

### Documentos do Projeto:

- **`STATUS_ATUAL.md`**: Estado atual do servidor
- **`MECANICAS.md`**: Mecânicas ativáveis
- **`COMANDOS.md`**: Lista completa de comandos
- **`ECONOMIA.md`**: Sistema econômico
- **`GUILDAS_TIMES.md`**: Times e guildas
- **`PAINEIS.md`**: Sistema de painéis
- **`AUDITORIA.md`**: Sistema de auditoria
- **`VANTAGENS_VIP.md`**: Benefícios VIP
- **`ROADMAP.md`**: Planejamento futuro

---

### APIs Externas:

**Mercado Pago API:**
- Docs: https://www.mercadopago.com.br/developers
- Criar pagamento: `POST /v1/payments`
- Consultar pagamento: `GET /v1/payments/{id}`
- Webhooks: https://www.mercadopago.com.br/developers/pt/docs/webhooks

**Spigot/Bukkit API:**
- Docs: https://hub.spigotmc.org/javadocs/spigot/
- Events: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/package-summary.html

**Vault API:**
- GitHub: https://github.com/MilkBowl/VaultAPI
- Docs: https://github.com/MilkBowl/Vault/wiki

---

## 🎓 PARTE 18: Melhores Práticas

### Desenvolvimento:

1. **Sempre testar localmente antes de deploy**
2. **Usar branches Git para features**
3. **Commitar com mensagens descritivas**
4. **Documentar mudanças importantes**
5. **Fazer backup do banco antes de updates**

---

### Operação:

1. **Monitorar logs regularmente**
2. **Fazer backup diário do banco de dados**
3. **Testar webhooks em ambiente de staging**
4. **Manter plugins de dependência atualizados**
5. **Usar Spark para profiling de performance**

---

### Segurança:

1. **Nunca commitar tokens/senhas no Git**
2. **Usar `.env` para secrets (Node.js)**
3. **Configurar firewall para proteger MySQL**
4. **Validar entrada de usuários**
5. **Limitar rate de APIs públicas**

---

## 🔮 PARTE 19: Roadmap Futuro

### Curto Prazo:
- ✅ Upgrade para Java 21
- 🔄 Sistema de níveis e progressão
- 🔄 GUIs visuais para comandos
- 🔄 Integração com Discord

### Médio Prazo:
- 🔄 Sistema de lojas integrado
- 🔄 Crates e recompensas
- 🔄 Cosméticos e partículas
- 🔄 API REST para consultas externas

### Longo Prazo:
- 🔄 Sistema de missões/quests
- 🔄 Minigames integrados
- 🔄 Ranking global
- 🔄 Dashboard web administrativo

---

## 📝 PARTE 20: Conclusão

Este projeto representa um **ecossistema completo** para servidores Minecraft, integrando:

✅ **Backend Node.js** para pagamentos reais  
✅ **Plugin Java modular** com 7 sistemas integrados  
✅ **Banco de dados robusto** (SQLite/MySQL)  
✅ **Auditoria completa** de eventos  
✅ **Sistema de painéis** com métricas em tempo real  
✅ **Integrações múltiplas** (Vault, DecentHolograms, etc.)  
✅ **Automação de deploy** via scripts PowerShell  

**Arquitetura:** Modular, escalável e bem documentada  
**Tecnologias:** Java 21, Node.js, Express, Maven, SQLite/MySQL  
**Padrões:** Services, Commands, Listeners, Event-driven  

---

## 📞 Suporte

Para questões técnicas, consulte:
1. Documentos em `backend/Plugin/docs/`
2. Logs em `backend/logs/` e `logs/latest.log`
3. Código-fonte com comentários
4. Esta documentação completa

---

**Documento gerado em:** 7 de dezembro de 2025  
**Versão do Plugin:** 0.1.0-SNAPSHOT  
**Java Runtime:** 21 LTS  
**Status:** ✅ Totalmente funcional e em produção
