# 📓 Diário de Bordo — Projeto Integrador Corporativo

> Registro cronológico das atividades, decisões técnicas e ocorrências do projeto.

---

## Índice

- [2026-11-15 — Reporting Service: Fase 5](#2026-11-15--reporting-service-fase-5)
- [2026-11-15 — Reporting Service: Fase 4](#2026-11-15--reporting-service-fase-4)
- [2026-11-15 — Reporting Service: Fase 3](#2026-11-15--reporting-service-fase-3)
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

## 2026-11-15 — Reporting Service: Fase 3

### 📋 Implementação Proposta

- Mapear os enums de relatórios, entidades JPA e repositórios Spring Data do `backend-java/reporting-service`.
- Configurar JSONB com Hibernate 6, datas de auditoria, geração textual de UUIDs e consultas agregadas para métricas e produtividade médica.
- Validar a compilação/testes Maven e registrar o resultado.

---

### ✅ O que foi Implementado

- Criados os enums `ReportType`, `OutputFormat` e `ReportStatus` em `model/enums`, persistidos como `VARCHAR` nas entidades.
- Criadas as entidades `ReportJob`, `ReportSchedule`, `DailyStats`, `WebhookConfig`, `WebhookDeliveryLog` e `ReportAuditLog`, alinhadas às tabelas da migração V1.
- Mapeados campos JSONB com `@JdbcTypeCode(SqlTypes.JSON)`, timestamps com `@CreationTimestamp`/`@UpdateTimestamp`, e IDs UUID textuais com `@PrePersist`.
- Adicionados construtores de domínio e construtores sem argumentos protegidos para uso do JPA; o log de auditoria foi marcado como imutável para impedir alterações posteriores.
- Criados repositórios para jobs, estatísticas diárias, agendamentos, configurações e logs de webhook e auditoria.
- Adicionadas consultas paginadas para jobs, consulta de schedules/webhooks ativos, agregação de métricas entre datas e produtividade agrupada por `entityId` (alias `doctorId`).

---

### ❌ O que deu Erro

- A primeira compilação identificou imports incorretos de `@UpdateTimestamp` como Jakarta Persistence; a anotação pertence ao Hibernate e os imports foram corrigidos antes da validação final.
- Não existem testes automatizados no módulo nesta etapa; `mvn test` compilou o código, mas não encontrou testes para executar.

---

### 🟢 O que Funcionou

- `clean test` finalizado com `BUILD SUCCESS` usando Java 21 e o Maven Wrapper do Gateway apontado para o POM do Reporting Service.
- As 17 fontes Java do módulo foram compiladas com sucesso.
- `git diff --check` concluiu sem apontar problemas de whitespace.

---

### 📝 Resumo do Dia

A camada de persistência da Fase 3 foi implementada para o Reporting Service, incluindo seis entidades, três enums e repositórios com consultas paginadas e agregadas. O mapeamento JSONB usa os tipos JDBC do Hibernate 6 e os nomes/tipos das colunas correspondem ao schema da Fase 2. A compilação Maven passou; a suíte não possui testes ainda, então a validação desta fase foi de compilação.

---

## 2026-11-15 — Reporting Service: Fase 4

### 📋 Implementação Proposta

- Criar os DTOs do Reporting Service como Java Records imutáveis com validações Jakarta.
- Preservar compatibilidade com o contrato legado/Frontend serializando propriedades em snake_case e validar a serialização.

---

### ✅ O que foi Implementado

- Criados os nove records: `ReportExportRequestDTO`, `ReportJobResponseDTO`, `DashboardSummaryDTO`, `DailyStatItemDTO`, `DoctorProductivityDTO`, `ScheduleCreateDTO`, `ScheduleResponseDTO`, `WebhookCreateDTO` e `WebhookResponseDTO`.
- Aplicada estratégia Jackson `SnakeCaseStrategy` aos DTOs, mantendo os nomes Java idiomáticos e emitindo propriedades como `report_type`, `total_consultations_today` e `download_url`.
- Adicionadas validações Jakarta para os campos obrigatórios e limites definidos; o webhook valida URL, tamanho de segredo (16–128) e descrição conforme o contrato legado.
- Mapas e listas opcionais recebem defaults seguros e cópias não modificáveis, evitando valores nulos e alterações externas após a criação dos records.
- Implementado `ReportJobResponseDTO.fromEntity(ReportJob, String)` para criar a resposta a partir da entidade e da URL pré-assinada.
- Criado teste de serialização para verificar enumeração e nomes snake_case do pedido e do resumo do dashboard.

---

### ❌ O que deu Erro

- Nenhum erro permaneceu. Durante a revisão do contrato, a validação de URL foi mantida compatível com URLs HTTP e HTTPS, como no `HttpUrl` do serviço Python.

---

### 🟢 O que Funcionou

- `clean test` concluiu com `BUILD SUCCESS` usando Java 21 e o Maven Wrapper do Gateway.
- As 26 fontes Java de produção e o teste foram compilados; o teste `DtoSerializationTest` passou (1 teste, 0 falhas, 0 erros).
- `git diff --check` concluiu sem apontar problemas de whitespace.

---

### 📝 Resumo do Dia

A Fase 4 do Reporting Service adicionou DTOs imutáveis em Java Records, com validações alinhadas às entradas do serviço e serialização snake_case compatível com o contrato FastAPI consumido pelo frontend. Também foi implementada a conversão de entidade de job para DTO e uma verificação automatizada de serialização. A compilação e o teste de contrato passaram.

---

## 2026-11-15 — Reporting Service: Fase 5

### 📋 Implementação Proposta

- Implementar os motores de exportação CSV, XLSX e PDF para o `reporting-service` usando OpenCSV, Apache POI e OpenPDF.
- Garantir estilo corporativo, compatibilidade com relatórios `FULL_SYSTEM`, paginação e proteção contra consumo excessivo de memória.
- Validar os formatos com testes automatizados e atualizar o diário de bordo.

---

### ✅ O que foi Implementado

- Criada a interface `ReportExporter` com contrato comum para exportação binária, content type e extensão.
- Criado `CsvReportExporter` usando OpenCSV, cabeçalho derivado das chaves do primeiro registro, escaping de campos e BOM UTF-8.
- Criado `ExcelReportExporter` com `SXSSFWorkbook`, janela de 100 linhas, compressão de arquivos temporários, limpeza dos temporários, cabeçalho azul petróleo `#1A6E6E`, bordas, zebra striping, autoajuste de colunas e formatação de datas/números.
- Para `FULL_SYSTEM`, o XLSX gera aba de resumo e abas específicas para as métricas disponíveis de consultas, pacientes, cancelamentos, prescrições e consultas por médico.
- Criado `PdfReportExporter` com OpenPDF, papel A4 retrato ou paisagem conforme a quantidade de colunas, cabeçalho com título/data/solicitante, tabela com cabeçalho repetido e rodapé com número atual/total de páginas.
- Adicionados limites defensivos comuns: no máximo 100.000 linhas, 200 colunas e 1.000.000 de células por exportação. As tabelas PDF são materializadas em blocos de até 500 linhas.
- Criados testes de CSV (BOM, escaping e seleção de colunas), XLSX (abas FULL_SYSTEM e tipos de células) e PDF (validade e paginação).

---

### ❌ O que deu Erro

- A primeira compilação indicou que o rastreamento de largura das colunas é específico de `SXSSFSheet`, não da interface `Sheet`; o tipo foi corrigido.
- A primeira compilação de teste encontrou uma API de extração de texto que não existe no OpenPDF 1.3.39 usado pelo projeto. O teste foi ajustado para validar a estrutura e paginação através de `PdfReader`.
- Uma limpeza de imports removeu por engano `Collections`, ainda necessário pelo helper de metadata; o import foi restaurado e a compilação/testes finais passaram.

---

### 🟢 O que Funcionou

- `clean test` finalizado com `BUILD SUCCESS` usando Java 21 e o Maven Wrapper do Gateway.
- Foram compiladas 31 fontes de produção e 2 fontes de teste; 4 testes passaram, sem falhas ou erros.
- `git diff --check` concluiu sem apontar problemas de whitespace.

---

### 📝 Resumo do Dia

A Fase 5 implementou os exportadores CSV, XLSX e PDF, com compatibilidade para relatórios completos em múltiplas abas no Excel. O uso de workbook streaming, processamento em blocos e limites de linhas/colunas/células reduz o risco de consumo descontrolado de memória, embora o contrato atual ainda retorne os artefatos como `byte[]`. As validações automatizadas passaram com 4 testes aprovados.

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
