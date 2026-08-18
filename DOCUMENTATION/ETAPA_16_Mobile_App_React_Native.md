# 📱 Passo a Passo: Criação do App Mobile do PROMPTUÁRIO

Como o ecossistema do **PROMPTUÁRIO** já utiliza **React** e **TypeScript** no frontend web, a escolha mais natural, produtiva e escalável para o aplicativo mobile é o **React Native**, utilizando o **Expo**. Isso permitirá o reaproveitamento de conhecimento, bibliotecas e até mesmo lógica de negócios.

Este documento detalha o passo a passo para a criação e integração do App Mobile (Android e iOS) com a arquitetura de microsserviços existente.

---

## 1. Escolha da Stack Mobile

| Tecnologia | Motivo |
| :--- | :--- |
| **React Native (com Expo)** | Permite construir para iOS e Android com o mesmo código. O Expo facilita o setup, build e deploy nas lojas. |
| **TypeScript** | Mantém a tipagem rigorosa já utilizada no Web e Backend. |
| **Zustand** | Gerenciamento de estado leve para autenticação e sessão do usuário no app. |
| **TanStack Query (React Query)** | Cache e sincronização de dados com as APIs FastAPI. |
| **NativeWind (Tailwind CSS)** | Permite usar as mesmas classes Tailwind do web no mobile, facilitando a consistência visual. |
| **Axios** | Para requisições HTTP aos microsserviços via API Gateway. |
| **React Navigation** | Roteamento e navegação fluida por telas e abas nativas. |

---

## 2. Configuração Inicial e Criação do Projeto

**Passo 2.1:** Certifique-se de ter o Node.js instalado.
**Passo 2.2:** Crie o projeto utilizando o template do Expo com TypeScript e roteamento baseado em arquivos (Expo Router).

```bash
npx create-expo-app@latest promptuario-mobile -t expo-template-blank-typescript
cd promptuario-mobile
```

**Passo 2.3:** Instale as dependências fundamentais:
```bash
npx expo install react-native-safe-area-context react-native-screens expo-status-bar expo-secure-store
npm install axios zustand @tanstack/react-query react-native-svg
```

**Passo 2.4:** Configuração do NativeWind (Tailwind):
```bash
npm install nativewind
npm install --save-dev tailwindcss
npx tailwindcss init
```
*Configure o `tailwind.config.js` e o `babel.config.js` conforme a documentação do NativeWind para suportar as classes do Tailwind.*

---

## 3. Estruturação de Pastas do App

Adote uma arquitetura limpa (Clean Architecture) similar ao frontend, baseada em *features*.

```text
promptuario-mobile/
├── src/
│   ├── api/            # Configuração do Axios apontando para o API Gateway
│   ├── assets/         # Imagens, fontes e ícones nativos
│   ├── components/     # Componentes de UI genéricos (Botões, Inputs, Cards)
│   ├── config/         # Variáveis de ambiente (URLs, chaves públicas)
│   ├── hooks/          # Custom Hooks (ex: useAuth)
│   ├── store/          # Store do Zustand (Estado Global)
│   ├── screens/        # Telas da Aplicação (divididas por domínio)
│   │   ├── auth/       # Login, Recuperar Senha
│   │   ├── patient/    # Listagem de Pacientes, Detalhes
│   │   ├── clinical/   # Prontuário, Evolução, Prescrições
│   │   └── ai/         # Interação com a IA (Chat/Assistente)
│   ├── navigation/     # Configuração do React Navigation (Rotas)
│   └── utils/          # Funções auxiliares (formatação de CPF, datas)
├── App.tsx             # Ponto de entrada do aplicativo
└── app.json            # Configurações do Expo (ícones, nome, pacotes)
```

---

## 4. Integração com a API e Segurança (JWT)

Como a arquitetura é baseada em microsserviços, o App se comunicará unicamente com o **API Gateway** (ETAPA 8).

**Passo 4.1:** Configure o cliente Axios base:
```typescript
// src/api/client.ts
import axios from 'axios';
import * as SecureStore from 'expo-secure-store';

export const api = axios.create({
  baseURL: 'https://api.promptuario.com/v1', // URL do API Gateway
  timeout: 10000,
});

// Interceptor para adicionar o token JWT
api.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```
*Nota:* No mobile, utilizamos o `expo-secure-store` para armazenar o JWT com segurança (Keychain no iOS e Keystore no Android), ao invés de `localStorage`.

---

## 5. Implementação das Principais Telas e Fluxos

1. **Autenticação (IAM Service):**
   - Tela de Login recebendo e-mail e senha.
   - Chamada ao endpoint `/iam/auth/login`.
   - Salvamento do JWT no `SecureStore` e atualização do estado global no Zustand.

2. **Dashboard / Home:**
   - Exibição de métricas rápidas (ex: consultas do dia).
   - Menu inferior de navegação (Bottom Tabs) para acessar Pacientes, Agenda e Prontuários.

3. **Prontuário Eletrônico (Patient & Clinical Services):**
   - Integração com a câmera do dispositivo nativo (`expo-camera`) para escanear documentos ou adicionar fotos à evolução do paciente.
   - Listagem em modo offline-first (usando React Query para cache persistente).

4. **Assistente IA (AI Service):**
   - Interface em formato de Chat interativo.
   - Recurso de "Voice-to-Text" nativo para o médico ditar a evolução, enviando o texto para o `ai-service` organizar no formato SOAP.

---

## 6. Testes em Dispositivos Físicos

Durante o desenvolvimento, utilize o aplicativo **Expo Go** (disponível na App Store e Google Play).
Basta rodar o comando:
```bash
npx expo start
```
E escanear o QR Code gerado no terminal com o celular. O aplicativo atualizará em tempo real a cada salvamento (Fast Refresh).

---

## 7. Build e Deploy (CI/CD) nas Lojas

Para gerar os aplicativos finais e submeter às lojas (Google Play e Apple App Store), utilizaremos o **EAS Build** (Expo Application Services).

**Passo 7.1:** Instale o EAS CLI:
```bash
npm install -g eas-cli
```

**Passo 7.2:** Configure o projeto no EAS:
```bash
eas build:configure
```

**Passo 7.3:** Geração de Builds:
- Para Android (APK/AAB):
  ```bash
  eas build --platform android
  ```
- Para iOS (IPA):
  ```bash
  eas build --platform ios
  ```

**Passo 7.4 (Pipeline de Deploy):**
No GitHub Actions, crie um workflow para que a cada merge na branch `main`, o Github Actions acione o EAS Build e envie automaticamente a atualização via "Over The Air" (OTA Updates) ou publique diretamente nas lojas (EAS Submit).
