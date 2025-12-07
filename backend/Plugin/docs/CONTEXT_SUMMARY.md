# Nexus — Contexto Consolidado (Resumo)

Este arquivo resume e interliga as regras e diretrizes do servidor Nexus para referência rápida durante o desenvolvimento.

## Visão Geral
- Servidor com foco em Times (Solar/Lunar), Guildas, Nexus/Escudo, Objetivos dinâmicos, Economia com auditoria, Painéis HUD, e VIPs equilibrados (não pay-to-win).
- Desenvolvimento por fases (ROADMAP):
  - Fase 1: plugin mínimo (times, economia básica, guilda básica, painéis estáveis, /fly admin).
  - Fase 2: Nexus/Escudo + objetivos dinâmicos com recompensas.
  - Fase 3: economia avançada, mercado/leilão, auditoria visual.
  - Fase 4: eventos sazonais, balanceamentos e telemetria.

## Times e Guildas
- Times: Solar e Lunar; escolha única; troca custa 1.000.000 moedas.
- Guildas: líder, oficiais (opcional), membros; criação/convite/entrada/saída/expulsão.
- Governança: impeachment e eleições com quórum e maioria simples.
- Integração: pontos por time, fundos de guilda, custos de upgrades/escudo com cofre da guilda.
- Auditoria interna: logs de membros e gastos do cofre.

## Nexus e Escudo
- Estados Nexus: Inexistente → Construído → Ativo → Em Ataque → Destruído (com anúncios e atualização de painéis).
- Upgrades: níveis com bônus (regen, pontos, capacidade de escudo), custos progressivos, limite por temporada.
- Escudo: Ativação → Warmup → Fully-active → Expiração; cooldown e custo proporcional ao nível.
- Cerco/Defesa: requisitos mínimos, contagem de dano por janelas, antizerg, recompensas por defesa e ataque com caps.
- Persistência/Auditoria: salvar estados e histórico; logs detalhados; rollback sob fraude.

## Objetivos Dinâmicos
- Categorias: PVE, PVP, Exploração, Suporte.
- Estrutura: nível, duração, requisitos, critérios; estados Gerado → Ativo → Concluído → Arquivado; timers e boss bars.
- Recompensas: moeda + itens por raridade + pontos de time + reputação de guilda; bônus por dificuldade/tempo/contribuição.
- Distribuição justa: por participação (dano/cura/objetivos), anticarry AFK, anti last-hit.
- Caps/cooldowns por jogador e categoria; multiplicadores rotativos; pity-count em loot; preços de referência.
- Antifraude: combate real, farms em cadeia, wash-fight, rollback seguro.

## Economia
- Moeda: moedas Nexus; itens de valor (diamantes etc.) com referência de preços.
- Fontes: objetivos dinâmicos, eventos, missões, comércio honesto.
- Sumidouros: troca de time, upgrades Nexus/Escudo, taxas (teleporte, anúncios, corretagem), mercado (listagem e venda 5–10%), manutenção.
- Anti-inflação: tetos por período, cooldowns, imposto progressivo, whitelist de itens, ajustes sazonais.
- Controles: rate limits, limites por transação e período, verificação de origem, análise de cadeia.
- Auditoria: trilha completa (quem/quando/quanto/porquê), alertas anômalos, painéis internos (inflação, Gini, top saldos, volume).
- Punições: congelamento, confisco/rollback, ban conforme gravidade; conluio e lavagem punidos.
- Implementação: operações atômicas/idempotentes, anti-doublespend, precisão decimal (BigDecimal/long), reprocessamento seguro.

## Painéis (HUD)
- Categorias: Global, Time, Guilda.
- Princípios visuais: sem rotação inclinada; posicionamento por teleport com yaw; texto legível sem brilhos/partículas excessivas.
- Persistência: JSON em `plugins/ServerMine/paineis/panels.json`.
- Comandos admin: criar/deletar/listar/refresh/clean/tp/seed/realign/info.
- Hooks: mudanças em time/guilda, eventos do Nexus/Escudo, objetivos, tarefas periódicas.

## VIP (Equilíbrio)
- Níveis: Guerreiro (Bronze), Lorde (Prata), Mago (Ouro).
- Benefícios: boosts de XP/moedas, recompensa diária VIP, slots de loja, kits semanais, night vision, prefixo, prioridade de entrada, partículas, área VIP (Mago).
- Multiplicadores nas recompensas BASE (Top 3 e Times): Guerreiro 1.5x, Lorde 2.0x, Mago 2.5x.
- Recompensa diária adicional: 5k/12k/25k via `/vip recompensa` (1x a cada 24h).
- Regras especiais: Top 3 por categoria com desambiguação aleatória quando um jogador aparece em múltiplas categorias.

## Comandos Principais
- Jogador: `/_saldo`, `/_pagar <jogador> <quantia>`, `/_historico`, `/_time escolher <Solar|Lunar>`, `/_time trocar`, `/_guild criar <nome>`, `/_guild convidar <jogador>`, `/_guild aceitar`, `/_guild sair`.
- Admin: `/_fly`, `/_econ freeze|audit|rollback`, `/painel ...`, `/_nexus set <guild> <estado>`, `/_escudo set <guild> <estado>`.
- Permissões exemplo: `paineis.admin`, `nexus.admin`, `economia.admin`, `server.fly`.

## Regras Gerais, Penalidades e Auditoria
- Conduta: respeito, jogo limpo, sem exploits/cheats, sem toxicidade, sem doxing/phishing.
- Penalidades: advertência → mute → suspensão → ban; confisco/rollback; apelações com evidências.
- Auditoria: logs detalhados; alertas; retenção por temporada; processos de investigação com direito de resposta.

## Diretrizes Técnicas
- Código simples/testável; documentação em `docs/` atualizada; nomenclatura estável; releases versionadas.
- Eventos assíncronos com debounces; cálculo de dano validado; hooks para painéis/economia/pontos; testes de carga.
- Idempotência e locks onde necessário; precisão decimal; reemissão segura em falhas; anti-doublespend.

## Próximos Passos (Fase 1)
- Inicializar projeto do plugin (ex.: Paper/Spigot) com `pom.xml`/`gradle`, `plugin.yml`, classe principal.
- Implementar `/_fly` (admin) e um `/painel` mínimo sem distorções.
- Esqueleto de times/guilda/economia para testes.
- Provar build e deploy local.

---
Este resumo consolida e referencia: `REGRAS_GERAIS.md`, `GUILDAS_TIMES.md`, `ECONOMIA.md`, `NEXUS_E_ESCUDO.md`, `OBJETIVOS_DINAMICOS.md`, `PAINEIS.md`, `COMANDOS.md`, `PENALIDADES_AUDITORIA.md`, `ROADMAP.md`, `VANTAGENS_VIP.md`. 
