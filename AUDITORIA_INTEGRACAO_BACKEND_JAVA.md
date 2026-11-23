# Auditoria de Integração do Backend Java

**Data:** 11/11/2026  
**Escopo:** `backend-java/`

## Resumo executivo

A estrutura Java está compilando corretamente e o Docker Compose possui sintaxe válida. O serviço de IA continua implementado em Python e permanece presente no ecossistema.

Entretanto, a integração ainda não está pronta para ser considerada funcional em runtime. Foram identificados dois bloqueios principais:

1. Ausência de migrações Flyway do `patient-clinical-service`.
2. Incompatibilidade entre os exchanges/routing keys de eventos publicados pelo Java e consumidos pelo AI Python.

Também existem referências antigas ao worker Celery no Makefile e os testes de integração ainda não comprovam o fluxo ponta a ponta completo.

## Componentes avaliados

- `backend-java/patient-clinical-service`
- `backend-java/iam-service`
- `backend-java/gateway`
- `backend-java/ai-service`
- `backend-java/docker-compose.yml`
- `backend/docker-compose.yml`
- contratos em `backend/shared/events`

## Validações realizadas

### Compilação Java

Os três serviços Java foram compilados com JDK 21:

```text
gateway:                   EXIT=0
iam-service:               EXIT=0
patient-clinical-service:  EXIT=0
```

### Docker Compose

O arquivo principal do backend Java foi validado:

```bash
docker compose -f backend-java/docker-compose.yml config --quiet
```

Resultado:

```text
COMPOSE_EXIT=0
```

### AI Python

O AI Python permanece em:

```text
backend-java/ai-service
```

Mantendo sua arquitetura baseada em:

- FastAPI;
- MongoDB;
- Redis;
- RabbitMQ;
- JWT;
- integração com LLM;
- publicação e consumo de eventos.

O contexto de build do AI foi ajustado para permitir acesso a:

- `backend-java/ai-service`;
- `backend/shared`.

## Problemas encontrados

### 1. Migrações Flyway ausentes no Patient-Clinical

O serviço está configurado com:

```yaml
spring.jpa.hibernate.ddl-auto: validate
spring.flyway.enabled: true
```

Porém, não foi encontrada uma pasta válida com scripts em:

```text
backend-java/patient-clinical-service/src/main/resources/db/migration
```

Isso pode impedir o startup do serviço, pois o Hibernate validará tabelas inexistentes no PostgreSQL.

É necessário criar uma migração inicial para, no mínimo:

- `patients`;
- `allergies`;
- `vaccines`;
- `continuous_medications`;
- `medication_history`;
- `patient_documents`;
- `appointments`;
- `doctor_schedules`;
- `time_slots`;
- `medical_records`;
- `prescriptions`;
- `exam_requests`;
- `medical_certificates`;
- tabelas de histórico clínico;
- `audit_logs`.

### 2. Incompatibilidade de eventos RabbitMQ

O AI Python espera os contratos legados:

```text
Exchange: promptuario.clinical
Routing key: clinical.medical_record.created
Routing key: clinical.prescription.generated
```

O serviço Java publica atualmente no contrato novo:

```text
Exchange: domain.events
Routing key: medical_record.created
Routing key: prescription.generated
```

Consequentemente, o AI pode subir normalmente, mas não receber eventos de prontuários e prescrições.

#### Solução recomendada

Durante a migração, o serviço Java deve publicar nos dois contratos:

- exchange novo `domain.events`;
- exchange legado `promptuario.clinical`.

O mesmo evento deve manter:

- `eventId`;
- `timestamp`;
- `correlationId`;
- payload equivalente.

A publicação legada deve ser controlada por uma configuração, por exemplo:

```yaml
app:
  messaging:
    legacy-publish-enabled: true
```

Após a migração de todos os consumidores, a publicação legada poderá ser desativada.

### 3. Referências antigas ao worker Celery

O `patient-clinical-worker` foi substituído pelo worker Spring AMQP dentro do serviço Java, mas ainda existem referências antigas no Makefile e em comandos auxiliares.

Essas referências devem ser removidas ou atualizadas para não tentar iniciar um container Celery inexistente.

Importante: isso não se aplica ao AI Python nem aos workers de outros serviços que ainda utilizem Celery.

### 4. Testes de integração incompletos

Existe um teste com Testcontainers para PostgreSQL, RabbitMQ e MinIO, mas o fluxo ponta a ponta ainda não está comprovado.

É necessário validar efetivamente:

1. criação de paciente;
2. criação de consulta;
3. criação de prontuário SOAP;
4. criação de prescrição;
5. publicação do evento no RabbitMQ;
6. consumo pelo AI Python;
7. geração do PDF;
8. upload no MinIO;
9. health checks;
10. idempotência dos consumidores.

Esses testes dependem de Docker ativo e de um schema Flyway completo.

## Estado por componente

| Componente | Situação |
|---|---|
| Gateway Java | Compila; rotas configuradas para o serviço Java e AI |
| IAM Java | Compila; migração IAM existente |
| Patient-Clinical Java | Compila; falta schema Flyway completo |
| AI Python | Mantido; build/contexto revisado |
| RabbitMQ | Configuração Java existente, mas contratos clínicos incompatíveis |
| MinIO/S3 | Configuração presente; requer teste runtime |
| Docker Compose | Sintaxe válida |
| Testcontainers | Estrutura criada; fluxo E2E ainda não comprovado |

## Prioridade de correção

### Prioridade alta

1. Criar as migrações Flyway do `patient-clinical-service`.
2. Implementar publicação dupla dos eventos Java.
3. Validar startup real com PostgreSQL, RabbitMQ e MinIO.

### Prioridade média

4. Atualizar Makefile e documentação para remover referências obsoletas ao worker Celery do patient-clinical.
5. Criar testes de contrato entre eventos Java e modelos Python.
6. Executar o fluxo ponta a ponta com Docker Compose.

### Prioridade baixa

7. Desativar gradualmente os contratos legados após a migração dos consumidores.
8. Remover código de compatibilidade somente após métricas confirmarem que não há consumidores antigos.

## Conclusão

O backend Java está estruturalmente compilável e o AI Python pode continuar sendo utilizado. Porém, a integração ainda não está completa em runtime.

Antes de considerar a migração concluída, é obrigatório corrigir:

- o schema Flyway do `patient-clinical-service`;
- a compatibilidade dos eventos RabbitMQ;
- as referências antigas ao worker Celery;
- os testes de integração ponta a ponta.

