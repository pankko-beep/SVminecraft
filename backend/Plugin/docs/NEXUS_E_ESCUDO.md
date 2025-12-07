# Nexus e Escudo — Detalhamento Profundo

## Visão
O Nexus é o coração da guilda; o Escudo protege temporariamente o Nexus. Ambos integram economia, objetivos e painéis.

## Estados do Nexus
- Inexistente → Construído → Ativo → Em Ataque → Destruído.
- Transições disparam anúncios globais e atualizações de painéis.

## Construção
- Custo inicial em moeda/itens raros.
- Membros online mínimos e permissões adequadas.
- Cooldown pós-destruição antes de reconstruir.

## Upgrades do Nexus
- Níveis que concedem bônus (regen, pontos, capacidade de escudo).
- Custos progressivos por nível; limite por temporada.
- Requer materiais raros + moeda (sinks econômicos).

## Escudo da Guilda
- Fluxo: Ativação → Warmup → Fully-active → Expiração.
- Protege o Nexus no estado fully-active.
- Cooldown entre ativações e custo proporcional ao nível.

## Cerco e Defesa
- Condições mínimas de atacantes/defensores presentes.
- Dano ao Nexus contabilizado por janela de evento.
- Recompensas por defesa proporcionais à pressão.
- Antizerg: diminishing returns para grupos enormes.

## Pontuação e Recompensas
- Defesa bem-sucedida rende pontos extras ao time/guilda.
- Ataque bem-sucedido gera pontos/loot com caps anti-snowball.
- Bônus de “primeira destruição da temporada”.

## Painéis
- Guilda: líder, membros, nível do Nexus, estado do escudo, histórico recente.
- Global: Nexus ativos, alertas de cerco e destaques.

## Persistência e Auditoria
- Salvar estado, upgrades, escudo e histórico de ataques.
- Logs detalhados por transição com autores e timestamps.
- Rollback sob fraude/exploit comprovado.

## Antiexploit
- Bloqueio de ataques combinados (wash-fight) entre conluiados.
- Verificação de dano real e tempo em área.
- Cooldowns e limites de tentativas por guilda contra a mesma alvo.

## Integrações
- Objetivos de cerco/defesa com boss bar e recompensas próprias.
- Economia: custos de manutenção e upgrades drenam moeda.
- Painéis: atualizações imediatas em estados críticos.

## Diretrizes Técnicas
- Eventos assíncronos com debounces para evitar spam.
- Cálculo de dano com validação anti-fake.
- Hooks para atualizar painéis, economia e pontos.
- Testes de carga para eventos de grande escala.
