# Contexto da Conversa e Decisões do Projeto

Data: 2025-12-07

Este documento consolida o contexto da nossa conversa e as decisões já tomadas para que qualquer colaborador (como seu amigo) tenha uma visão clara do estado atual e das diretrizes.

## Visão Geral do Projeto
- Plugin: Nexus (Paper/Spigot 1.20.x, Java 17)
- Objetivo: Nexus como hub de sistemas — economia, times/guildas, painéis/hologramas, auditoria/observabilidade, com módulos ativáveis.
- Integrações:
  - Vault (obrigatório) + provedor de economia (EssentialsX Economy recomendado)
  - DecentHolograms (opcional) — hologramas dos painéis; fallback TextDisplay
  - AuthMe Reloaded (opcional) — hooks de login/logout

## Linha do Tempo (resumo)
- Scaffold inicial do plugin, comandos base e Vault.
- Persistência de jogador (join/quit) e auditoria em DB.
- Painéis com métricas (GLOBAL/TIME/GUILDA) e refresh.
- Transações em DB, comando de consulta/export.
- Módulos ativáveis no `config.yml`.
- Hook de AuthMe (login/logout) e bloqueio de movimento sem time.

## Funcionalidades Implementadas
- Economia: `saldo`, `pagar`, `historico`; `/econ` para freeze/status (admin).
- Times: escolha obrigatória (Solar/Lunar); bloqueio de movimento até escolher; cores no nome (Solar = amarelo `§e`, Lunar = roxo `§5`).
- Guildas: criar, convidar, entrar/sair, status.
- Painéis: criação/listagem/info/refresh; tipos GLOBAL/TIME/GUILDA; métricas via auditoria/transações; DH se presente, senão TextDisplay.
- Auditoria: eventos em `audit_events`; comando `auditoria listar/export`.
- Transações: registro em `transactions`; comando `/_transacoes listar/export` com filtros (minutos, limite, jogador, nota).
- Login: ciclo de jogador (join/quit) + hooks opcionais (SimpleLogin/AuthMe).

## Modularidade
- Config: `config.yml` → seção `modulos.*` controla registro de comandos/listeners/scheduler.
  - Módulos atuais: `economia`, `times`, `guildas`, `paineis`, `auditoria`, `transacoes`, `login`.
- Todos ativos por padrão.

## Padrões de UX
- Comandos em português, com tab-complete completo.
- Mensagens configuráveis em `config.yml`.
- Cores por Time:
  - Solar: `§e` (amarelo)
  - Lunar: `§5` (roxo)
- Painéis: renderização automática (DH se presente; senão TextDisplay).

## Decisões Técnicas
- DB: SQLite por padrão; HikariCP; MySQL opcional.
- Auditoria e Transações: escrita assíncrona; consultas por janela/limite/filtros.
- Painéis DH: uso de `create`, `setlocation <world> x y z`, `addline`; `refresh` recria e reposiciona (garante mundo/posição).

## Comandos (atuais)
- Economia: `saldo`, `pagar <jogador> <valor> [nota]`, `historico [minutos] [limite]`, `econ <freeze|unfreeze|status>`.
- Times: `time escolher <Solar|Lunar>`, `time trocar confirmar`.
- Guildas: `guild criar <nome>`, `guild convidar <jogador>`, `guild entrar`, `guild sair`, `guild status`.
- Painéis: `painel criar <global|time|guilda>`, `painel criar-guilda <nome>`, `painel listar`, `painel info <id>`, `painel deletar <id>`, `painel refresh`.
- Auditoria: `auditoria <listar|export>`.
- Transações: `/_transacoes <listar|export>`.

## Configuração (chaves relevantes)
- `painel.usar-decent-holograms: true|false` — usa DH se presente; fallback TextDisplay.
- `painel.refresh-segundos: 30` — intervalo de atualização.
- `painel.metricas.janela-minutos`, `painel.metricas.top` — janela e top N.
- `mensagens.time-cor-solar: "§e"`, `mensagens.time-cor-lunar: "§5"` — cores.
- `mensagens.time-escolha-obrigatoria` — prompt para escolher time.
- `storage.tipo: sqlite|mysql` e credenciais.
- `modulos.*` — liga/desliga cada mecânica.

## Testes Rápidos
- Time: entrar no servidor, tentar mover (bloqueia), executar `time escolher Solar/Lunar` (libera, colore nome).
- Painel: `painel criar global` → aparece holograma (DH) ou TextDisplay; `painel listar/info/refresh`.
- Auditoria: `auditoria listar` → `player.join`, `time.choose`, `panel.create`.
- Transações: `/_transacoes listar 60 100 [jogador] [nota]`; `export csv/json`.

## Troubleshooting
- Painéis DH não aparecem:
  - Verificar se DH está habilitado; checar console para erros dos comandos `dh`.
  - Conferir mundo/posição (`setlocation <world> x y z`).
  - Tentar `painel refresh`.
- Economia não responde:
  - Confirmar Vault + provedor (EssentialsX/CMI); verificar permissões/admin.

## Próximos Passos Priorizados
1. Auditoria intuitiva: saída legível com tempo relativo, ícones/cores, filtros simples.
2. Compatibilidade DecentHolograms por versão (ajustar comandos se necessário).
3. Economia completa: validações de limites/cooldown, histórico enriquecido.
4. GUIs iniciais: escolha de time e painéis básicos.

## Documentos Relacionados
- `docs/STATUS_ATUAL.md` — estado atual, comandos e componentes.
- `docs/MECANICAS.md` — módulos (mecânicas), estado e roadmap.

## Convenções
- Sem features “inventadas” fora do combinado: documentos refletem apenas o implementado ou o planejado explícito.
- Atualização contínua dos docs conforme novas entregas.
