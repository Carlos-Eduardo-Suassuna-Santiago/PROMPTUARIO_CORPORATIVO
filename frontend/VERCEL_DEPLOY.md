# Deploy do frontend na Vercel

O frontend deve ser publicado na Vercel. O backend continua sendo executado na AWS.

## Configuração do projeto na Vercel

- **Root Directory:** `frontend`
- **Framework Preset:** Vite
- **Build Command:** `npm run build`
- **Output Directory:** `dist`
- **Variable de ambiente de produção:**

```text
VITE_API_BASE_URL=https://api.promptuario.health/api/v1
```

O arquivo `vercel.json` mantém o fallback do React Router para `index.html`.

## Configuração necessária na AWS

No ambiente do API Gateway, configure `FRONTEND_URL` com o domínio gerado pela Vercel e, se houver domínio próprio, inclua os dois valores separados por vírgula:

```text
FRONTEND_URL=https://seu-projeto.vercel.app,https://app.seudominio.com
```

Depois, recrie o gateway para aplicar o CORS:

```bash
docker compose up -d --build gateway
```

Não é necessário executar o container `frontend` na AWS para o deploy da Vercel. O `frontend/docker-compose.yml` continua disponível apenas para desenvolvimento/testes locais.
