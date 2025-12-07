# Economia — Regras Profundas

## Princípios
- Estabilidade: valor previsível; evitar inflação.
- Meritocracia: ganhos por esforço e atividades legítimas.
- Transparência: regras e custos claros; auditoria integral.
- Equidade: limites e impostos para reduzir abusos.

## Moeda e Itens
- Moeda principal: moedas Nexus.
- Itens de valor: diamantes e outros com tabelas de referência.

## Fontes (Sources)
- Objetivos dinâmicos (PVE/PVP/exploração/suporte) com pesos.
- Eventos programados com caps por jogador.
- Missões diárias/semanais com limites por período.
- Comércio honesto entre jogadores.

## Sumidouros (Sinks)
- Troca de time (1.000.000 moedas).
- Upgrades de Nexus e Escudo com custos progressivos.
- Taxas de serviço (teleportes especiais, anúncios, corretagem).
- Mercado/leilão: taxa de listagem e de venda (5–10%).
- Manutenção/reparos por temporada.

## Prevenção de Inflação
- Tetos de recompensa/dia/semana por jogador.
- Cooldowns por tipo de objetivo.
- Imposto progressivo em transações altas e saldos extremos.
- Preços de referência para conversão item↔moeda.
- Ajustes sazonais baseados em métricas.

## Controles e Limites
- Rate limits em pagar/sacar/depositar.
- Limites por transação (ex.: 500k) e por período (ex.: 2M/dia).
- Verificação de origem e padrões suspeitos (análise de cadeia).
- Whitelist de itens elegíveis à conversão.

## Trocas entre Jogadores
- Contratos seguros com confirmação dupla.
- Taxa de corretagem pequena para sink constante.
- Anti-scam: logs completos e possibilidade de rollback pela staff.
- Escrow opcional para valores altos.

## Auditoria e Logs
- Trilha completa: quem/quando/quanto/por quê.
- Alertas de picos anômalos, wash trading, conluio.
- Painéis internos: inflação, Gini, top saldos, volume.

## Punições e Fraudes
- Exploits/dupes/macros abusivos: congelamento, perda de ganhos, ban.
- Conluio/price-fixing: multas, suspensão, ban reincidente.
- Lavagem: anulada com punição progressiva.

## Balanceamento Contínuo
- Seasonal tuning: caps, taxas, recompensas revisadas por temporada.
- Elasticidade: ajustes dinâmicos com dados reais.
- Feedback da comunidade e testes A/B.

## Comandos (Exemplos)
- `/_saldo` — mostra saldo.
- `/_pagar <jogador> <quantia>` — transfere com taxa/limites.
- `/_sacar <quantia>` / `/_depositar <quantia>` — movimentações com cooldown.
- `/_mercado listar <item> <preço>` — taxa de listagem; vendas com taxa.
- `/_historico` — extrato recente.
- Admin: `/_econ freeze`, `/_econ audit`, `/_econ rollback`.

## Diretrizes de Implementação
- Operações atômicas e idempotentes.
- Anti-doublespend: locks e verificação de saldo.
- Precisão decimal consistente (BigDecimal ou long centavos).
- Tratamento de falhas com reprocessamento seguro.
