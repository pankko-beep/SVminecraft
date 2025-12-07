# Mecânicas do Nexus (Módulos Ativáveis)

Data: 2025-12-07

Este documento lista os blocos de funcionalidades (mecânicas) do Nexus e seu estado atual. Será atualizado conforme evoluímos o plugin. Apenas o que existe ou está explicitamente planejado é listado aqui.

## Como ativar/desativar
- Arquivo: `plugins/Nexus/config.yml`
- Seção: `modulos.*` (true/false)
- Ex.: `modulos.economia: true`

## Mecânicas Implementadas (ativas)
- **Economia**: saldo, pagamentos, congelamento, histórico; integra Vault.
- **Times**: escolha obrigatória (Solar/Lunar), bloqueio de movimento até escolher, cores no nome e mensagens.
- **Guildas**: criar, convidar, entrar/sair, status básico.
- **Painéis/Hologramas**: painéis GLOBAL/TIME/GUILDA com métricas; DecentHolograms (opcional) ou TextDisplay (fallback).
- **Auditoria/Transações**: logs em `audit_events` e transações bilaterais em `transactions`; listar/exportar.
- **Login/Autenticação**: hooks opcionais de SimpleLogin/AuthMe (login/logout); ciclo de jogador em join/quit.

## Em Evolução (próximas entregas)
- **Auditoria Intuitiva**: formatação amigável (tempo relativo, ícones/cores), agrupamentos e filtros simplificados.
- **Compatibilidade DH**: ajustes por versão (teleport/setlines) e robustez de criação/refresh.
- **Economia Completa**: validações de limites/cooldown, histórico enriquecido e mensagens.
- **GUIs Iniciais**: menus para escolha de time e painéis simples de economia/auditoria.
- **Desempenho/Robustez**: caching, filas assíncronas, índices extras e integração com Spark.

## Planejado (roadmap)
- **Progressão**: níveis, objetivos, conquistas.
- **PvP**: regras, penalidades, proteções.
- **Lojas/Comércio**: integração com shopkeepers e logs de compra/venda.
- **Votos e Crates**: NuVotifier e recompensas com auditoria.
- **Cosméticos**: partículas, títulos, visuais.
- **Observabilidade**: métricas agregadas (diárias/semanais) e export avançado.
- **Anti‑abuso/Segurança**: rate-limits e validações.
- **Modularidade Avançada**: inicialização desacoplada por módulo e flags detalhadas.

## Notas
- Módulos atuais no `config.yml`: `economia`, `times`, `guildas`, `paineis`, `auditoria`, `transacoes`, `login`.
- Para detalhes de comandos e estado atual, veja `docs/STATUS_ATUAL.md`.
