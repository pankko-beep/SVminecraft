const express = require("express");
const path = require("path");
const cors = require("cors");
const app = express();

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

// Rota principal
app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

// API para consultar status do servidor Minecraft (exemplo)
app.get("/api/status", async (req, res) => {
  // Aqui você pode integrar com a API do backend ou consultar status do servidor
  res.json({
    online: true,
    jogadores: 42,
    maxJogadores: 100,
    versao: "1.20.4",
    ip: "play.svminecraft.com.br"
  });
});

// API para listar VIPs disponíveis
app.get("/api/vips", (req, res) => {
  res.json([
    {
      id: "vip_basic",
      nome: "VIP Básico",
      preco: 14.90,
      beneficios: [
        "Kit semanal de recursos",
        "Prefixo [VIP] no chat",
        "Acesso a /fly por 30min/dia",
        "2x XP em todas atividades"
      ]
    },
    {
      id: "vip_premium",
      nome: "VIP Premium",
      preco: 29.90,
      beneficios: [
        "Todos os benefícios do VIP Básico",
        "Kit diário de recursos premium",
        "Prefixo [PREMIUM] colorido",
        "/fly ilimitado",
        "3x XP em todas atividades",
        "Acesso a área VIP exclusiva"
      ]
    },
    {
      id: "vip_ultimate",
      nome: "VIP Ultimate",
      preco: 49.90,
      beneficios: [
        "Todos os benefícios do Premium",
        "Kit diário Ultimate",
        "Prefixo [ULTIMATE] animado",
        "5x XP em todas atividades",
        "Cosméticos exclusivos",
        "Acesso prioritário ao servidor"
      ]
    }
  ]);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🌐 Website rodando em http://localhost:${PORT}`);
});
