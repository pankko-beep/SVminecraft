// API Base URL
const API_URL = 'http://localhost:3000/api';

// Carregar status do servidor
async function loadServerStatus() {
    try {
        const response = await fetch(`${API_URL}/status`);
        const data = await response.json();
        
        const statusElement = document.getElementById('serverStatus');
        const playerCountElement = document.getElementById('playerCount');
        
        if (data.online) {
            playerCountElement.textContent = `${data.jogadores}/${data.maxJogadores}`;
        } else {
            statusElement.querySelector('.status-indicator').classList.remove('online');
            statusElement.querySelector('.status-indicator').style.background = '#f44336';
            playerCountElement.textContent = 'Offline';
        }
    } catch (error) {
        console.error('Erro ao carregar status:', error);
    }
}

// Carregar VIPs
async function loadVIPs() {
    try {
        const response = await fetch(`${API_URL}/vips`);
        const vips = await response.json();
        
        const vipsGrid = document.getElementById('vipsGrid');
        vipsGrid.innerHTML = '';
        
        vips.forEach((vip, index) => {
            const card = document.createElement('div');
            card.className = `vip-card ${index === 1 ? 'featured' : ''}`;
            
            card.innerHTML = `
                <h3 class="vip-name">${vip.nome}</h3>
                <div class="vip-price">
                    R$ ${vip.preco.toFixed(2)}
                    <span>/mês</span>
                </div>
                <ul class="vip-benefits">
                    ${vip.beneficios.map(b => `<li>${b}</li>`).join('')}
                </ul>
                <button class="btn-primary btn-large" onclick="comprarVIP('${vip.id}', '${vip.nome}', ${vip.preco})">
                    Comprar Agora
                </button>
            `;
            
            vipsGrid.appendChild(card);
        });
    } catch (error) {
        console.error('Erro ao carregar VIPs:', error);
    }
}

// Copiar IP do servidor
function copyIP() {
    const ip = 'play.svminecraft.com.br';
    navigator.clipboard.writeText(ip).then(() => {
        alert(`✅ IP copiado: ${ip}`);
    }).catch(err => {
        console.error('Erro ao copiar IP:', err);
    });
}

// Comprar VIP
function comprarVIP(id, nome, preco) {
    // Aqui você pode integrar com o backend de pagamentos
    alert(`🛒 Compra de ${nome} - R$ ${preco.toFixed(2)}\n\nEm breve você será redirecionado para o pagamento!`);
    
    // Exemplo de integração com backend de pagamentos:
    /*
    const nick = prompt('Digite seu nick no Minecraft:');
    const uuid = prompt('Digite seu UUID (opcional):');
    
    if (nick) {
        fetch('http://localhost:3333/criar-pix', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                valor: preco,
                nick: nick,
                uuid: uuid || 'uuid-temporario',
                vip: id
            })
        })
        .then(res => res.json())
        .then(data => {
            // Exibir QR Code do PIX
            console.log('PIX gerado:', data);
            // Abrir modal com QR Code
        });
    }
    */
}

// Tabs de Ranking
document.addEventListener('DOMContentLoaded', () => {
    // Carregar dados iniciais
    loadServerStatus();
    loadVIPs();
    
    // Atualizar status a cada 30 segundos
    setInterval(loadServerStatus, 30000);
    
    // Tabs de ranking
    const tabs = document.querySelectorAll('.ranking-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            const tabType = tab.dataset.tab;
            loadRanking(tabType);
        });
    });
});

// Carregar ranking
function loadRanking(type) {
    // Aqui você pode integrar com uma API real de rankings
    console.log(`Carregando ranking de ${type}`);
    
    // Exemplo de dados mockados
    const mockData = {
        moedas: [
            { nick: 'Player123', value: '150.000 moedas' },
            { nick: 'MegaBuilder', value: '125.000 moedas' },
            { nick: 'ProGamer99', value: '98.500 moedas' }
        ],
        nivel: [
            { nick: 'MaxLevel', value: 'Nível 85' },
            { nick: 'ProPlayer', value: 'Nível 72' },
            { nick: 'GamerPro', value: 'Nível 68' }
        ],
        kills: [
            { nick: 'PvPKing', value: '1.245 kills' },
            { nick: 'WarriorX', value: '987 kills' },
            { nick: 'Assassin', value: '756 kills' }
        ]
    };
    
    const rankingList = document.getElementById('rankingList');
    rankingList.innerHTML = '';
    
    mockData[type].forEach((player, index) => {
        const item = document.createElement('div');
        item.className = 'ranking-item';
        item.innerHTML = `
            <span class="rank">#${index + 1}</span>
            <img src="https://mc-heads.net/avatar/${player.nick}/32" alt="${player.nick}">
            <span class="player-name">${player.nick}</span>
            <span class="score">${player.value}</span>
        `;
        rankingList.appendChild(item);
    });
}

// Smooth scroll
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});
