# Prompt de Unificacao: Patient + Clinical

Use o prompt abaixo para executar a unificacao dos servicos `patient-service` e `clinical-service` em um unico microservico, com validacoes por fase e possibilidade de rollback.

```text
Voce e um engenheiro senior responsavel por unificar os servicos:

- backend/patient-service
- backend/clinical-service

O objetivo e criar um unico microservico chamado:

backend/patient-clinical-service

A unificacao deve preservar os contratos externos, reduzir riscos de regressao e permitir rollback durante toda a migracao.

REGRAS OBRIGATORIAS

1. Nao apagar patient-service ou clinical-service durante as fases intermediarias.
2. Nao alterar as URLs publicas da API.
3. Nao alterar IDs existentes de pacientes, consultas, prontuarios, prescricoes ou documentos.
4. Nao remover eventos publicados que sejam consumidos por AI, Reporting ou outros componentes.
5. Nao executar uma fase seguinte se a fase atual possuir erros.
6. Antes de qualquer edicao, verificar o estado do Git e preservar alteracoes existentes do usuario.
7. Usar alteracoes pequenas e testaveis.
8. Apos cada fase:
   - executar os testes relacionados;
   - verificar erros de compilacao e importacao;
   - revisar o diff;
   - registrar o resultado.
9. Nao utilizar `Base.metadata.create_all()` como substituto de uma migracao de dados existente.
10. Nao modificar arquivos nao relacionados a unificacao.

ESTADO ATUAL

O Patient possui:

- Modelos em backend/patient-service/app/domain/models/patient.py
- Rotas em backend/patient-service/app/api/routers.py
- Servicos em backend/patient-service/app/domain/services/patient_service.py
- Repositorio em backend/patient-service/app/infrastructure/repositories/patient_repository.py
- Consumidores IAM em backend/patient-service/app/infrastructure/events/consumers.py
- Bucket MinIO para documentos de pacientes
- Testes de pacientes, documentos e historico de medicacoes

O Clinical possui:

- Modelos em backend/clinical-service/app/domain/models/clinical.py
- Rotas em backend/clinical-service/app/api/routers.py
- Servicos em backend/clinical-service/app/domain/services/clinical_service.py
- Repositorio em backend/clinical-service/app/infrastructure/repositories/clinical_repository.py
- Consumidores Patient e IAM em backend/clinical-service/app/infrastructure/events/consumers.py
- Worker Celery em backend/clinical-service/app/workers/
- Bucket MinIO para prescricoes
- Testes clinicos, PDFs, assinaturas e rich notes

FASE 0 — BASELINE E INVENTARIO

Antes de editar:

1. Verificar:
   - git status;
   - estrutura dos dois servicos;
   - variaveis de ambiente;
   - rotas;
   - modelos;
   - eventos;
   - workers;
   - testes;
   - Dockerfiles;
   - docker-compose.yml.
2. Executar os testes atuais de Patient e Clinical.
3. Registrar quais testes ja falham antes da migracao.
4. Criar um relatorio de baseline contendo:
   - testes aprovados;
   - testes falhos preexistentes;
   - quantidade de tabelas;
   - eventos publicados e consumidos;
   - buckets utilizados;
   - portas e nomes dos containers.

Nao realizar alteracoes nesta fase.

FASE 1 — CRIAR O NOVO SERVICO SEM REMOVER OS ANTIGOS

1. Criar `backend/patient-clinical-service/`.
2. Criar a estrutura:

app/
├── api/
├── domain/
│   ├── models/
│   ├── schemas/
│   └── services/
├── infrastructure/
│   ├── events/
│   ├── repositories/
│   └── s3/
└── workers/

3. Copiar os modulos Patient e Clinical para o novo servico.
4. Manter os modulos separados internamente:
   - patient_routers.py;
   - clinical_routers.py;
   - patient_service.py;
   - clinical_service.py;
   - patient_repository.py;
   - clinical_repository.py.
5. Nao alterar regras de negocio nesta fase.
6. Criar Dockerfile proprio.
7. Criar Dockerfile.worker proprio, se necessario.
8. Criar requirements.txt combinando as dependencias sem duplicacoes perigosas.
9. Criar um main.py inicial que carregue os modelos Patient e Clinical.

Validacao:

- Verificar todos os imports.
- Executar testes unitarios copiados.
- Executar `python -m compileall`.
- Confirmar que o novo servico inicia sem alterar os servicos antigos.
- Nao avancar se houver erro de importacao ou inicializacao.

FASE 2 — UNIFICAR CONFIGURACOES

Criar um unico app/config.py contendo:

- DATABASE_URL;
- RABBITMQ_URL;
- JWT_SECRET_KEY;
- JWT_ALGORITHM;
- configuracoes S3 de documentos;
- configuracoes S3 de prescricoes;
- configuracoes Celery;
- regras de cancelamento;
- configuracoes de assinatura digital;
- limites de upload.

Manter os buckets separados:

- patient-documents;
- prescriptions.

Nao sobrescrever configuracoes com valores incompativeis.

Validacao:

- Testar carregamento das configuracoes.
- Verificar que variaveis obrigatorias continuam obrigatorias.
- Confirmar que os testes conseguem criar suas configuracoes.
- Confirmar que Patient e Clinical usam o mesmo DATABASE_URL.

FASE 3 — UNIFICAR OS MODELOS

1. Importar todos os modelos Patient e Clinical no novo servico.
2. Garantir que todas as classes usem a mesma instancia Base.
3. Confirmar que os nomes das tabelas nao colidem.
4. Revisar nomes de ENUM PostgreSQL, especialmente:
   - gender_type;
   - allergy_severity;
   - medication_change_type;
   - appointment_type;
   - appointment_status;
   - exam_urgency.
5. Manter todas as tabelas existentes.
6. Nao remover patient_projections ainda.
7. Criar uma verificacao de metadata que liste todas as tabelas carregadas.

Validacao:

- Executar teste que carregue Base.metadata.
- Confirmar a presenca de todas as tabelas.
- Confirmar ausencia de colisoes de modelos.
- Executar testes de persistencia de Patient e Clinical.

FASE 4 — UNIFICAR AS ROTAS

Registrar no novo main.py:

- Patient router;
- Appointments router;
- Schedules router;
- Records router.

Preservar exatamente:

/api/v1/patients/*
/api/v1/appointments/*
/api/v1/schedules/*
/api/v1/records/*

Nao alterar:

- metodos HTTP;
- nomes dos campos;
- codigos HTTP;
- regras de autorizacao;
- formato de respostas.

Validacao:

- Gerar ou inspecionar as rotas FastAPI.
- Comparar as rotas antigas com as rotas novas.
- Executar testes HTTP dos dois servicos.
- Verificar `/healthz`.
- Confirmar que nao existem rotas duplicadas conflitantes.

FASE 5 — REMOVER A DEPENDENCIA DE PATIENT_PROJECTIONS

So executar esta fase apos a aplicacao unificada funcionar com os modelos atuais.

1. Alterar consultas clinicas para utilizar diretamente Patient.
2. Remover dependencias de PatientProjection nos servicos e routers.
3. Remover PatientProjectionRepository.
4. Remover os handlers internos de:
   - PatientCreatedEvent;
   - PatientUpdatedEvent.
5. Manter a publicacao desses eventos para consumidores externos.
6. Atualizar o tratamento de UserDeactivatedEvent para:
   - localizar o paciente diretamente;
   - desativar o paciente;
   - cancelar consultas futuras;
   - registrar auditoria;
   - executar tudo de forma transacional.
7. Nao remover a tabela fisica ainda.

Validacao:

- Criar paciente e consultar seus dados no fluxo clinico.
- Criar consulta para paciente existente.
- Desativar usuario e verificar cancelamento das consultas futuras.
- Confirmar que nao ha referencias a PatientProjection.
- Executar busca textual por patient_projections, PatientProjection e ProjectionRepository.

FASE 6 — MIGRACAO DOS BANCOS

Nao executar migracao destrutiva.

1. Fazer backup de patient_db e clinical_db.
2. Fazer backup dos arquivos MinIO relacionados.
3. Criar patient_clinical_db.
4. Criar tabelas no banco novo.
5. Migrar dados preservando todos os IDs.
6. Validar contagens e relacionamentos:
   - patients;
   - allergies;
   - vaccines;
   - continuous_medications;
   - medication_history;
   - patient_documents;
   - appointments;
   - medical_records;
   - prescriptions;
   - exam_requests;
   - medical_certificates;
   - tabelas de historico.
7. Validar se todos os patient_id clinicos possuem paciente correspondente.
8. Validar se todos os arquivos S3 continuam acessiveis.
9. Manter patient_projections somente durante a transicao.
10. Nao executar DROP TABLE ate a validacao final.

Validacao:

- Executar consultas de contagem antes e depois.
- Comparar IDs.
- Validar chaves estrangeiras.
- Validar documentos e prescricoes no MinIO.
- Executar testes de leitura e escrita usando o novo banco.

FASE 7 — EVENTOS RABBITMQ

Manter os eventos externos:

- PatientCreatedEvent;
- PatientUpdatedEvent;
- AllergyAddedEvent;
- AppointmentCreatedEvent;
- AppointmentCancelledEvent;
- MedicalRecordCreatedEvent;
- PrescriptionGeneratedEvent.

O novo servico deve:

1. Ter um unico EventPublisher.
2. Ter um unico EventConsumer.
3. Consumir eventos IAM necessarios.
4. Publicar eventos Patient e Clinical.
5. Nao consumir seus proprios eventos para recriar dados internos.
6. Manter idempotencia dos consumidores.
7. Preservar routing keys atuais.

Validacao:

- Publicar e consumir eventos em ambiente de teste.
- Confirmar que AI continua recebendo eventos clinicos.
- Confirmar que Reporting continua recebendo eventos Patient e Clinical.
- Confirmar ausencia de loops de eventos.
- Testar processamento duplicado do mesmo evento.

FASE 8 — CELERY E MINIO

1. Migrar o worker de prescricao para o novo servico.
2. Manter o mesmo nome das tarefas Celery, se possivel.
3. Manter o Redis DB utilizado pelo Clinical.
4. Manter o bucket prescriptions.
5. Manter o bucket patient-documents.
6. Garantir que documentos de pacientes e PDFs de prescricoes nao sejam misturados.
7. Validar geracao de:
   - prescription PDF;
   - documentos de pacientes;
   - URLs pre-assinadas;
   - assinaturas digitais.

Validacao:

- Executar o worker.
- Criar uma prescricao.
- Verificar geracao do PDF.
- Fazer upload e download de documento de paciente.
- Confirmar persistencia das chaves S3.

FASE 9 — DOCKER COMPOSE

Adicionar:

- patient-clinical-service;
- patient-clinical-worker;
- db-patient-clinical.

Atualizar dependencias para:

- PostgreSQL;
- RabbitMQ;
- Redis;
- MinIO.

Nao remover ainda:

- patient-service;
- clinical-service;
- db-patient;
- db-clinical.

Executar os servicos antigo e novo em paralelo durante a validacao, usando portas diferentes.

Validacao:

- `docker compose config`;
- build das imagens;
- startup do novo servico;
- health check;
- conexao com banco;
- conexao com RabbitMQ;
- conexao com Redis;
- conexao com MinIO;
- execucao do worker.

FASE 10 — ATUALIZAR O GATEWAY

Alterar o Gateway para encaminhar:

/api/v1/patients/* -> patient-clinical-service
/api/v1/appointments/* -> patient-clinical-service
/api/v1/schedules/* -> patient-clinical-service
/api/v1/records/* -> patient-clinical-service

Manter as URLs publicas exatamente iguais.

Atualizar:

- PATIENT_SERVICE_URL;
- CLINICAL_SERVICE_URL;
- health check agregado;
- documentacao do Gateway;
- testes de roteamento.

Durante a transicao, permitir rollback para os servicos antigos por variavel de ambiente ou configuracao reversivel.

Validacao:

- Login pelo Gateway.
- Criar paciente pelo Gateway.
- Consultar paciente pelo Gateway.
- Criar consulta pelo Gateway.
- Criar prontuario pelo Gateway.
- Criar prescricao pelo Gateway.
- Fazer download de documento.
- Verificar resposta de `/healthz/services`.

FASE 11 — TESTES COMPLETOS

Executar:

1. Testes originais do Patient.
2. Testes originais do Clinical.
3. Testes do novo servico.
4. Testes do Gateway.
5. Testes de eventos.
6. Testes de banco.
7. Testes de documentos.
8. Testes de PDF.
9. Smoke tests.
10. Testes de autorizacao.

Adicionar testes para:

- paciente e consulta no mesmo banco;
- transacao entre paciente e consulta;
- desativacao de paciente;
- cancelamento de consultas;
- prontuario com paciente inexistente;
- isolamento entre pacientes;
- publicacao de eventos;
- processamento idempotente;
- rollback de migracao.

Nao considerar a migracao concluida se houver regressao em comportamento ja existente.

FASE 12 — CORTE E REMOCAO DOS SERVICOS ANTIGOS

Somente apos todas as validacoes:

1. Fazer backup final dos bancos antigos.
2. Ativar o Gateway para o novo servico.
3. Monitorar logs, metricas e erros.
4. Manter os servicos antigos desligados, mas disponiveis para rollback.
5. Aguardar o periodo definido de observacao.
6. Remover:
   - patient-service;
   - clinical-service;
   - patient_db;
   - clinical_db;
   - PatientProjection;
   - configuracoes antigas do Gateway.
7. Atualizar documentacao, CI/CD, Makefile e scripts.
8. Atualizar o README e os diagramas arquiteturais.

CRITERIOS FINAIS DE ACEITE

A unificacao somente sera considerada concluida quando:

- existir apenas um servico para Patient e Clinical;
- todas as rotas antigas continuarem funcionando;
- nao houver chamada HTTP entre Patient e Clinical;
- nao houver uso de patient_projections;
- os dados forem migrados sem alteracao de IDs;
- AI e Reporting continuarem recebendo eventos;
- o worker de prescricao continuar funcional;
- documentos e PDFs continuarem acessiveis;
- todos os testes relevantes passarem;
- o Gateway apontar para o novo servico;
- houver procedimento de rollback documentado;
- a documentacao refletir a nova arquitetura.

Ao finalizar cada fase, apresente:

1. Arquivos alterados.
2. Mudancas realizadas.
3. Comandos executados.
4. Testes aprovados.
5. Testes falhos.
6. Riscos identificados.
7. Decisao sobre avancar ou interromper.

Nunca avance automaticamente para a fase seguinte quando uma validacao falhar.
```

## Observacao de uso

Execute uma fase por vez. O agente deve parar quando encontrar falha de teste, erro de migracao, conflito de modelo, problema de infraestrutura ou divergencia de contrato.
