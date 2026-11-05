# Prompts de Execução para Migração do Clinical Service

Este documento contém os **prompts exatos** que você deve copiar e colar para que um agente de IA (como eu) execute a migração do **Clinical Service** de Python para Java + Spring Boot passo a passo. 

Siga a ordem dos prompts rigorosamente. Cada prompt instrui a IA a ler o arquivo Python atual, converter a lógica para o ecossistema Spring Boot, e criar o novo arquivo Java na pasta correta (`backend-java/clinical-service/`).

---

## Passo 1: Inicialização e Configuração Básica (Setup)

**Copie e cole este prompt:**
```text
Atue como um Especialista em Java e Spring Boot. 
Sua tarefa é inicializar o projeto `clinical-service` em Java no diretório `backend-java/clinical-service/`.

1. Crie o arquivo `pom.xml` básico com as dependências do Spring Boot 3.2+ (Web, Data JPA, PostgreSQL, Flyway, Validation, Spring Security OAuth2 Resource Server, Actuator, AMQP, e AWS SDK S3).
2. Crie a classe principal `ClinicalServiceApplication.java` no pacote `clinical`.
3. Crie o arquivo `src/main/resources/application.yml`. Ele deve conter a configuração para porta 8003, conexão com PostgreSQL, configuração do RabbitMQ, credenciais do MinIO (S3) e JWT issuer do Keycloak. Baseie-se nas chaves encontradas em `backend/clinical-service/app/config.py`.
```

---

## Passo 2: Banco de Dados e Migrações (Flyway)

**Copie e cole este prompt:**
```text
Atue como um Especialista em Banco de Dados Relacional.
Leia o diretório `backend/clinical-service/migrations` (ou os modelos em `backend/clinical-service/app/domain/models/`) para entender o esquema do banco de dados atual do serviço clínico.

Sua tarefa é criar o script inicial do Flyway para o Spring Boot:
1. Crie o arquivo `src/main/resources/db/migration/V1__init_schema.sql` no projeto `backend-java/clinical-service/`.
2. O script deve conter os comandos DDL (CREATE TABLE) para as tabelas `medical_records`, `prescriptions`, `exams` e `audit_logs` exatamente com as colunas, tipos (UUID, JSON, VARCHAR) e relacionamentos mapeados no SQLAlchemy.
3. Certifique-se de que a sintaxe seja 100% compatível com PostgreSQL 15+.
```

---

## Passo 3: Entidades JPA (Models)

**Copie e cole este prompt:**
```text
Atue como um Especialista em JPA e Hibernate.
Leia todos os arquivos na pasta `backend/clinical-service/app/domain/models/`.

Sua tarefa é migrar esses modelos do SQLAlchemy para Entidades JPA no Java:
1. Crie as classes equivalentes na pasta `backend-java/clinical-service/src/main/java/clinical/model/`.
2. Mapeie corretamente os UUIDs (use @JdbcTypeCode(SqlTypes.VARCHAR) se necessário para compatibilidade).
3. Mapeie o campo `diagnosis_codes` (que é JSON) usando conversores do Hibernate (`@JdbcTypeCode(SqlTypes.JSON)`).
4. Utilize as anotações do Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor, @Builder) para reduzir o código boilerplate.
5. Adicione as anotações de auditoria `@CreationTimestamp` e `@UpdateTimestamp` equivalentes ao `func.now()`.
```

---

## Passo 4: Data Transfer Objects (DTOs)

**Copie e cole este prompt:**
```text
Atue como um Especialista em Spring Boot e Clean Architecture.
Verifique se há arquivos Pydantic na pasta do Python (geralmente schemas dentro de `api` ou `domain`). Se não encontrar a pasta de schemas, deduza as estruturas de entrada e saída pelos endpoints em `backend/clinical-service/app/api/`.

Sua tarefa é criar os DTOs do projeto Java:
1. Crie `Records` do Java (ex: `MedicalRecordCreateDTO.java`, `PrescriptionResponseDTO.java`) na pasta `backend-java/clinical-service/src/main/java/clinical/dto/`.
2. Aplique as anotações do Jakarta Validation (`@NotBlank`, `@NotNull`, `@Size`) para substituir as validações nativas do Pydantic ou do FastAPI.
```

---

## Passo 5: Repositórios (Spring Data JPA)

**Copie e cole este prompt:**
```text
Leia a pasta `backend/clinical-service/app/infrastructure/repositories/` para entender as queries de acesso a dados atuais.

Sua tarefa é migrar isso para o ecossistema Spring:
1. Crie as interfaces estendendo `JpaRepository<Entity, UUID>` na pasta `backend-java/clinical-service/src/main/java/clinical/repository/`.
2. Se o código Python possuir queries personalizadas (como buscar histórico ordenado, ou buscar por paciente), implemente os equivalentes em Java utilizando `@Query` do Spring Data ou métodos de nomenclatura automática (ex: `findByPatientIdOrderByCreatedAtDesc`).
```

---

## Passo 6: Integrações de Infraestrutura (RabbitMQ e S3)

**Copie e cole este prompt:**
```text
Leia os arquivos dentro de `backend/clinical-service/app/infrastructure/events/` e `backend/clinical-service/app/infrastructure/s3/`.

Sua tarefa é migrá-los para os padrões do Spring:
1. Em `backend-java/clinical-service/src/main/java/clinical/messaging/`, crie os Event Publishers usando `RabbitTemplate` para disparar os eventos atuais (ex: `MedicalRecordCreatedEvent`).
2. Em `backend-java/clinical-service/src/main/java/clinical/service/`, crie uma classe `S3StorageService.java` usando a biblioteca oficial da AWS (`S3Client`) para substituir as chamadas ao boto3 para uploads de arquivos/PDFs. Use `@Value` para resgatar as chaves do MinIO vindas do `application.yml`.
```

---

## Passo 7: Camada de Serviços (Regras de Negócio)

**Copie e cole este prompt:**
```text
Leia detalhadamente todos os arquivos em `backend/clinical-service/app/domain/services/`. Esta é a parte mais crítica, pois contém a regra de negócio.

Sua tarefa é migrar essa lógica:
1. Crie as classes equivalentes em `backend-java/clinical-service/src/main/java/clinical/service/` anotadas com `@Service`.
2. Converta o código Python para Java aplicando injeção de dependência por construtor (@RequiredArgsConstructor do Lombok).
3. Coloque a anotação `@Transactional` nos métodos que salvam múltiplas entidades.
4. Mantenha as integrações de disparo de evento e gravação no storage chamando as classes recém criadas de RabbitMQ e S3.
```

---

## Passo 8: API e Controladores (Controllers REST)

**Copie e cole este prompt:**
```text
Leia a pasta `backend/clinical-service/app/api/` e entenda o roteamento e respostas do FastAPI.

Sua tarefa é criar os Controllers do Spring Boot:
1. Crie classes anotadas com `@RestController` e `@RequestMapping("/api/v1/...")` na pasta `backend-java/clinical-service/src/main/java/clinical/controller/`.
2. Migre todas as funções mapeadas (`@post`, `@get`) para equivalentes `@PostMapping`, `@GetMapping`.
3. Injete os Services usando Lombok.
4. Configure os parâmetros da rota (PathVariables) e injete os RequestBodies (usando `@Valid` para disparar a validação do DTO).
5. Crie um `GlobalExceptionHandler.java` com `@RestControllerAdvice` para mapear erros e retornar JSON padronizado com os códigos HTTP corretos (como o FastAPI faz).
```

---

## Passo 9: Segurança e Autorização (Security)

**Copie e cole este prompt:**
```text
Sua tarefa é substituir o auth middleware do FastAPI pelo Spring Security.

1. Na pasta `backend-java/clinical-service/src/main/java/clinical/config/`, crie a classe `SecurityConfig.java`.
2. Configure o `SecurityFilterChain` definindo o servidor como um OAuth2 Resource Server habilitado para JWT.
3. Habilite anotações de método (`@EnableMethodSecurity`) para que possamos usar `@PreAuthorize` nos Controllers no futuro.
4. Configure o mapeador de authorities para ler a claim de 'roles' do JWT corretamente (vinda do Keycloak ou do IAM Service) e aplicar o prefixo 'ROLE_'.
```

---

## Passo 10: Dockerização do Serviço Java

**Copie e cole este prompt:**
```text
Atue como Especialista em DevOps.

Sua tarefa é finalizar o serviço preparando ele para ser conteinerizado e integrado:
1. Crie um arquivo `Dockerfile` na raiz de `backend-java/clinical-service/`. O Dockerfile deve ser multi-stage (uma fase com imagem Maven para buildar o `.jar`, e uma imagem JRE 21 alpine para rodar).
2. Edite o arquivo `docker-compose.yml` da raiz do projeto PROMPTUARIO para alterar o bloco do `clinical-service`. Mude o contexto de build de `backend/clinical-service` para `backend-java/clinical-service`, mantendo a porta 8003.
```
