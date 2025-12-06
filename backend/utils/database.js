const fs = require("fs");
const path = require("path");

const pedidosPath = path.join(__dirname, "../database/pedidos.json");
const pagamentosPath = path.join(__dirname, "../database/pagamentos.json");
const vipsPath = path.join(__dirname, "../database/vips_ativos.json");

// =========================
// FUNÇÕES GENÉRICAS
// =========================
function lerArquivo(caminho) {
  if (!fs.existsSync(caminho)) return [];
  return JSON.parse(fs.readFileSync(caminho, "utf-8") || "[]");
}

function salvarArquivo(caminho, dados) {
  fs.writeFileSync(caminho, JSON.stringify(dados, null, 2));
}

// =========================
// PEDIDOS
// =========================
function salvarPedido(pedido) {
  const pedidos = lerArquivo(pedidosPath);
  pedidos.push(pedido);
  salvarArquivo(pedidosPath, pedidos);
}

// =========================
// PAGAMENTOS (ANTI DUPLICADO)
// =========================
function jaFoiProcessado(id) {
  const pagos = lerArquivo(pagamentosPath);
  return pagos.includes(id);
}

function marcarComoProcessado(id) {
  const pagos = lerArquivo(pagamentosPath);
  pagos.push(id);
  salvarArquivo(pagamentosPath, pagos);
}

// =========================
// VIPS ATIVOS
// =========================
function ativarVipArquivo(vipData) {
  const vips = lerArquivo(vipsPath);
  vips.push(vipData);
  salvarArquivo(vipsPath, vips);
}

module.exports = {
  salvarPedido,
  jaFoiProcessado,
  marcarComoProcessado,
  ativarVipArquivo,
};
