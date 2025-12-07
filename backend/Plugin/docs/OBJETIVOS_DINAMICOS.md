# Objetivos Dinâmicos e Sistema de Recompensas

## Categorias
- PVE: chefes, hordas, coleta dirigida.
- PVP: duelos, capturas, defesa/ataque.
- Exploração: descobertas, marcos geográficos, artefatos.
- Suporte: craft, logística, cura, buffs.

## Estrutura de Objetivo
- Atributos: nível, duração, requisitos, critérios de conclusão.
- Estados: Gerado → Ativo → Concluído → Arquivado.
- Timers, boss bars e mensagens de progresso.

## Recompensas
- Moeda base + itens por raridade + pontos do time + reputação da guilda.
- Bônus por dificuldade, tempo e contribuição efetiva.

## Distribuição Justa
- Partição por participação (dano/cura/objetivos secundários).
- Mínimo de esforço exigido; bloqueio a “AFK carry”.
- Evita last-hit: usa métricas agregadas.

## Caps e Cooldowns
- Limites por jogador/dia/semana.
- Cooldown por categoria; incentiva diversidade.
- Multiplicadores rotativos (objetivos em destaque).

## Tabelas de Loot
- Tiers: comum, incomum, raro, épico, mítico.
- Pity-count para itens de alta raridade.
- Preços de referência para economia saudável.

## Integrações
- Atualiza painéis (global/time/guilda).
- Gatilhos para Nexus/Escudo em cerco/defesa.
- Escrita em logs de economia.

## Antifraude
- Validação de combate real: tempo e dano consistentes.
- Detecção de farms em cadeia e wash-fight.
- Rollback de recompensas suspeitas.

## Telemetria e Balanceamento
- Métricas: tempo médio de conclusão, taxa de participação, inflação de loot.
- Ajustes sazonais e testes A/B.

## Diretrizes Técnicas
- Processamento idempotente de recompensas.
- Locks por jogador/objetivo durante pagamento.
- Reemissão segura em caso de falha.
