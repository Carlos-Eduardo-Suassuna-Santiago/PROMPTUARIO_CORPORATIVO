# 📓 Diário de Bordo — Projeto Integrador Corporativo

> Registro cronológico das atividades, decisões técnicas e ocorrências do projeto.

---

## Índice

- [2026-11-15 — Reporting Service: Fase 1](#2026-11-15--reporting-service-fase-1)
- [2026-08-28](#2026-08-28)

---

## 2026-08-28

### 📋 Implementação Proposta

- Criação do Diário de Bordo do projeto (`DIARIO_DE_BORDO.md`) para registro estruturado das atividades diárias.

---

### ✅ O que foi Implementado

- Arquivo de diário de bordo criado com estrutura padrão para registro diário.
- Seções definidas: implementação proposta, o que foi implementado, erros encontrados, o que funcionou e resumo do dia.

---

### ❌ O que deu Erro

_Nenhum erro registrado neste dia._

---

### 🟢 O que Funcionou

- Estrutura do diário definida e aprovada pela equipe.

---

### 📝 Resumo do Dia

Dia dedicado à organização e padronização da documentação do projeto. O diário de bordo foi criado para garantir rastreabilidade das atividades, decisões técnicas e ocorrências ao longo do desenvolvimento do Projeto Integrador Corporativo 2026.

---

---

## 2026-11-15 — Reporting Service: Fase 1

### 📋 Implementação Proposta

- Criar o bootstrap do `backend-java/reporting-service` com Java 21 e Spring Boot 3.3.4, incluindo dependências, configurações de infraestrutura e pools de execução assíncrona.
- Validar a compilação Maven e registrar o resultado no diário de bordo.

---

### ✅ O que foi Implementado

- Criado `pom.xml` com coordenadas `br.edu.ifrn.promptuario:reporting-service`, Java 21 e Spring Boot 3.3.4.
- Incluídas as dependências para Web, JPA, PostgreSQL, Flyway, validação, segurança/OAuth2 Resource Server, RabbitMQ, Actuator/Prometheus, AWS SDK S3, Apache POI, OpenCSV, OpenPDF, Thymeleaf, Springdoc, Lombok e Testcontainers (PostgreSQL, RabbitMQ e LocalStack).
- Criado `application.yml` com porta 8000, conexão PostgreSQL `reporting_db`, Flyway, RabbitMQ, armazenamento S3/MinIO, configuração JWT HMAC-SHA256, endpoints Actuator e Swagger UI em `/docs`.
- Criada a classe principal `ReportingServiceApplication` com `@SpringBootApplication`, `@EnableAsync` e `@EnableScheduling`.
- Criada `AsyncConfig` com `reportTaskExecutor` (pool 4–8, fila de 50) e `reportTaskScheduler` dedicado aos agendamentos.

---

### ❌ O que deu Erro

- O comando `mvn` direto não está disponível no host. A primeira tentativa de executar o wrapper falhou porque o comando foi iniciado fora do diretório do wrapper.

---

### 🟢 O que Funcionou

- Java 21 está instalado e foi usado na validação.
- A compilação `clean compile` terminou com `BUILD SUCCESS` usando o Maven Wrapper existente em `backend-java/gateway/mvnw`, apontado para o POM do Reporting Service.
- Foram compiladas as duas classes Java iniciais sem erros.

---

### 📝 Resumo do Dia

O bootstrap da Fase 1 do Reporting Service foi criado com dependências e configurações alinhadas ao plano de migração. A compilação Maven foi validada com sucesso por meio do wrapper já existente no repositório, apesar de o executável `mvn` não estar instalado no host. O serviço está pronto para avançar à Fase 2, que criará o schema inicial do banco via Flyway.

---

<!-- TEMPLATE PARA NOVO DIA — Copie o bloco abaixo ao iniciar uma nova entrada -->

<!--
## YYYY-MM-DD

### 📋 Implementação Proposta

- Descreva aqui o que foi planejado para o dia.

---

### ✅ O que foi Implementado

- Liste tudo que foi efetivamente desenvolvido ou configurado.

---

### ❌ O que deu Erro

- Descreva os problemas encontrados, mensagens de erro relevantes e contexto.

---

### 🟢 O que Funcionou

- Liste o que funcionou como esperado.

---

### 📝 Resumo do Dia

Breve parágrafo com o balanço geral do dia, decisões tomadas e próximos passos.

---
-->
