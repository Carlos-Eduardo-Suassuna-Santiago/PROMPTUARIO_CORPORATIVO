# Patient-Clinical Service

Serviço unificado em Java 21 e Spring Boot 3.3 que atende pacientes, consultas,
prontuários e prescrições na porta interna `8000`.

## Processo único

API e processamento assíncrono são executados no mesmo processo Spring Boot:

- `PrescriptionPdfWorker` usa `@RabbitListener` para consumir a fila durável
  `prescription.pdf.generation`;
- `PdfWorkerConfig` configura concorrência, retry e dead-letter queue;
- não existe container `patient-clinical-worker` nem dependência de Celery ou
  Redis para esse worker;
- RabbitMQ e MinIO continuam obrigatórios para mensageria e armazenamento.

O serviço aparece uma única vez em `backend-java/docker-compose.yml` e
`backend/docker-compose.yml`, sob o nome `patient-clinical-service`.

## Desenvolvimento

A partir de `backend-java/`:

```bash
make dev-patient-clinical
```

O comando constrói e inicia o container Java juntamente com suas dependências
do Compose. O listener Spring AMQP é iniciado automaticamente com a aplicação.

## Testes

```bash
make test-patient-clinical
```

Equivalentemente, com Maven disponível:

```bash
cd patient-clinical-service
mvn clean test
```

O AI Service permanece em Python e pode ser iniciado e testado separadamente:

```bash
make dev-ai
make test-ai
```
