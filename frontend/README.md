# Website SVminecraft

Site oficial do servidor SVminecraft.

## 🚀 Como Rodar

### Desenvolvimento Local

```powershell
cd frontend
npm install
npm start
```

O site estará disponível em: `http://localhost:3000`

### Produção

```powershell
npm install
npm start
```

## 📁 Estrutura

```
frontend/
├── public/
│   ├── index.html      # Página principal
│   ├── css/
│   │   └── style.css   # Estilos
│   └── js/
│       └── main.js     # JavaScript
├── server.js           # Servidor Express
└── package.json        # Dependências
```

## 🔗 APIs Disponíveis

### GET /api/status
Retorna status do servidor Minecraft

**Resposta:**
```json
{
  "online": true,
  "jogadores": 42,
  "maxJogadores": 100,
  "versao": "1.20.4",
  "ip": "play.svminecraft.com.br"
}
```

### GET /api/vips
Lista planos VIP disponíveis

**Resposta:**
```json
[
  {
    "id": "vip_basic",
    "nome": "VIP Básico",
    "preco": 14.90,
    "beneficios": ["..."]
  }
]
```

## 🎨 Customização

### Cores (CSS Variables)

Edite em `public/css/style.css`:

```css
:root {
    --primary-color: #4CAF50;
    --secondary-color: #2196F3;
    --dark-bg: #1a1a1a;
    --light-bg: #2d2d2d;
}
```

### Conteúdo

Edite `public/index.html` para alterar textos, seções e estrutura.

## 🔌 Integração com Backend

Para integrar com o backend de pagamentos (`backend/server.js`):

1. Atualize a URL da API em `public/js/main.js`:
```javascript
const BACKEND_API = 'http://localhost:3333';
```

2. Descomente a função `comprarVIP()` para usar a API real

## 🌐 Deploy

### Opção 1: Heroku
```bash
heroku create svminecraft-website
git push heroku main
```

### Opção 2: Vercel
```bash
vercel deploy
```

### Opção 3: Servidor Próprio
```bash
# Copiar arquivos para servidor
scp -r frontend/* user@server:/var/www/html/

# Rodar com PM2
pm2 start server.js --name "svminecraft-site"
```

## 📝 Notas

- Backend de pagamentos roda na porta **3333**
- Website roda na porta **3000**
- Sem conflitos entre os servidores
- Pode rodar ambos simultaneamente
