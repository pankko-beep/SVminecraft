# Comandos e Permissões

## Jogador
- `/_saldo` — consulta saldo.
- `/_pagar <jogador> <quantia>` — transfere com taxa/limites/cooldown.
- `/_historico` — últimas transações.
- `/_time escolher <Solar|Lunar>` — escolha única.
- `/_time trocar` — solicita troca (custo 1.000.000 moedas).
- `/_guild criar <nome>` — cria guilda.
- `/_guild convidar <jogador>` — envia convite.
- `/_guild aceitar` — entra na guilda.
- `/_guild sair` — saída com cooldown.

## Admin
- `/_fly` — alterna voo para admins.
- `/_econ freeze <jogador>` — congela economia do jogador.
- `/_econ audit <jogador>` — auditoria de transações.
- `/_econ rollback <id>` — reverte transação.
- `/painel ...` — comandos de painel (ver PAINEIS.md).
- `/_nexus set <guild> <estado>` — força estado do Nexus.
- `/_escudo set <guild> <estado>` — força estado do Escudo.

## Permissões (exemplos)
- `paineis.admin` — gerenciar painéis.
- `nexus.admin` — manipular Nexus/Escudo.
- `economia.admin` — auditoria e rollback.
- `server.fly` — usar `/_fly`.

## Boas Práticas
- Mensagens claras e traduzidas.
- Tab-complete para todos os comandos.
- Limites e validações de entrada robustos.
