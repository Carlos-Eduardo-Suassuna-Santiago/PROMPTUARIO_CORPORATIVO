# Prompts para Correção da Integração do Backend Java

Este arquivo transforma os problemas registrados em `AUDITORIA_INTEGRACAO_BACKEND_JAVA.md` em prompts executáveis para implementação e validação.

## Prompt mestre

```text
Atue como Engenheiro de Software Sênior, DBA PostgreSQL, especialista em Spring Boot 3.3+, Java 21, RabbitMQ, Docker Compose, Python FastAPI e Testcontainers.

Corrija os problemas de integração descritos em `AUDITORIA_INTEGRACAO_BACKEND_JAVA.md`, preservando a funcionalidade principal do `backend-java/ai-service`, que continuará em Python.

Não remova o AI Python, não altere seus endpoints públicos e não faça alterações destrutivas. Preserve mudanças existentes e trabalhe somente nos arquivos necessários.

Objetivos obrigatórios:

1. Criar migrações Flyway completas para o `patient-clinical-service`.
2. Tornar os eventos Java compatíveis simultaneamente com consumidores novos e legados Python.
3. Remover referências obsoletas ao worker Celery do patient-clinical.
4. Completar os testes de integração ponta a ponta.
5. Validar compilação, startup, Docker Compose, RabbitMQ, PostgreSQL, MinIO e AI Python.

Ao final, apresente um relatório com arquivos alterados, comandos executados, resultados e limitações ambientais.
```

## Prompt 1 — Migrações Flyway do Patient-Clinical

```text
Atue como DBA PostgreSQL 15 e especialista em Flyway.

Crie as migrações SQL ausentes em:

`backend-java/patient-clinical-service/src/main/resources/db/migration/`

Analise primeiro todas as entidades JPA, repositórios e DTOs do serviço para obter nomes, tipos, nulabilidade, índices e relacionamentos reais.

Crie uma migração inicial, por exemplo `V1__init_patient_clinical_schema.sql`, contemplando:

- patients;
- allergies;
- vaccines;
- continuous_medications;
- medication_history;
- patient_documents;
- doctor_schedules;
- time_slots;
- appointments;
- medical_records;
- medical_record_history;
- prescriptions;
- prescription_history;
- exam_requests;
- exam_request_history;
- medical_certificates;
- medical_certificate_history;
- audit_logs.

Requisitos:

- PostgreSQL 15;
- IDs VARCHAR(36) compatíveis com UUID textual;
- TIMESTAMPTZ para Instant;
- JSONB para campos mapeados com `@JdbcTypeCode(SqlTypes.JSON)`;
- enums persistidos como VARCHAR;
- foreign keys corretas;
- ON DELETE CASCADE somente onde compatível com as entidades;
- índices para campos usados pelos repositórios;
- constraints NOT NULL alinhadas às anotações JPA;
- nomes exatos de tabela e coluna usados por `@Table` e `@Column`;
- migração idempotente apenas quando isso não esconder erro de versão Flyway;
- não usar `ddl-auto: create` ou `update` como solução.

Verificações obrigatórias:

1. Executar Flyway contra PostgreSQL 15 limpo.
2. Executar `mvn clean test`.
3. Subir o serviço com `ddl-auto=validate`.
4. Confirmar que não há erro de tabela, coluna, tipo ou constraint.
5. Confirmar que a segunda inicialização não reaplica a migração.
```

## Prompt 2 — Compatibilidade dos eventos RabbitMQ

```text
Atue como arquiteto de integração especializado em RabbitMQ, Spring AMQP e contratos Python/Pydantic.

Analise:

- `backend/shared/events/__init__.py`;
- `backend/shared/events/broker.py`;
- `backend-java/patient-clinical-service/.../RabbitMQConfig.java`;
- `backend-java/patient-clinical-service/.../EventPublisher.java`;
- todos os eventos Java de patient e clinical.

Implemente publicação dupla temporária, sem quebrar o contrato existente.

Contrato novo:

- exchange `domain.events`;
- routing key `medical_record.created`;
- routing key `prescription.generated`.

Contrato legado Python:

- exchange `promptuario.clinical`;
- routing key `clinical.medical_record.created`;
- routing key `clinical.prescription.generated`.

Requisitos:

- mesmo `eventId` nas duas publicações;
- mesmo `timestamp`;
- mesmo `correlationId`;
- payload JSON equivalente;
- headers `eventId`, `eventType`, `timestamp`, `correlationId`;
- exchanges e bindings duráveis;
- feature flag `app.messaging.legacy-publish-enabled`;
- retry para falhas transitórias;
- logs estruturados com exchange, routing key e eventId;
- nenhuma publicação duplicada no mesmo destino;
- idempotência no consumidor Python por eventId.

Aceite aliases camelCase e snake_case quando necessário:

- eventId/event_id;
- eventType/event_type;
- recordId/record_id;
- prescriptionId/prescription_id;
- patientId/patient_id;
- correlationId/correlation_id.

Crie testes de contrato comparando o JSON produzido pelo Java com os modelos Python.
```

## Prompt 3 — Remoção das referências obsoletas ao Celery

```text
Atue como engenheiro DevOps responsável pela migração do worker Celery para Spring AMQP.

Revise:

- `backend-java/Makefile`;
- `backend-java/docker-compose.yml`;
- `backend/docker-compose.yml`;
- READMEs e scripts de inicialização.

Remova ou atualize somente referências ao `patient-clinical-worker` e ao Celery usado exclusivamente pelo patient-clinical.

Não remova:

- o AI Python;
- workers Celery pertencentes a outros serviços;
- Redis usado pelo AI;
- RabbitMQ;
- endpoints existentes.

Garanta que o patient-clinical Java seja iniciado como um único container, incluindo seu worker Spring AMQP interno.

Adicione comandos claros:

- `make dev-ai`;
- `make test-ai`;
- `make dev-patient-clinical`;
- `make test-patient-clinical`.

Valide com:

```bash
docker compose -f backend-java/docker-compose.yml config
docker compose -f backend/docker-compose.yml config
```
```

## Prompt 4 — Testes de integração ponta a ponta

```text
Atue como QA sênior especializado em JUnit 5, Spring Boot Test e Testcontainers.

Complete:

`backend-java/patient-clinical-service/src/test/java/br/edu/ifrn/promptuario/PatientClinicalIntegrationTest.java`

Use containers para:

- PostgreSQL 15;
- RabbitMQ;
- MinIO ou LocalStack S3;
- MongoDB e Redis quando o fluxo realmente envolver o AI Python.

Valide o seguinte fluxo:

1. `/healthz` retorna HTTP 200.
2. Criação de paciente retorna HTTP 201.
3. Criação de consulta retorna HTTP 201.
4. Criação de prontuário SOAP retorna HTTP 201.
5. Criação de prescrição retorna HTTP 201.
6. Evento de prontuário é publicado no exchange novo e legado.
7. Evento de prescrição é publicado no exchange novo e legado.
8. O AI Python consome o evento.
9. O evento é processado uma única vez por eventId.
10. O worker gera o PDF.
11. O PDF é armazenado no bucket `prescriptions`.
12. O registro de prescrição recebe `pdfS3Key` e `pdfGeneratedAt`.

Não deixe testes dependentes de serviços externos permanentes. Use `@DynamicPropertySource`.

Caso Docker não esteja disponível, os testes devem ser compiláveis e devem informar claramente que foram pulados por falta do daemon.

Crie também testes de falha:

- mensagem duplicada;
- RabbitMQ indisponível;
- bucket ausente;
- paciente inexistente;
- CPF duplicado;
- conflito de horário;
- payload inválido.
```

## Prompt 5 — Validação final da integração

```text
Execute uma auditoria final do backend-java após as correções.

Comandos obrigatórios:

```bash
cd backend-java/gateway
mvn clean test

cd ../iam-service
mvn clean test

cd ../patient-clinical-service
mvn clean test

cd ../ai-service
pytest -q

cd ../..
docker compose -f backend-java/docker-compose.yml config --quiet
docker compose -f backend/docker-compose.yml config --quiet
```

Com Docker disponível, execute:

```bash
docker compose -f backend-java/docker-compose.yml up -d \
  db-iam db-patient-clinical db-ai rabbitmq redis minio \
  iam-service patient-clinical-service ai-service gateway
```

Valide:

- `http://localhost:8000/healthz`;
- `http://localhost:8001/healthz`;
- `http://localhost:8002/healthz`;
- `http://localhost:8004/healthz`;
- `http://localhost:8002/docs`;
- logs sem falhas de Flyway;
- logs sem falhas de conexão RabbitMQ;
- eventos recebidos pelo AI;
- buckets MinIO criados;
- PDFs gerados;
- ausência do container `patient-clinical-worker`;
- ausência de referências ativas ao Redis DB 3 para o patient-clinical.

Gere um relatório final contendo:

- arquivos alterados;
- testes executados;
- resultados;
- serviços realmente iniciados;
- limitações por ausência de Docker ou credenciais externas;
- itens ainda pendentes.
```

## Critérios finais de aceite

- O schema do `patient-clinical-service` é criado pelo Flyway.
- O serviço Java inicia com `ddl-auto=validate`.
- O AI Python continua funcionando sem alteração de sua função principal.
- Eventos são entregues aos consumidores novos e legados.
- Publicação legada pode ser desativada por configuração.
- O worker Celery do patient-clinical não é iniciado.
- O worker Spring AMQP gera prescrições dentro do serviço Java.
- O Docker Compose é validado sem erros.
- Os três serviços Java compilam.
- Os testes Python passam.
- Os testes de integração passam quando Docker está disponível.
- Health checks retornam HTTP 200.
- Não há alterações destrutivas nem remoção de endpoints públicos.
