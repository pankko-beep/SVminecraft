require("dotenv").config();
const express = require("express");
const crypto = require("crypto");
const fs = require("fs");
const app = express();

const {
  salvarPedido,
  jaFoiProcessado,
  marcarComoProcessado,
  ativarVipArquivo,
} = require("./utils/database");

// ===============================
// PARSE DE QUALQUER BODY
// ===============================
app.use(express.json()); // JSON
app.use(express.urlencoded({ extended: true })); // URL-encoded
app.use(express.text({ type: "*/*" })); // Qualquer outro tipo

// ===============================
// FETCH
// ===============================
const fetch = (...args) =>
  import("node-fetch").then(({ default: fetch }) => fetch(...args));

// ===============================
// ROTA TESTE
// ===============================
app.get("/", (req, res) => res.send("✅ Servidor online"));

// ===============================
// CRIAR PAGAMENTO PIX
// ===============================
app.post("/criar-pix", async (req, res) => {
  try {
    const { valor, nick, uuid, vip } = req.body;
    if (!valor || !nick || !uuid || !vip)
      return res.status(400).json({ error: "Dados incompletos" });

    const orderId = crypto.randomUUID();
    salvarPedido({ orderId, nick, uuid, vip, valor, status: "pending", data: new Date().toISOString() });

    const response = await fetch("https://api.mercadopago.com/v1/payments", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${process.env.MP_ACCESS_TOKEN}`,
        "Content-Type": "application/json",
        "X-Idempotency-Key": `${Date.now()}`,
      },
      body: JSON.stringify({
        transaction_amount: valor,
        description: `VIP ${vip} para ${nick}`,
        payment_method_id: "pix",
        payer: { email: "comprador@email.com" },
        notification_url: `${process.env.NGROK_URL}/webhook`,
        metadata: { nick, uuid, vip, servidor: "SVminecraft" },
      }),
    });

    const data = await response.json();
    console.log("✅ PIX GERADO:", data.id);
    res.json(data);
  } catch (err) {
    fs.appendFileSync("./logs/erros.log", err + "\n");
    res.status(500).json({ error: "Erro ao criar pagamento" });
  }
});

// ===============================
// WEBHOOK MERCADO PAGO / TESTES
// ===============================
app.post("/webhook", async (req, res) => {
  try {
    // 🔹 Tratar qualquer tipo de corpo
    let body = req.body;
    if (typeof body === "string") {
      try {
        body = JSON.parse(body);
      } catch {
        console.log("⚠️ Corpo não JSON:", body);
        body = {};
      }
    }
    console.log("💬 Webhook recebido:", body);

    // 🔹 Obter paymentId
    const paymentId = body?.data?.id || body?.id || body?.paymentId;
    if (!paymentId) {
      console.log("⚠️ Webhook sem paymentId válido");
      return res.sendStatus(200); // não dá 400, apenas ignora
    }

    // 🔹 Evitar duplicidade
    if (jaFoiProcessado(paymentId)) {
      console.log("⚠️ Pagamento duplicado:", paymentId);
      return res.sendStatus(200);
    }

    // 🔹 Buscar dados completos do pagamento (Mercado Pago)
    let payment = body; // default para testes via PowerShell
    if (body.type === "payment" || body.action?.startsWith("payment.")) {
      const response = await fetch(`https://api.mercadopago.com/v1/payments/${paymentId}`, {
        headers: { Authorization: `Bearer ${process.env.MP_ACCESS_TOKEN}` },
      });
      payment = await response.json();
    }

    // 🔹 Só processar pagamentos aprovados PIX
    if (payment.status === "approved" && payment.payment_method_id === "pix") {
      marcarComoProcessado(paymentId);

      const { nick, uuid, vip } = payment.metadata || {};
      ativarVipArquivo({
        nick,
        uuid,
        vip,
        payment_id: paymentId,
        valor: payment.transaction_amount || 0,
        data: new Date().toISOString(),
      });

      fs.appendFileSync(
        "./logs/webhook.log",
        `APROVADO: ${nick} - ${vip} - ${paymentId}\n`
      );
      console.log(`✅ VIP SALVO: ${nick} -> ${vip}`);
    }

    res.sendStatus(200);
  } catch (err) {
    console.log("❌ Erro webhook:", err);
    fs.appendFileSync("./logs/erros.log", err + "\n");
    res.sendStatus(500);
  }
});

// ===============================
const PORT = 3333;
app.listen(PORT, () => console.log(`✅ Servidor rodando na porta ${PORT}`));
