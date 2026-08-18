# 🏠 Guia de Implantação Local (Backend Local + Frontend Vercel)

Este guia ensina como executar toda a infraestrutura pesada (Backend, Bancos de Dados e Filas) em um servidor local (sua própria máquina ou servidor de casa) e manter a interface do usuário (Frontend) hospedada gratuitamente na Vercel.

Essa arquitetura é ideal para quem quer economizar com nuvem, mas manter a performance e acessibilidade global do Frontend.

Para resolver a comunicação entre a nuvem (Vercel com HTTPS) e a sua rede local sem precisar abrir portas no seu roteador e configurar DDNS, usaremos o **Ngrok**.

---

## 🏗️ Passo 1: Preparar o Servidor Local

1. **Sistema Operacional Recomendado:** Linux (Ubuntu Server, Debian) ou Windows com WSL2 e Docker Desktop.
2. **Instalar Docker e Docker Compose:**
   - No Linux (Ubuntu), execute:
     ```bash
     sudo apt update && sudo apt upgrade -y
     curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh
     sudo usermod -aG docker $USER
     ```
   *(Importante: Faça logoff e login novamente na sua sessão para que as permissões do Docker entrem em vigor sem a necessidade do `sudo`).*

---

## ⚙️ Passo 2: Clonar o Projeto e Configurar as Variáveis

1. Baixe o código-fonte para o seu servidor:
   ```bash
   git clone <URL_DO_SEU_REPOSITORIO> PROMPTUARIO_2026
   cd PROMPTUARIO_2026
   ```

2. **Configurar o Backend:**
   Navegue até a pasta `backend` e crie o arquivo de variáveis de ambiente baseando-se no exemplo:
   ```bash
   cd backend
   cp .env.example .env
   ```
   *Abra o arquivo `.env` e ajuste o que for necessário (senhas do banco, a chave `JWT_SECRET_KEY` e, principalmente, as chaves de API de IA como a `LLM_API_KEY`).*

---

## 🚀 Passo 3: Iniciar o Backend Local

Ainda dentro da pasta `backend`, inicie o ecossistema completo:

```bash
docker compose up -d
```

O Docker fará o download das imagens pesadas (Postgres, MongoDB, Redis, RabbitMQ, MinIO) e construirá os microsserviços do sistema (isso pode demorar alguns minutos na primeira vez). O processo principal da API (o **API Gateway**) ficará disponível na porta local `8000`.

*Nota de Segurança: Você não precisa (e não deve) abrir nenhuma dessas portas de banco de dados no firewall do roteador da sua casa.*

---

## 🌐 Passo 4: Expor a API com HTTPS via Ngrok

Como o Frontend na Vercel roda com segurança (HTTPS), ele bloqueará qualquer tentativa de comunicação com o seu IP local (ex: `http://192.168.x.x:8000`) devido ao bloqueio de *Mixed Content* dos navegadores. Para resolver isso, usaremos o Ngrok gratuito, que fornece um domínio com certificado SSL automático.

### 4.1 Criar a conta e o Domínio Fixo
1. Acesse [ngrok.com](https://ngrok.com) e crie uma conta gratuita.
2. No painel principal, vá no menu lateral esquerdo em **Cloud Edge** > **Domains**.
3. Clique no botão **"Create Domain"**. O Ngrok fornecerá um domínio gratuito estático para sua conta (exemplo: `urso-polar-feliz.ngrok-free.app`). Salve este link.

### 4.2 Instalar o Ngrok no Servidor Local
**No Ubuntu/Debian:**
```bash
curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
  | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
  && echo "deb https://ngrok-agent.s3.amazonaws.com buster main" \
  | sudo tee /etc/apt/sources.list.d/ngrok.list \
  && sudo apt update && sudo apt install ngrok
```

### 4.3 Autenticar e Iniciar o Túnel
Acesse a aba **Your Authtoken** no painel do Ngrok, copie seu token e rode no terminal do seu servidor:
```bash
ngrok config add-authtoken SEU_TOKEN_AQUI
```

Inicie o túnel vinculando seu novo domínio fixo à porta `8000` (API Gateway):
```bash
nohup ngrok http --domain=SEU_DOMINIO_AQUI.ngrok-free.app 8000 > /dev/null 2>&1 &
```
*Dica: O comando `nohup` e o `&` no final garantem que o Ngrok continue rodando "invisível" em segundo plano, mesmo que você feche a aba do terminal SSH.*

Sua API local agora está disponível com segurança para o mundo no endereço `https://SEU_DOMINIO_AQUI.ngrok-free.app`.

---

## 🎨 Passo 5: Conectar o Frontend na Vercel

O último passo é avisar o seu aplicativo na Vercel sobre o novo endereço da API que está apontando para a sua máquina.

1. Acesse sua conta na [Vercel](https://vercel.com) e selecione o projeto do seu Frontend.
2. Vá até a aba **Settings** > **Environment Variables**.
3. Localize e edite (ou adicione) a variável `VITE_API_BASE_URL`.
4. Coloque como valor o link HTTPS do Ngrok que você gerou, apontando para a rota de versão (se aplicável):
   - `https://SEU_DOMINIO_AQUI.ngrok-free.app/v1`
5. Clique em **Save**.
6. Vá na aba **Deployments**, clique nos três pontinhos ao lado do seu último deploy e escolha **Redeploy**. Isso é obrigatório para que a Vercel reescreva o site com o link correto embutido.

---

## 🛡️ Passo 6: Liberação de Portas (Firewall / Rede Local)

Embora o Ngrok já cuide do tráfego da API principal para a Vercel sem exigir que você mexa no seu roteador de internet, você provavelmente vai querer acessar os painéis administrativos do sistema a partir de **outros computadores da sua rede local** (como o seu notebook na mesma rede Wi-Fi).

Para que os outros computadores consigam acessar os serviços, é necessário liberar as portas no firewall interno do seu servidor Linux (ou Windows Firewall).

### 6.1 Liberação no Firewall Local (Exemplo usando UFW no Ubuntu)

Execute os comandos abaixo no servidor para liberar o acesso:

```bash
# Permite acesso à API diretamente na rede local (sem passar pelo Ngrok, se desejar)
sudo ufw allow 8000/tcp

# Painel do Grafana (Monitoramento de Métricas)
sudo ufw allow 3001/tcp

# Painel de Controle do RabbitMQ (Gestão de Mensagens e Filas)
sudo ufw allow 15672/tcp

# Console do MinIO (Visualização dos Arquivos e PDFs Salvos)
sudo ufw allow 9001/tcp

# Painel do Mailpit (Visualizador de E-mails de Teste)
sudo ufw allow 8025/tcp

# Rastreador Jaeger (Traces de Performance)
sudo ufw allow 16686/tcp

# Recarrega o firewall para aplicar as novas regras
sudo ufw reload
```

Após isso, se o IP do seu servidor na rede for `192.168.1.100`, você poderá acessar o Grafana pelo seu notebook digitando `http://192.168.1.100:3001`.

### 6.2 O que NÃO abrir no Roteador (Port Forwarding)
Caso decida no futuro expor esses serviços para a internet abrindo as portas do seu roteador da operadora, **nunca exponha as portas diretas dos bancos de dados**. Elas já funcionam em rede isolada interna do Docker. Mantenha estas portas bloqueadas para o mundo externo:
- `5432` (Postgres)
- `27017` (MongoDB)
- `6379` (Redis)
- `5672` (RabbitMQ - tráfego interno)

---

## ✅ Conclusão

Pronto! Seu sistema está no ar com uma arquitetura dividida estrategicamente:
- **Poder Computacional e Dados:** Todo o processamento pesado, bancos de dados, uploads (MinIO) e mensagens ficam na sua máquina local, garantindo zero custo mensal de servidores de aplicação ou banco de dados em nuvem.
- **Túnel Seguro:** A comunicação da rua para a sua casa ocorre por um túnel altamente criptografado e não exige intervenções de porta/NAT no roteador de internet, protegendo sua rede doméstica.
- **Distribuição Global Rápida:** A interface web é servida pelo CDN da Vercel globalmente, oferecendo carregamento ultrarrápido aos clientes em qualquer lugar.
