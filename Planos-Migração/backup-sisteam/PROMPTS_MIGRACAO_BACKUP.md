# Guia de Prompts de Implementação: Migração do Backup Service (Python -> Java 21 / Spring Boot 3.x / Disaster Recovery)

Este documento contém o conjunto completo de **prompts técnicos estruturados e detalhados**, organizados em **6 fases sequenciais (Fases B0 a B5)**, para guiar a implementação assistida por IA da migração do **Backup Service** de Python (script procedural `backup_runner.py` / `subprocess` / `schedule`) para **Java 21 (LTS), Spring Boot 3.3+, Spring Task Scheduling, AWS SDK for Java 2.x, AES-256-GCM e Micrometer Prometheus**.

O conteúdo foi desenhado com base no documento [PLANO_MIGRACAO_BACKUP.md](file:///c:/Users/zackv/Desktop/Integrador_Corporativo/Planos-Migra%C3%A7%C3%A3o/backup-sisteam/PLANO_MIGRACAO_BACKUP.md) e na topologia unificada pós-Fase 12 do PROMPTUÁRIO.

---

## Índice das Fases de Implementação

1. [Fase B0: Correção Imediata da Dívida Técnica no Runner Python (Topologia Unificada)]( #fase-b0-correção-imediata-da-dívida-técnica-no-runner-python-topologia-unificada)
2. [Fase B1: Fundação do Projeto Java 21, Dependências Maven e Arquitetura Base](#fase-b1-fundação-do-projeto-java-21-dependências-maven-e-arquitetura-base)
3. [Fase B2: Dumpers em Stream, Criptografia AES-256-GCM, Upload MinIO e Retenção](#fase-b2-dumpers-em-stream-criptografia-aes-256-gcm-upload-minio-e-retenção)
4. [Fase B3: Módulo de Restauração Controlada, Validação SHA-256 e DR Drill](#fase-b3-módulo-de-restauração-controlada-validação-sha-256-e-dr-drill)
5. [Fase B4: Observabilidade, Métricas Micrometer para Prometheus e Alertas](#fase-b4-observabilidade-métricas-micrometer-para-prometheus-e-alertas)
6. [Fase B5: CLI Runner, API REST Operacional, Dockerfile Multi-Stage e Cutover Final](#fase-b5-cli-runner-api-rest-operacional-dockerfile-multi-stage-e-cutover-final)

---

## Diretrizes Gerais para o Ciclo de Execução

- **Execução Sequencial Obrigatória:** Inicie rigorosamente pela **Fase B0**. Ela corrige a vulnerabilidade de desastre imediata no runner atual antes de qualquer compilação Java.
- **Topologia Auditada:** Nunca tente conectar em `db-patient` ou `db-clinical`. Os bancos agora residem no container `db-patient-clinical:5432`, banco `patient_clinical_db` (usuário `clinical`, senha `clinical_pass`).
- **Segurança LGPD (Zero Plaintext Leak):** Nenhum arquivo não criptografado contendo dados sensíveis de pacientes deve ser gravado desprotegido em disco. O fluxo de dump deve encadear `ProcessBuilder -> GZIPOutputStream -> CipherOutputStream (AES-256-GCM) -> S3/Disk`.
- **Compatibilidade Operacional com `Makefile`:** Manter integralmente o suporte aos comandos `--once`, `--restore`, `--kind`, `--target` e `--file`, além de gerar o arquivo local `/var/backups/status.json`.

---

## Fase B0: Correção Imediata da Dívida Técnica no Runner Python (Topologia Unificada)

### Objetivo
Corrigir imediatamente o script atual `backend/backup/backup_runner.py` e a orquestração no `backend/docker-compose.yml`, eliminando as referências aos hosts órfãos legados (`db-patient` e `db-clinical`) e incluindo o banco de dados unificado `db-patient-clinical` (`patient_clinical_db`). Coletar o baseline de tempo e tamanho do dump.

### Arquivos Alvo
- `backend/backup/backup_runner.py`
- `backend/docker-compose.yml`
- `backend/backup/BACKUP_BASELINE.md` (novo arquivo)

### Prompt da Fase B0

```text
Atue como Engenheiro de Confiabilidade de Sistemas (SRE) e Administrador de Banco de Dados.
Precisamos aplicar uma correção crítica e imediata no serviço de backup atual em Python localizado em "backend/backup/backup_runner.py".

Contexto da Falha Atual:
Após a Fase 12 de unificação do PROMPTUÁRIO, os serviços "patient-service" e "clinical-service" foram fundidos no "patient-clinical-service", com banco único "patient_clinical_db" hospedado no container "db-patient-clinical".
O runner atual em "backup_runner.py" ainda tenta fazer dump dos hosts órfãos inexistentes "db-patient" e "db-clinical", gerando falhas e deixando o banco clínico unificado desprotegido contra desastres.

Tarefas Obrigatórias:

1. Modifique a lista "POSTGRES_DATABASES" em "backend/backup/backup_runner.py":
   - Remova as entradas de "db-patient" e "db-clinical".
   - Adicione a entrada do banco unificado:
     {"host": os.getenv("PATIENT_CLINICAL_DB_HOST", "db-patient-clinical"), "user": "clinical", "password": "clinical_pass", "dbname": "patient_clinical_db"}
   - Mantenha os bancos "iam_db" (host: "db-iam") e "reporting_db" (host: "db-reporting").
   - Certifique-se de que variáveis de ambiente como POSTGRES_USER e POSTGRES_PASSWORD possam sobrescrever as credenciais default caso definidas.

2. Verifique o "backend/docker-compose.yml":
   - Garanta que o serviço "backup-service" dependa ("depends_on") de:
     * "db-iam" (service_healthy)
     * "db-patient-clinical" (service_healthy)
     * "db-reporting" (service_healthy)
     * "db-ai" (service_healthy)
     * "minio" (service_healthy)
   - Assegure que as redes do Docker Compose permitam que o "backup-service" alcance todos esses containers.

3. Crie um script de teste e o arquivo "backend/backup/BACKUP_BASELINE.md":
   - Documente o comando de execução de contingência: "docker compose run --rm backup-service python backup_runner.py --once".
   - Detalhe a estrutura do arquivo gerado "/var/backups/status.json", registrando tempo de execução, tamanho dos dumps de cada banco e status final ("success" ou "partial").

Gere o código modificado com preservação de formatação, logging limpo e garantia de que nenhuma exceção não tratada derrube a execução dos demais bancos se um falhar isoladamente.
```

### Critérios de Aceite
- Execução de `docker compose run --rm backup-service python backup_runner.py --once` concluída com sucesso.
- O arquivo `/var/backups/status.json` registra dumps válidos para `iam_db`, `patient_clinical_db`, `reporting_db` e `ai_db`.
- O dump de `patient_clinical_db` é gravado no volume local e enviado para o MinIO no bucket `backups`.

---

## Fase B1: Fundação do Projeto Java 21, Dependências Maven e Arquitetura Base

### Objetivo
Inicializar a estrutura do novo microsserviço corporativo `backend-java/backup-service` com Java 21 e Spring Boot 3.3+, configurando `pom.xml`, `application.yml` tipado com `@ConfigurationProperties`, os modelos de dados e enums de domínio, e a infraestrutura de agendamento Spring TaskScheduler.

### Arquivos Alvo
- `backend-java/backup-service/pom.xml`
- `backend-java/backup-service/src/main/resources/application.yml`
- `backend-java/backup-service/src/main/java/br/edu/ifrn/promptuario/backup/BackupServiceApplication.java`
- `backend-java/backup-service/src/main/java/br/edu/ifrn/promptuario/backup/config/BackupProperties.java`
- `backend-java/backup-service/src/main/java/br/edu/ifrn/promptuario/backup/config/AsyncConfig.java`
- `backend-java/backup-service/src/main/java/br/edu/ifrn/promptuario/backup/model/`: `DatabaseTarget.java`, `ExecutionMode.java`, `BackupJobExecution.java`, `DatabaseBackupResult.java`, `BackupManifest.java`.

### Prompt da Fase B1

```text
Atue como Engenheiro de Software Sênior especialista em Java 21 e Spring Boot 3.3+.
Vamos criar a base do novo microsserviço "backup-service" localizado em "backend-java/backup-service/".

Implemente a fundação do projeto com os seguintes componentes:

1. "pom.xml":
   - Java 21, Spring Boot 3.3.4 (ou superior compatível).
   - GroupId: "br.edu.ifrn.promptuario"
   - ArtifactId: "backup-service"
   - Dependências:
     * "spring-boot-starter-web" (para API REST operacional e Actuator)
     * "spring-boot-starter-validation"
     * "spring-boot-starter-actuator"
     * "io.micrometer:micrometer-registry-prometheus"
     * "software.amazon.awssdk:s3:2.25.0+" e "software.amazon.awssdk:netty-nio-client"
     * "info.picocli:picocli-spring-boot-starter:4.7.5" (para suporte robusto a argumentos de linha de comando)
     * "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
     * "org.projectlombok:lombok" (scope provided)
     * "spring-boot-starter-test" com JUnit 5 e Mockito.

2. "src/main/resources/application.yml":
   - Configurações do servidor HTTP: porta 8000 interna.
   - Propriedades de agendamento e retenção sob o prefixo "promptuario.backup":
     * schedule-cron: "${BACKUP_SCHEDULE_CRON:0 0 3 * * ?}" (execução diária às 03:00 UTC)
     * retention-days: "${BACKUP_RETENTION_DAYS:7}"
     * root-dir: "${BACKUP_ROOT_DIR:/var/backups}"
     * encryption-enabled: "${BACKUP_ENCRYPTION_ENABLED:true}"
     * encryption-secret: "${BACKUP_ENCRYPTION_KEY:PROMPTUARIO_AES_256_SECRET_KEY_SAMPLE_32B}"
   - Topologia de bancos mapeada:
     * postgres:
       - iam: { host: "${DB_IAM_HOST:db-iam}", port: 5432, dbname: "iam_db", user: "iam", password: "${DB_IAM_PASS:iam_pass}" }
       - patient-clinical: { host: "${DB_PATIENT_CLINICAL_HOST:db-patient-clinical}", port: 5432, dbname: "patient_clinical_db", user: "clinical", password: "${DB_PATIENT_CLINICAL_PASS:clinical_pass}" }
       - reporting: { host: "${DB_REPORTING_HOST:db-reporting}", port: 5432, dbname: "reporting_db", user: "reporting", password: "${DB_REPORTING_PASS:reporting_pass}" }
     * mongo:
       - host: "${MONGO_HOST:db-ai}", port: 27017, dbname: "ai_db", user: "ai", password: "${MONGO_PASS:ai_pass}"
   - Storage S3 MinIO sob "app.storage.s3":
     * endpoint: "${MINIO_ENDPOINT:http://minio:9000}"
     * access-key: "${MINIO_ACCESS_KEY:promptuario}"
     * secret-key: "${MINIO_SECRET_KEY:promptuario_pass}"
     * bucket-backups: "${MINIO_BACKUP_BUCKET:backups}"
     * region: "us-east-1"
   - Endpoints do Actuator expostos: "health,info,prometheus".

3. Modelos de Domínio e Enums (pacote "br.edu.ifrn.promptuario.backup.model"):
   - "DatabaseTarget" (Enum): IAM_DB, PATIENT_CLINICAL_DB, REPORTING_DB, MONGO_AI_DB, MINIO_BUCKETS.
   - "ExecutionMode" (Enum): SCHEDULED, MANUAL, CLI, RESTORE.
   - "DatabaseBackupResult" (Record/Class): target, kind, status ("SUCCESS", "FAILED"), filePath, s3Key, sizeBytes, durationSeconds, sha256Hex, error.
   - "BackupManifest" (Record): backupId, generatedAt, schemaVersion, databases (List<DatabaseBackupResult>), encryptionAlgorithm, rpoMet, retentionUntil.
   - "BackupJobExecution": estado consolidado do ciclo completo de backup.

4. Classes de Configuração:
   - "BackupProperties.java" (@ConfigurationProperties(prefix = "promptuario.backup")): mapeando todas as propriedades estruturadas.
   - "AsyncConfig.java": ThreadPoolTaskScheduler com pool dedicado para não concorrer com dumps paralelos.
   - "BackupServiceApplication.java": @SpringBootApplication com @EnableScheduling e @EnableAsync.

Gere código limpo, totalmente compilável e seguindo as melhores práticas do Spring Boot 3.
```

### Critérios de Aceite
- Execução de `mvn clean compile` sem erros em `backend-java/backup-service`.
- Injeção das propriedades via `BackupProperties` validada por testes de contexto.

---

## Fase B2: Dumpers em Stream, Criptografia AES-256-GCM, Upload MinIO e Retenção

### Objetivo
Implementar a camada central de extração com zero vazamento de texto plano: dumpers em stream (`PostgresDatabaseDumper` e `MongoDatabaseDumper`), o motor de criptografia autenticada (`CryptoService` com AES-256-GCM), o cálculo de hash SHA-256 em tempo real, o cliente S3 para MinIO (`S3BackupStorageService`), o orquestrador (`BackupOrchestratorService`) e a limpeza de retenção (`RetentionCleanupService`).

### Arquivos Alvo
- `service/CryptoService.java`
- `dumper/DatabaseDumper.java`
- `dumper/PostgresDatabaseDumper.java`
- `dumper/MongoDatabaseDumper.java`
- `storage/S3BackupStorageService.java`
- `service/RetentionCleanupService.java`
- `service/StatusReportService.java`
- `service/BackupOrchestratorService.java`

### Prompt da Fase B2

```text
Atue como Engenheiro de Software Especialista em Segurança da Informação, Criptografia e Armazenamento em Nuvem.
Vamos implementar o pipeline streaming de dump, criptografia e upload do "backup-service".

Arquitetura do Pipeline Obrigatória:
ProcessBuilder (stdout stream) -> GZIPOutputStream -> CipherOutputStream (AES-256-GCM) -> DigestOutputStream (SHA-256) -> FileOutputStream (disco local) -> Upload para o MinIO.

Implemente os seguintes serviços em "br.edu.ifrn.promptuario.backup":

1. "CryptoService.java" (pacote "service"):
   - Algoritmo: "AES/GCM/NoPadding" (chave simétrica de 256 bits).
   - IV (Initialization Vector) seguro: gerar 12 bytes aleatórios via SecureRandom para cada arquivo gravado.
   - Gravação do IV: prefixar os 12 bytes do IV diretamente no início do stream de saída.
   - Tag de autenticação GCM: 128 bits.
   - Método "createEncryptingStream(OutputStream destination, SecretKey key)": retorna CipherOutputStream devidamente inicializado.
   - Método "createDecryptingStream(InputStream source, SecretKey key)": lê o IV dos primeiros 12 bytes e retorna CipherInputStream pronto para decodificação.

2. "DatabaseDumper.java" e Implementações (pacote "dumper"):
   - Interface comum: "DatabaseBackupResult dump(DatabaseTarget target, ExecutionMode mode)".
   - "PostgresDatabaseDumper":
     * Executa via ProcessBuilder: "pg_dump -h <host> -p <port> -U <user> <dbname>".
     * Injeta senha com segurança via variável de ambiente "PGPASSWORD".
     * Encadeia o stream de saída diretamente no GZIPOutputStream e CipherOutputStream, calculando o Digest SHA-256 simultaneamente.
     * Timeout de execução configurável (padrão 10 minutos).
     * Se returnCode != 0, captura stderr e gera DatabaseBackupResult com status FAILED sem travar os outros bancos.
   - "MongoDatabaseDumper":
     * Executa "mongodump --host=<host> --port=<port> --username=<user> --password=<pass> --authenticationDatabase=admin --db=<dbname> --archive --gzip".
     * Encripta a saída e salva localmente.

3. "S3BackupStorageService.java" (pacote "storage"):
   - Configura "S3Client" apontando para o endpoint MinIO com "forcePathStyle(true)".
   - Método "ensureBucketExists(String bucketName)".
   - Método "uploadArtifact(Path localFile, String key)": upload direto usando PutObjectRequest com metadados adicionais (SHA-256, target, timestamp).
   - Estrutura de chaves no MinIO: "backups/{kind}/{dbname}/{yyyy}/{MM}/{dd}/{filename}".

4. "RetentionCleanupService.java" (pacote "service"):
   - Expurga arquivos locais em "/var/backups" cuja data de modificação seja superior a "retention-days".
   - Executa deleção em lote no MinIO via "DeleteObjectsRequest" para objetos expirados no bucket de backups.

5. "StatusReportService.java" e "BackupOrchestratorService.java" (pacote "service"):
   - "BackupOrchestratorService.executeFullBackup(ExecutionMode mode)":
     1. Assegura a existência do bucket MinIO.
     2. Executa o dump de cada banco em sequência protegida por try-catch isolado:
        * IAM_DB
        * PATIENT_CLINICAL_DB (banco unificado)
        * REPORTING_DB
        * MONGO_AI_DB
     3. Gera o arquivo de manifesto criptográfico "manifest.json".
     4. Atualiza o arquivo persistente "/var/backups/status.json".
     5. Executa a limpeza de retenção.
     6. Retorna BackupJobExecution consolidado.

Forneça o código completo, com tratamento robusto de I/O exceptions, fechamento seguro de streams (try-with-resources) e sem alocação massiva de memória RAM.
```

### Critérios de Aceite
- Os arquivos locais e no S3 terminam com a extensão `.sql.gz.enc` e não podem ser lidos diretamente por `gunzip` sem a chave AES.
- Geração correta do hash SHA-256 e gravação do manifesto JSON de integridade.
- Falha na extração de um banco secundário não interrompe os demais.

---

## Fase B3: Módulo de Restauração Controlada, Validação SHA-256 e DR Drill

### Objetivo
Construir o módulo de restauração de desastre (`RestoreOrchestratorService`, `PostgresDatabaseRestorer`, `MongoDatabaseRestorer`), com pré-checagem obrigatória de hash SHA-256, decodificação autenticada AES-GCM (abortando se a tag falhar) e suporte a simulações periódicas automatizadas de restauração (Drills de DR).

### Arquivos Alvo
- `restorer/DatabaseRestorer.java`
- `restorer/PostgresDatabaseRestorer.java`
- `restorer/MongoDatabaseRestorer.java`
- `service/RestoreOrchestratorService.java`
- `service/DrillVerificationService.java`

### Prompt da Fase B3

```text
Atue como Engenheiro Especialista em Disaster Recovery (DR) e Resiliência de Dados.
Vamos implementar o mecanismo de restauração segura e validação automatizada de integridade do "backup-service".

Princípios de Segurança da Restauração:
1. Validação Pré-Restore: Calcular o SHA-256 do arquivo e comparar com o manifesto antes de qualquer conexão ao banco.
2. Autenticação Criptográfica: O CipherInputStream AES-GCM falhará imediatamente se os dados tiverem sofrido corrupção de bits ou adulteração.
3. Isolamento e Timeout: A restauração deve ter timeout de segurança estrito (ex: 30 minutos).

Implemente os seguintes componentes em "br.edu.ifrn.promptuario.backup":

1. Interface "DatabaseRestorer.java" (pacote "restorer"):
   - "boolean restore(DatabaseTarget target, Path encryptedBackupFile, String expectedSha256)".

2. "PostgresDatabaseRestorer.java":
   - Pipeline de Restauração:
     FileInputStream -> CipherInputStream (AES-256-GCM) -> GZIPInputStream -> ProcessBuilder stdin ("psql -h <host> -p <port> -U <user> <dbname>").
   - Valida previamente se o arquivo existe e confere o SHA-256 se fornecido.
   - Captura stdout e stderr do psql; se o comando falhar, lança RestoreFailedException com detalhamento do erro sem vazar credenciais.

3. "MongoDatabaseRestorer.java":
   - Decodifica o arquivo encriptado e alimenta o "mongorestore --host=<host> --port=<port> --username=<user> --password=<pass> --authenticationDatabase=admin --db=<dbname> --archive --gzip --drop".

4. "RestoreOrchestratorService.java" (pacote "service"):
   - "restoreDatabase(String targetName, String filePath, String sha256)":
     * Identifica o target correspondente (ex: "patient_clinical_db" -> DatabaseTarget.PATIENT_CLINICAL_DB).
     * Se o arquivo informado for remoto (S3 key), faz download temporário seguro no diretório de staging.
     * Aciona o restorer correspondente.
     * Grava log de auditoria da restauração e atualiza métricas.

5. "DrillVerificationService.java" (pacote "service"):
   - Simulação mensal automatizada de contingência (Dry-Run Drill):
     * Permite testar a restauração em um banco de dados efêmero ou base de homologação sem sobrescrever a produção.
     * Registra o resultado em "backup_restore_drill_status" para atestar compliance de RTO e RPO.

Crie testes unitários abrangentes com JUnit 5 simulando arquivos íntegros e arquivos corrompidos para garantir que a restauração seja barrada em caso de erro na tag GCM.
```

### Critérios de Aceite
- Restauração de dump PostgreSQL e MongoDB validada com sucesso contra bancos de teste.
- Abortagem imediata com erro descritivo caso o checksum SHA-256 divirja ou a chave AES-GCM esteja incorreta.

---

## Fase B4: Observabilidade, Métricas Micrometer para Prometheus e Alertas

### Objetivo
Integrar o serviço ao ecossistema de observabilidade corporativo do PROMPTUÁRIO via Micrometer e Spring Boot Actuator, expondo métricas customizadas de latência, tamanho de artefatos, taxa de sucesso/falha e timestamp do último backup para scrape pelo Prometheus, além de registrar as regras de alerta no arquivo `alerts.yml`.

### Arquivos Alvo
- `metrics/BackupMetricsService.java`
- `backend/observability/prometheus/alerts.yml` (atualização)
- `backend/observability/prometheus/prometheus.yml` (atualização)
- `backend/observability/grafana/provisioning/dashboards/backup_dashboard.json` (novo)

### Prompt da Fase B4

```text
Atue como Engenheiro de Confiabilidade (SRE) especialista em Prometheus, Grafana e Micrometer.
Vamos implementar a observabilidade completa do "backup-service".

1. "BackupMetricsService.java" em "br.edu.ifrn.promptuario.backup.metrics":
   - Injeta "MeterRegistry" do Micrometer.
   - Declare e gerencie as seguintes métricas padronizadas:
     * "backup_execution_duration_seconds" (Timer / Summary): tempo total por banco e status (labels: target, status).
     * "backup_artifact_size_bytes" (Gauge): tamanho em bytes do artefato comprimido e criptografado (labels: target).
     * "backup_last_success_timestamp_seconds" (Gauge): epoch timestamp em segundos do último backup com sucesso (labels: target).
     * "backup_status_total" (Counter): total de execuções (labels: target, status=["success", "failed"]).
     * "backup_retention_deleted_total" (Counter): quantidade de artefatos expurgados pela política de retenção.
     * "backup_restore_duration_seconds" (Timer): tempo gasto em restaurações.
   - Atualize essas métricas em tempo real ao final de cada execução no "BackupOrchestratorService" e "RestoreOrchestratorService".

2. Atualização no "backend/observability/prometheus/prometheus.yml":
   - Adicione o scrape job para o novo serviço de backup:
     ```yaml
     - job_name: 'backup-service'
       metrics_path: '/actuator/prometheus'
       scrape_interval: 15s
       static_configs:
         - targets: ['backup-service:8000']
     ```

3. Atualização no "backend/observability/prometheus/alerts.yml":
   - Adicione as regras de alerta essenciais:
     * "BackupJobFailed": dispara alerta CRÍTICO se "increase(backup_status_total{status='failed'}[1h]) > 0".
     * "BackupMissing24h": dispara alerta WARNING se "(time() - backup_last_success_timestamp_seconds) > 86400" (violação de RPO).
     * "BackupArtifactTooSmall": dispara alerta WARNING se dump for inferior a 100KB ("backup_artifact_size_bytes < 100000").

4. Painel Grafana ("backend/observability/grafana/provisioning/dashboards/backup_dashboard.json"):
   - Crie o dashboard "Disaster Recovery & Backups" contendo:
     * Stat panels: RPO Status (Verde se < 24h, Vermelho se > 24h), Tamanho Total dos Backups, Último Backup por Banco.
     * Time series: Duração da Execução dos Dumps ao longo do tempo.
     * Gauge: Tamanho individual dos bancos (IAM, Patient-Clinical, Reporting, AI).
     * Tabela com histórico das últimas execuções e status.

Forneça os arquivos completos com a sintaxe exata para importação automática no Grafana.
```

### Critérios de Aceite
- Endpoint `/actuator/prometheus` servindo todas as métricas customizadas `backup_*`.
- O Prometheus realiza scrape sem erros de conexão (`UP = 1`).
- Painel Grafana importado e renderizando os dados dos jobs.

---

## Fase B5: CLI Runner, API REST Operacional, Dockerfile Multi-Stage e Cutover Final

### Objetivo
Finalizar a implementação do `BackupCommandLineRunner` para compatibilidade total com os comandos do `Makefile`, criar os controladores REST operacionais protegidos por JWT, empacotar o serviço em um `Dockerfile` multi-stage otimizado (com `eclipse-temurin:21-jre-alpine` + `pg_dump` + `mongodump`), atualizar o `docker-compose.yml` e o `Makefile`, e executar o corte seguro em shadow mode.

### Arquivos Alvo
- `cli/BackupCommandLineRunner.java`
- `controller/BackupOperationController.java`
- `controller/RestoreOperationController.java`
- `config/SecurityConfig.java`
- `backend-java/backup-service/Dockerfile`
- `backend/docker-compose.yml` (atualização)
- `backend/Makefile` (atualização)

### Prompt da Fase B5

```text
Atue como Especialista em DevOps, Arquitetura de Contêineres e Spring Boot.
Vamos finalizar a implementação e realizar o cutover do "backup-service" de Python para Java 21.

1. "BackupCommandLineRunner.java" (pacote "cli"):
   - Implementa "CommandLineRunner" utilizando Picocli ou análise de argumentos:
     * Se receber argumentos "--once": dispara backup completo manual imediatamente e finaliza com System.exit(0) se sucesso ou exit(1) se falha.
     * Se receber argumentos "--restore --kind <postgres|mongo> --target <target> --file <path>": executa a restauração controlada do arquivo e sai com status correspondente.
     * Se executado sem argumentos: inicia como daemon, rodando o agendador periódico (@Scheduled) e servindo a API REST na porta 8000.

2. Controladores REST e Segurança (pacotes "controller" e "config"):
   - "SecurityConfig.java":
     * Libera "/healthz", "/actuator/prometheus" e "/actuator/health".
     * Protege endpoints "/api/v1/backups/**" exigindo role "ADMIN" via Spring Security OAuth2 Resource Server (JWT).
   - "BackupOperationController.java":
     * GET "/healthz" -> { "status": "ok", "service": "backup-service", "version": "1.0.0-java" }
     * GET "/api/v1/backups/status" -> Retorna o último status consolidado de "/var/backups/status.json".
     * POST "/api/v1/backups/execute" -> Dispara backup assíncrono sob demanda.
   - "RestoreOperationController.java":
     * POST "/api/v1/backups/restore" -> Inicia restore controlado.

3. "backend-java/backup-service/Dockerfile":
   - Multi-stage build otimizado para Alpine Linux:
     * Stage 1 (Build): "maven:3.9-eclipse-temurin-21-alpine", copia pom.xml, baixa dependências e compila JAR executável.
     * Stage 2 (Runtime): "eclipse-temurin:21-jre-alpine", instala ferramentas nativas de dump via apk:
       "apk add --no-cache postgresql15-client mongodb-tools bash tzdata"
     * Copia "app.jar" do stage de build.
     * Define diretório de trabalho "/app" e cria volume persistente "/var/backups".
     * Define usuário não-root onde viável ou permissões para execução de dumps.
     * Entrypoint padrão: ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

4. Atualização no "backend/docker-compose.yml":
   - Substitua o serviço "backup-service" existente:
     ```yaml
     backup-service:
       build:
         context: ../backend-java/backup-service
         dockerfile: Dockerfile
       container_name: backup-service
       restart: unless-stopped
       environment:
         - BACKUP_SCHEDULE_CRON=0 0 3 * * ?
         - BACKUP_RETENTION_DAYS=7
         - BACKUP_ENCRYPTION_ENABLED=true
         - BACKUP_ENCRYPTION_KEY=${BACKUP_ENCRYPTION_KEY:-CHAVE_SECRETA_AES_256_PROMPTUARIO_32B}
         - DB_IAM_HOST=db-iam
         - DB_PATIENT_CLINICAL_HOST=db-patient-clinical
         - DB_REPORTING_HOST=db-reporting
         - MONGO_HOST=db-ai
         - MINIO_ENDPOINT=http://minio:9000
         - MINIO_ACCESS_KEY=${MINIO_ROOT_USER:-promptuario}
         - MINIO_SECRET_KEY=${MINIO_ROOT_PASSWORD:-promptuario_pass}
         - MINIO_BACKUP_BUCKET=backups
         - JWT_SECRET_KEY=${JWT_SECRET_KEY}
       volumes:
         - backup_data:/var/backups
       ports:
         - "8006:8000"
       depends_on:
         db-iam:
           condition: service_healthy
         db-patient-clinical:
           condition: service_healthy
         db-reporting:
           condition: service_healthy
         db-ai:
           condition: service_healthy
         minio:
           condition: service_healthy
       networks:
         - backend
     ```

5. Atualização no "backend/Makefile":
   - "make backup-once": atualizado para "docker compose run --rm backup-service java -jar app.jar --once"
   - "make restore-db": atualizado para "docker compose run --rm backup-service java -jar app.jar --restore --kind postgres --target $(TARGET) --file $(FILE)"

Forneça os arquivos e um checklist de validação pós-cutover.
```

### Critérios de Aceite
- Build da imagem Docker final bem-sucedido com tamanho inferior a 300MB.
- Comandos `make backup-once` e `make restore-db` funcionando perfeitamente sem alteração de sintaxe externa.
- Container operando normalmente em background e emitindo logs estruturados e métricas para o Prometheus.

---

## Matriz de Riscos e Planos de Rollback

| Fase | Risco Principal | Impacto | Medida Mitigatória / Rollback |
| :--- | :--- | :--- | :--- |
| **B0** | Erro de sintaxe na edição do script Python legado | Falha no backup noturno | `git checkout backend/backup/backup_runner.py` restaura imediatamente o estado anterior. |
| **B2** | Falha de decodificação por perda da chave AES | Perda irrecuperável de backups criptografados | Backup da chave `BACKUP_ENCRYPTION_KEY` em cofre seguro de segredos e rotação planejada com versionamento de chave no manifesto. |
| **B2** | Esgotamento de memória RAM durante dump grande | OOM Killer derrubando o container | Processamento 100% em stream (sem carregar dump em `byte[]` na memória). |
| **B3** | Adulteração ou corrupção de dump silenciosa | Falha descoberta apenas no momento do desastre | Verificação de integridade obrigatória (SHA-256 + GCM Authentication Tag) e Drills mensais automatizados. |
| **B5** | Incompatibilidade de binários `pg_dump` com PostgreSQL 15 | Dump rejeitado por diferença de versão de protocolo | Utilização de `postgresql15-client` na imagem Alpine do Dockerfile runtime. |

---

## Comandos Rápidos de Validação

```bash
# 1. Executar backup sob demanda via Makefile (compatibilidade legada):
make backup-once

# 2. Inspecionar o status gerado da última rotina:
curl -s http://localhost:8006/api/v1/backups/status | jq .

# 3. Conferir métricas expostas para o Prometheus:
curl -s http://localhost:8006/actuator/prometheus | grep "backup_"

# 4. Simulação de restauração em ambiente local:
make restore-db TARGET=patient_clinical_db FILE=/var/backups/postgresql/patient_clinical_db/2026/10/16/postgres_patient_clinical_db_exemplo.sql.gz.enc
```
