# Status Atual do Servidor

Data: 2025-12-07

Este documento descreve o estado atual do servidor e será atualizado conforme novas funcionalidades forem implementadas. Não inclui itens planejados; somente o que já existe no código.

## Visão Geral
- Plataforma: Spigot/Paper 1.20.x (Java 17)
- Plugin principal: Nexus
- Integrações:
  - Vault (Economia)
  - DecentHolograms (opcional, fallback para TextDisplay)
- Persistência:
  - Banco: SQLite (padrão) via HikariCP; suporte a MySQL configurável
  - Tabelas: `audit_events`, `transactions`
  - Arquivos YAML: dados de jogadores, times, guildas, paineis
- Auditoria: eventos gravados no DB (entradas/saídas, ações de painel, etc.)
- Paineis: tipos GLOBAL, TIME e GUILDA; atualização periódica; suporte a métricas via auditoria
 - Modularidade: módulos ativáveis em `config.yml` (`modulos.*`)

## Componentes Implementados
- Economia (Vault): saldo, pagar, congelar/descongelar e status
- Times: entrada/saída e consulta de status
- Guildas: criar, convidar, entrar, sair e status
- Painéis: criar (global/time/guilda), listar, deletar, info, refresh; métricas derivadas da auditoria e transações
- Auditoria: registro e consulta/exportação
- Transações: persistência no DB e consulta/exportação com filtros
 - Ciclo de Jogador: salvar dados em login/quit; hooks opcionais de SimpleLogin/AuthMe (login e logout)

## Comandos
(Comandos existentes e suas funções; somente o que está implementado)

- `saldo`
  - Função: mostra o saldo atual do jogador.

- `pagar <jogador> <valor> [nota]`
  - Função: transfere quantia para outro jogador; registra transação e auditoria.

- `historico [minutos] [limite]`
  - Função: mostra histórico pessoal de transações.

- `time <entrar|sair|status>`
  - Função: gerencia entrada/saída em times e consulta status.

- `guild <criar|convidar|entrar|sair|status>`
  - Função: gerencia guildas e consulta status.

- `fly [on|off]`
  - Função: alterna voo do jogador (requer permissão adequada).

- `econ <freeze|unfreeze|status>`
  - Função: congela/descongela economia de jogadores e consulta status (admin).

- `painel <criar|criar-guilda|deletar|listar|info|refresh>`
  - Função: gerencia paineis e exibe metadados.
  - Observação: `listar` exibe `guilda=<nome>` para paineis do tipo GUILDA.

- `auditoria <listar|export>`
  - Função: consulta eventos de auditoria e exporta (admin).

- `/_transacoes <listar|export>`
  - Função: consulta e exporta transações (admin).
  - Parâmetros:
    - `listar [minutos] [limite] [jogador] [nota-contendo]`
    - `export <csv|json> [minutos] [limite] [nota-contendo]`

## Exibição dos Painéis
- Com DecentHolograms: holograma multi-linhas com título e métricas; atualização periódica.
- Sem DecentHolograms: `TextDisplay` centralizado (billboard), sem fundo/sombra, mesmas linhas de texto; atualização periódica.

## Configuração
- `config.yml`
  - Define mensagens (prefixo), limites, e storage (SQLite/MySQL)
  - Intervalo de atualização de painéis
  - Módulos: `modulos.economia`, `modulos.times`, `modulos.guildas`, `modulos.paineis`, `modulos.auditoria`, `modulos.transacoes`, `modulos.login`

## Banco de Dados
- `audit_events`
  - Armazena: tipo, jogador, alvo, contexto e timestamp
- `transactions`
  - Armazena: id, from_uuid, to_uuid, amount, note, timestamp

## Observações
- Build: Maven gera `nexus-plugin-0.1.0-SNAPSHOT-shaded.jar` e `nexus-plugin-0.1.0-SNAPSHOT.jar` em `target/`.
- Permissões: comandos administrativos requerem permissões (ex.: `paineis.admin`).

## Diretrizes e Metas (Roadmap)
Estas metas orientam a consolidação do Nexus como hub. Não são funcionalidades já implementadas; serão adicionadas gradualmente.

- **Hub de Sistemas**: economia, progressão, PvP, lojas, votos, crates, cosméticos, hologramas e painéis; todos com logs em `audit_events` e transações bilaterais em `transactions`.
- **UX Aprimorada**: comandos em português claros, tab-complete completo, mensagens configuráveis, GUIs intuitivas, hologramas/lombadas visuais opcionais.
- **Robustez e Desempenho**: caching, tarefas assíncronas, índices no banco, filas de escrita, profiling com Spark, limites anti-abuso.
- **Observabilidade e Auditoria**: trilhas completas de ações, exportação e métricas.
- **Modularidade**: cada mecânica como módulo do Nexus, ativável/desativável em `config.yml`.

