# Painéis (HUDs no Mundo)

## Visão
Painéis informativos inspirados em hologramas no mundo, com três categorias: Global, Time, Guilda.

## Princípios Visuais
- Sem rotação inclinada via `Transformation` (evita distorções).
- Uso de `teleport(Location)` para posicionar com yaw.
- Texto centralizado e legível; sem brilhos/partículas excessivas.

## Categorias
- Global: status do servidor, top jogadores, online, eventos.
- Time: pontos e variação, jogadores do time.
- Guilda: líder, membros, Nexus/escudo, histórico recente.

## Persistência
- Arquivo JSON (ex.: `plugins/ServerMine/paineis/panels.json`).
- Load/Save em inicialização/desligamento.

## Comandos de Painel (Admin)
- `/painel criar <global|time|guilda> [nome]`
- `/painel deletar <id>`
- `/painel listar`
- `/painel refresh`
- `/painel clean`
- `/painel tp`
- `/painel seed-guilds [solar|lunar]`
- `/painel seed-all`
- `/painel info <id>`
- `/painel realign <id> [north|south|east|west]`

## Tab-Complete
- Tipos, times (Solar/Lunar), guildas existentes e IDs.

## Hooks de Atualização
- Mudança de time, criação/saída/expulsão de guilda.
- Eventos do Nexus/Escudo.
- Objetivos: gerar/concluir/remover.
- Tarefas periódicas de atualização.

## Diretrizes Técnicas
- Evitar brilho/sombra por padrão.
- Elementos de texto estáveis, offsets relativos ao yaw, sem rotação de entidades.
- Re-render seguro em realign.
