# Auditoria de Integração Frontend ↔ Backend

**Data:** 18/11/2026  
**Escopo:** `frontend/` integrado ao Gateway Java (`:8000`), IAM (`:8001`) e Patient-Clinical (`:8002`).

## Resultado executivo

O frontend compila (`npm run build` concluído com sucesso), e o Gateway está corretamente configurado como base da API (`VITE_API_BASE_URL=http://localhost:8000/api/v1`). Entretanto, a integração de contratos ainda não está pronta para produção.

**Classificação atual: parcialmente integrada — bloqueadores de contrato existem.**

## Evidências positivas

- O frontend usa o Gateway, não acessa diretamente os microsserviços.
- O interceptor adiciona `Authorization: Bearer` e tenta renovar tokens em 401.
- As rotas principais do Gateway incluem IAM, Patient-Clinical e IA.
- Login, consulta de perfil, criação de consulta, confirmação e criação de prontuário foram exercitados manualmente com sucesso usando JSON compatível.
- `npm run build` passou sem erros de TypeScript/Vite.

## Bloqueadores encontrados

### 1. Convenção de nomes JSON incompatível — crítico

O frontend usa `snake_case` (`patient_id`, `scheduled_at`, `appointment_id`, `chief_complaint`), enquanto os Records Java recebem e retornam `camelCase` (`patientId`, `scheduledAt`, `appointmentId`, `chiefComplaint`). Não foi identificada configuração global de Jackson para converter snake case.

Impactos:

- Criação de consultas pelo frontend pode retornar 400.
- Criação/edição de prontuários pode retornar 400.
- Respostas chegam com propriedades diferentes das interfaces TypeScript, gerando telas vazias ou dados `undefined`.

Arquivos: `frontend/src/api/services.ts`, `frontend/src/types/index.ts`, DTOs Java em `modules/clinical/dto` e `modules/patient/dto`.

**Correção recomendada:** escolher uma convenção única. A opção de menor impacto no frontend é habilitar `PropertyNamingStrategies.SNAKE_CASE` no ObjectMapper Java e garantir que todos os DTOs/records usem a mesma convenção. Alternativamente, alterar todos os tipos e payloads TypeScript para camelCase.

### 2. Métodos HTTP divergentes — alto

O frontend envia:

- `PUT /appointments/{id}/cancel`
- `PUT /appointments/{id}/confirm`
- `PUT /appointments/{id}/complete`
- `PUT /patients/{id}`
- `PUT /records/{id}`

O backend expõe `PATCH` para essas operações. Isso resulta em HTTP 405.

**Correção recomendada:** alterar os métodos no frontend para `api.patch(...)`, ou adicionar aliases `@PutMapping` temporários no backend durante a migração.

### 3. Histórico de prontuários incompatível — alto

O frontend chama `GET /records/patient/{patientId}` esperando `{ items, total, page, size }`. O backend retorna diretamente uma lista JSON (`List<MedicalRecordResponseDto>`).

**Correção recomendada:** retornar um DTO paginado no backend ou ajustar `recordsApi.listByPatient` para consumir uma lista.

### 4. Endpoint inexistente no backend — alto

O frontend define `recordsApi.list()` para `GET /records`, mas o controlador Java não possui esse endpoint. A chamada falhará com 404.

**Correção recomendada:** remover a chamada ou implementar `GET /api/v1/records` com paginação e filtros.

### 5. Payload de paciente divergente — alto

`PatientCreate` do frontend usa `user_id`, `address` aninhado e `emergency_contact` aninhado. O DTO Java espera campos planos (`userId`, `street`, `city`, `state`, `zipCode`, `emergencyName`, etc.).

`patientsApi.update` também usa `PUT`, enquanto o backend usa `PATCH`.

### 6. Payload de prontuário incompleto — alto

`MedicalRecordCreate` do frontend não declara `patient_id`, mas o DTO Java exige `patientId`/`patient_id` obrigatório. Além disso, os demais campos estão em convenções diferentes.

### 7. Parâmetros de consulta divergentes — médio

O frontend envia `patient_id`, `doctor_id`, `from_date`, `to_date` e `active_only`. O backend espera `patientId`, `doctorId`, `fromDate`, `toDate` e `activeOnly`. Os filtros podem ser ignorados sem erro explícito.

### 8. Upload/downloads parcialmente implementados — médio

O frontend não expõe serviço correspondente ao upload de documentos, embora o backend tenha `POST /patients/{id}/documents/upload`. Também existem chamadas de PDF (`/pdf`) no frontend sem rotas correspondentes nos controladores Java atuais.

### 9. Ambiente de produção do frontend — médio

Há duas entradas `VITE_API_BASE_URL` em `frontend/.env*`, uma local e outra de produção. O valor efetivo depende do arquivo carregado pelo Vite e pode causar apontamento acidental para o ambiente errado. O `frontend/docker-compose.yml` usa uma imagem GHCR, não o build local.

## Segurança e autorização

- O frontend envia o Bearer token corretamente.
- A chave JWT precisa ser a mesma entre IAM, Gateway e Patient-Clinical; isso foi corrigido no ambiente Docker de teste.
- O backend restringe prontuário a `DOCTOR`/`ADMIN`, coerente com o domínio.
- Recomenda-se restringir no backend o acesso de pacientes a recursos de outros pacientes; algumas rotas GET aceitam qualquer role sem validação de vínculo.

## Verificações executadas

| Verificação | Resultado |
|---|---|
| Build frontend (`npm run build`) | PASSOU |
| Gateway como base `/api/v1` | OK |
| Login via Gateway | OK |
| Perfil `/patients/me` | OK com contrato Java camelCase |
| Agendamento/confirmacão manual | OK com payload camelCase e método PATCH |
| Prontuário manual | OK com payload camelCase e método POST |
| Contrato literal frontend ↔ Java | **FALHA nos itens acima** |

## Plano de correção prioritário

1. Padronizar JSON (recomendado: snake_case global no backend ou camelCase global no frontend).
2. Corrigir métodos HTTP no `frontend/src/api/services.ts`.
3. Corrigir tipos/payloads de Patient, Appointment e MedicalRecord.
4. Alinhar paginação de histórico e implementar/remover `GET /records`.
5. Alinhar rotas de PDF/documentos.
6. Separar claramente `.env.development` e `.env.production`.
7. Criar testes de contrato automatizados executando frontend client contra o Gateway.

## Correções aplicadas nesta rodada

- Jackson configurado com `SNAKE_CASE` no IAM e Patient-Clinical.
- Métodos `PATCH` alinhados no cliente para paciente, consulta e prontuário.
- Filtros do frontend traduzidos para os nomes de query params esperados pelo backend.
- `GET /records` implementado e histórico por paciente passou a retornar paginação.
- `patient_id` incluído na criação de prontuário e resolvido automaticamente no formulário de consulta.
- Paciente pode omitir `patient_id` ao agendar; o backend vincula o próprio perfil com validação de identidade.
- Rotas de presigned URL para PDFs de receitas/atestados adicionadas.
- Serviços frontend de upload/listagem de documentos adicionados.
- Frontend recompilado com sucesso após as alterações.

## Reauditoria — 18/11/2026

### Resultado

**Integração principal aprovada.** Frontend, Gateway, IAM, Patient-Clinical e IA estão ativos; os health checks responderam com sucesso e as filas de PDF de prescrição/atestado estão declaradas.

### Evidências adicionais

- `npm run build`: passou.
- Build Docker do Patient-Clinical com worker de atestados: passou.
- Gateway `/actuator/health`: `UP`.
- IAM `/healthz`: `ok`.
- Patient-Clinical `/healthz`: `ok`.
- IA `/healthz`: `200`, circuito da LLM `CLOSED`.
- `/api/v1/patients/me`: resposta em `snake_case` compatível com TypeScript.
- `/api/v1/records/patient/{id}`: resposta paginada compatível (`items`, `total`, `page`, `size`).
- Filas confirmadas: `prescription.pdf.generation` e `certificate.pdf.generation`.

### Pendências residuais

- Não há teste automatizado de contrato executando o cliente Axios real contra o Gateway; os testes realizados nesta reauditoria foram build e chamadas HTTP reais.
- O worker de atestados está implementado e compilado, mas a validação visual de um PDF real depende da criação de um atestado no ambiente com dados clínicos completos.

